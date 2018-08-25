package io.anuke.mindustry.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.gen.RemoteReadServer;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.io.CountableByteArrayOutputStream;
import io.anuke.ucore.io.delta.ByteDeltaEncoder;
import io.anuke.ucore.io.delta.ByteMatcherHash;
import io.anuke.ucore.io.delta.DEZEncoder;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;

import static io.anuke.mindustry.Vars.*;

public class NetServer extends Module{
    public final static int maxSnapshotSize = 2047;
    public final static boolean showSnapshotSize = false;

    private final static byte[] reusableSnapArray = new byte[maxSnapshotSize];
    private final static float serverSyncTime = 4, kickDuration = 30 * 1000;
    private final static Vector2 vector = new Vector2();
    /**If a play goes away of their server-side coordinates by this distance, they get teleported back.*/
    private final static float correctDist = 16f;

    public final Administration admins = new Administration();

    /**Maps connection IDs to players.*/
    private IntMap<Player> connections = new IntMap<>();
    private boolean closing = false;

    /**Stream for writing player sync data to.*/
    private CountableByteArrayOutputStream syncStream = new CountableByteArrayOutputStream();
    /**Data stream for writing player sync data to.*/
    private DataOutputStream dataStream = new DataOutputStream(syncStream);
    /**Encoder for computing snapshot deltas.*/
    private DEZEncoder encoder = new DEZEncoder();

    public NetServer(){

        Net.handleServer(Connect.class, (id, connect) -> {
            if(admins.isIPBanned(connect.addressTCP)){
                kick(id, KickReason.banned);
            }
        });

        Net.handleServer(Disconnect.class, (id, packet) -> {
            Player player = connections.get(id);
            if(player != null){
                onDisconnect(player);
            }
            connections.remove(id);
        });

        Net.handleServer(ConnectPacket.class, (id, packet) -> {
            String uuid = packet.uuid;

            NetConnection connection = Net.getConnection(id);

            if(connection == null ||
                    admins.isIPBanned(connection.address)) return;

            if(connection.hasBegunConnecting){
                kick(id, KickReason.idInUse);
                return;
            }

            connection.hasBegunConnecting = true;

            TraceInfo trace = admins.getTraceByID(uuid);
            PlayerInfo info = admins.getInfo(uuid);
            trace.uuid = uuid;
            trace.ip = connection.address;
            trace.android = packet.mobile;

            if(admins.isIDBanned(uuid)){
                kick(id, KickReason.banned);
                return;
            }

            if(TimeUtils.millis() - info.lastKicked < kickDuration){
                kick(id, KickReason.recentKick);
                return;
            }

            if(packet.version == -1 && Version.build != -1 && !admins.allowsCustomClients()){
                kick(id, KickReason.customClient);
                return;
            }

            boolean preventDuplicates = headless && !debug;

            if(preventDuplicates){
                for(Player player : playerGroup.all()){
                    if(player.name.equalsIgnoreCase(packet.name)){
                        kick(id, KickReason.nameInUse);
                        return;
                    }

                    if(player.uuid.equals(packet.uuid) || player.usid.equals(packet.usid)){
                        kick(id, KickReason.idInUse);
                        return;
                    }
                }
            }

            packet.name = fixName(packet.name);

            if(packet.name.trim().length() <= 0){
                kick(id, KickReason.nameEmpty);
                return;
            }

            Log.info("Recieved connect packet for player '{0}' / UUID {1} / IP {2}", packet.name, uuid, trace.ip);

            String ip = Net.getConnection(id).address;

            admins.updatePlayerJoined(uuid, ip, packet.name);

            if(packet.version != Version.build && Version.build != -1 && packet.version != -1){
                kick(id, packet.version > Version.build ? KickReason.serverOutdated : KickReason.clientOutdated);
                return;
            }

            if(packet.version == -1){
                trace.modclient = true;
            }

            Player player = new Player();
            player.isAdmin = admins.isAdmin(uuid, packet.usid);
            player.con = Net.getConnection(id);
            player.usid = packet.usid;
            player.name = packet.name;
            player.uuid = uuid;
            player.isMobile = packet.mobile;
            player.mech = packet.mobile ? Mechs.starterMobile : Mechs.starterDesktop;
            player.dead = true;
            player.setNet(player.x, player.y);
            player.color.set(packet.color);
            player.color.a = 1f;

            if(state.mode.isPvp){
                //find team with minimum amount of players and auto-assign player to that.
                Team min = Mathf.findMin(Team.all, team -> {
                    if(state.teams.isActive(team)){
                        int count = 0;
                        for(Player other : playerGroup.all()){
                            if(other.getTeam() == team){
                                count ++;
                            }
                        }
                        return count;
                    }
                    return Integer.MAX_VALUE;
                });
                player.setTeam(min);
            }

            connections.put(id, player);

            trace.playerid = player.id;

            sendWorldData(player, id);

            Platform.instance.updateRPC();
        });

        //update last recieved snapshot based on client snapshot
        Net.handleServer(ClientSnapshotPacket.class, (id, packet) -> {
            Player player = connections.get(id);
            NetConnection connection = Net.getConnection(id);
            if(player == null || connection == null || packet.snapid < connection.lastRecievedClientSnapshot) return;

            boolean verifyPosition = !player.isDead() && !debug && headless && player.getCarrier() == null;

            if(connection.lastRecievedClientTime == 0) connection.lastRecievedClientTime = TimeUtils.millis() - 16;

            long elapsed = TimeUtils.timeSinceMillis(connection.lastRecievedClientTime);

            float maxSpeed = packet.boosting && !player.mech.flying ? player.mech.boostSpeed : player.mech.speed;
            float maxMove = elapsed / 1000f * 60f * Math.min(compound(maxSpeed, player.mech.drag) * 1.2f, player.mech.maxSpeed * 1.05f);

            player.pointerX = packet.pointerX;
            player.pointerY = packet.pointerY;
            player.setMineTile(packet.mining);
            player.isBoosting = packet.boosting;
            player.isShooting = packet.shooting;
            player.getPlaceQueue().clear();
            for(BuildRequest req : packet.requests){
                //auto-skip done requests
                if(req.remove && world.tile(req.x, req.y).block() == Blocks.air){
                    continue;
                }else if(!req.remove && world.tile(req.x, req.y).block() == req.recipe.result && (!req.recipe.result.rotate || world.tile(req.x, req.y).getRotation() == req.rotation)){
                    continue;
                }
                player.getPlaceQueue().addLast(req);
            }

            vector.set(packet.x - player.getInterpolator().target.x, packet.y - player.getInterpolator().target.y);
            vector.limit(maxMove);

            float prevx = player.x, prevy = player.y;
            player.set(player.getInterpolator().target.x, player.getInterpolator().target.y);
            if(!player.mech.flying){
                player.move(vector.x, vector.y);
            }else{
                player.x += vector.x;
                player.y += vector.y;
            }
            float newx = player.x, newy = player.y;

            if(!verifyPosition){
                player.x = prevx;
                player.y = prevy;
                newx = packet.x;
                newy = packet.y;
            }else if(Vector2.dst(packet.x, packet.y, newx, newy) > correctDist){
                Call.onPositionSet(id, newx, newy); //teleport and correct position when necessary
            }

            //reset player to previous synced position so it gets interpolated
            player.x = prevx;
            player.y = prevy;

            //set interpolator target to *new* position so it moves toward it
            player.getInterpolator().read(player.x, player.y, newx, newy, packet.timeSent, packet.rotation, packet.baseRotation);
            player.getVelocity().set(packet.xv, packet.yv); //only for visual calculation purposes, doesn't actually update the player

            //when the client confirms recieveing a snapshot, update base and clear map
            if(packet.lastSnapshot > connection.currentBaseID){
                connection.currentBaseID = packet.lastSnapshot;
                connection.currentBaseSnapshot = connection.lastSentRawSnapshot;
            }

            connection.lastRecievedClientSnapshot = packet.snapid;
            connection.lastRecievedClientTime = TimeUtils.millis();
        });

        Net.handleServer(InvokePacket.class, (id, packet) -> {
            Player player = connections.get(id);
            if(player == null) return;
            RemoteReadServer.readPacket(packet.writeBuffer, packet.type, player);
        });
    }

    private float compound(float speed, float drag){
        float total = 0f;
        for(int i = 0; i < 20; i++){
            total *= (1f - drag);
            total += speed;
        }
        return total;
    }

    /** Sends a raw byte[] snapshot to a client, splitting up into chunks when needed.*/
    private static void sendSplitSnapshot(int userid, byte[] bytes, int snapshotID, int base){
        if(bytes.length < maxSnapshotSize){
            Call.onSnapshot(userid, bytes, snapshotID, (short) 0, bytes.length, base);
        }else{
            int remaining = bytes.length;
            int offset = 0;
            int chunkid = 0;
            while(remaining > 0){
                int used = Math.min(remaining, maxSnapshotSize);
                byte[] toSend;
                //re-use sent byte arrays when possible
                if(used == maxSnapshotSize){
                    toSend = reusableSnapArray;
                    System.arraycopy(bytes, offset, toSend, 0, Math.min(offset + maxSnapshotSize, bytes.length) - offset);
                }else{
                    toSend = Arrays.copyOfRange(bytes, offset, Math.min(offset + maxSnapshotSize, bytes.length));
                }
                Call.onSnapshot(userid, toSend, snapshotID, (short) chunkid, bytes.length, base);

                remaining -= used;
                offset += used;
                chunkid++;
            }
        }
    }

    public void sendWorldData(Player player, int clientID){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DeflaterOutputStream def = new DeflaterOutputStream(stream);
        NetworkIO.writeWorld(player, def);
        WorldStream data = new WorldStream();
        data.stream = new ByteArrayInputStream(stream.toByteArray());
        Net.sendStream(clientID, data);

        Log.info("Packed {0} compressed bytes of world data.", stream.size());
    }

    public static void onDisconnect(Player player){
        if(player.con.hasConnected){
            Call.sendMessage("[accent]" + player.name + "[accent] has disconnected.");
            Call.onPlayerDisconnect(player.id);
        }
        player.remove();
        netServer.connections.remove(player.con.id);
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void onAdminRequest(Player player, Player other, AdminAction action){

        if(!player.isAdmin){
            Log.err("ACCESS DENIED: Player {0} / {1} attempted to perform admin action without proper security access.",
                    player.name, player.con.address);
            return;
        }

        if(other == null || (other.isAdmin && other != player)){ //fun fact: this means you can ban yourself
            Log.err("{0} attempted to perform admin action on nonexistant or admin player.", player.name);
            return;
        }

        if(action == AdminAction.wave){
            //no verification is done, so admins can hypothetically spam waves
            //not a real issue, because server owners may want to do just that
            state.wavetime = 0f;
        }else if(action == AdminAction.ban){
            netServer.admins.banPlayerIP(other.con.address);
            netServer.kick(other.con.id, KickReason.banned);
            Log.info("&lc{0} has banned {1}.", player.name, other.name);
        }else if(action == AdminAction.kick){
            netServer.kick(other.con.id, KickReason.kick);
            Log.info("&lc{0} has kicked {1}.", player.name, other.name);
        }else if(action == AdminAction.trace){
            //TODO
            if(player.con != null){
                Call.onTraceInfo(player.con.id, netServer.admins.getTraceByID(other.uuid));
            }else{
                NetClient.onTraceInfo(netServer.admins.getTraceByID(other.uuid));
            }
            Log.info("&lc{0} has requested trace info of {1}.", player.name, other.name);
        }
    }

    @Remote(targets = Loc.client)
    public static void connectConfirm(Player player){
        if(player.con == null || player.con.hasConnected) return;

        player.add();
        player.con.hasConnected = true;
        Call.sendMessage("[accent]" + player.name + "[accent] has connected.");
        Log.info("&y{0} has connected.", player.name);
    }

    public void update(){
        if(!headless && !closing && Net.server() && state.is(State.menu)){
            closing = true;
            reset();
            ui.loadfrag.show("$text.server.closing");
            Timers.runTask(5f, () -> {
                Net.closeServer();
                ui.loadfrag.hide();
                closing = false;
            });
        }

        if(!state.is(State.menu) && Net.server()){
            sync();
        }
    }

    public void reset(){
        admins.clearTraces();
    }

    public void kickAll(KickReason reason){
        for(NetConnection con : Net.getConnections()){
            kick(con.id, reason);
        }
    }

    public void kick(int connection, KickReason reason){
        NetConnection con = Net.getConnection(connection);
        if(con == null){
            Log.err("Cannot kick unknown player!");
            return;
        }else{
            Log.info("Kicking connection #{0} / IP: {1}. Reason: {2}", connection, con.address, reason);
        }

        Player player = connections.get(con.id);

        if(player != null && (reason == KickReason.kick || reason == KickReason.banned) && player.uuid != null){
            PlayerInfo info = admins.getInfo(player.uuid);
            info.timesKicked++;
            info.lastKicked = TimeUtils.millis();
        }

        Call.onKick(connection, reason);

        Timers.runTask(2f, con::close);

        admins.save();
    }

    public void writeSnapshot(Player player, DataOutputStream dataStream) throws IOException{
        //write wave datas
        dataStream.writeFloat(state.wavetime);
        dataStream.writeInt(state.wave);

        Array<Tile> cores = state.teams.get(player.getTeam()).cores;

        dataStream.writeByte(cores.size);

        //write all core inventory data
        for(Tile tile : cores){
            dataStream.writeInt(tile.packedPosition());
            tile.entity.items.write(dataStream);
        }

        //write timestamp
        dataStream.writeLong(TimeUtils.millis());

        int totalGroups = 0;

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && (group.all().get(0) instanceof SyncTrait)) totalGroups++;
        }

        //write total amount of serializable groups
        dataStream.writeByte(totalGroups);

        //check for syncable groups
        for(EntityGroup<?> group : Entities.getAllGroups()){
            //TODO range-check sync positions to optimize?
            if(group.isEmpty() || !(group.all().get(0) instanceof SyncTrait)) continue;

            //make sure mapping is enabled for this group
            if(!group.mappingEnabled()){
                throw new RuntimeException("Entity group '" + group.getType() + "' contains SyncTrait entities, yet mapping is not enabled. In order for syncing to work, you must enable mapping for this group.");
            }

            int amount = 0;

            for(Entity entity : group.all()){
                if(((SyncTrait) entity).isSyncing()){
                    amount++;
                }
            }

            //write group ID + group size
            dataStream.writeByte(group.getID());
            dataStream.writeShort(amount);

            for(Entity entity : group.all()){
                if(!((SyncTrait) entity).isSyncing()) continue;

                int position = syncStream.position();
                //write all entities now
                dataStream.writeInt(entity.getID()); //write id
                dataStream.writeByte(((SyncTrait) entity).getTypeID()); //write type ID
                ((SyncTrait) entity).write(dataStream); //write entity
                int length = syncStream.position() - position; //length must always be less than 127 bytes
                if(length > 127)
                    throw new RuntimeException("Write size for entity of type " + group.getType() + " must not exceed 127!");
            }
        }
    }

    String getUUID(int connectionID){
        return connections.get(connectionID).uuid;
    }

    String fixName(String name){
        if(name.equals("[") || name.equals("]")){
            return "";
        }

        for(int i = 0; i < name.length(); i++){
            if(name.charAt(i) == '[' && i != name.length() - 1 && name.charAt(i + 1) != '[' && (i == 0 || name.charAt(i - 1) != '[')){
                String prev = name.substring(0, i);
                String next = name.substring(i);
                String result = checkColor(next);

                name = prev + result;
            }
        }

        StringBuilder result = new StringBuilder();
        int curChar = 0;
        while(curChar < name.length() && result.toString().getBytes().length < maxNameLength){
            result.append(name.charAt(curChar++));
        }
        return result.toString();
    }

    String checkColor(String str){

        for(int i = 1; i < str.length(); i++){
            if(str.charAt(i) == ']'){
                String color = str.substring(1, i);

                if(Colors.get(color.toUpperCase()) != null || Colors.get(color.toLowerCase()) != null){
                    Color result = (Colors.get(color.toLowerCase()) == null ? Colors.get(color.toUpperCase()) : Colors.get(color.toLowerCase()));
                    if(result.a <= 0.8f){
                        return str.substring(i + 1);
                    }
                }else{
                    try{
                        Color result = Color.valueOf(color);
                        if(result.a <= 0.8f){
                            return str.substring(i + 1);
                        }
                    }catch(Exception e){
                        return str;
                    }
                }
            }
        }
        return str;
    }

    void sync(){
        try{

            //iterate through each player
            for(Player player : connections.values()){
                NetConnection connection = player.con;

                if(!connection.isConnected()){
                    //player disconnected, ignore them
                    onDisconnect(player);
                    return;
                }

                if(!player.timer.get(Player.timerSync, serverSyncTime) || !connection.hasConnected) continue;

                //if the player hasn't acknowledged that it has recieved the packet, send the same thing again
                if(connection.currentBaseID < connection.lastSentSnapshotID){
                    if(showSnapshotSize)
                        Log.info("Re-sending snapshot: {0} bytes, ID {1} base {2} baselength {3}", connection.lastSentSnapshot.length, connection.lastSentSnapshotID, connection.lastSentBase, connection.currentBaseSnapshot.length);
                    sendSplitSnapshot(connection.id, connection.lastSentSnapshot, connection.lastSentSnapshotID, connection.lastSentBase);
                    return;
                }

                //reset stream to begin writing
                syncStream.reset();

                writeSnapshot(player, dataStream);

                byte[] bytes = syncStream.toByteArray();

                if(connection.currentBaseID == -1){
                    //assign to last sent snapshot so that there is only ever one unique snapshot with ID 0
                    if(connection.lastSentSnapshot != null){
                        bytes = connection.lastSentSnapshot;
                    }else{
                        connection.lastSentRawSnapshot = bytes;
                        connection.lastSentSnapshot = bytes;
                    }

                    if(showSnapshotSize) Log.info("Sent raw snapshot: {0} bytes.", bytes.length);
                    ///Nothing to diff off of in this case, send the whole thing
                    sendSplitSnapshot(connection.id, bytes, 0, -1);
                }else{
                    connection.lastSentRawSnapshot = bytes;

                    //send diff, otherwise
                    byte[] diff = ByteDeltaEncoder.toDiff(new ByteMatcherHash(connection.currentBaseSnapshot, bytes), encoder);
                    if(showSnapshotSize)
                        Log.info("Shrank snapshot: {0} -> {1}, Base {2} ID {3} base length = {4}", bytes.length, diff.length, connection.currentBaseID, connection.currentBaseID + 1, connection.currentBaseSnapshot.length);
                    sendSplitSnapshot(connection.id, diff, connection.currentBaseID + 1, connection.currentBaseID);
                    connection.lastSentSnapshot = diff;
                    connection.lastSentSnapshotID = connection.currentBaseID + 1;
                    connection.lastSentBase = connection.currentBaseID;
                }
            }

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
