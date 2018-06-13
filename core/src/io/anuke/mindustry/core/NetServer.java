package io.anuke.mindustry.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.gen.RemoteReadServer;
import io.anuke.mindustry.io.Version;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class NetServer extends Module{
    private final static float serverSyncTime = 4, kickDuration = 30 * 1000;
    private final static boolean preventDuplicatNames = false;
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
                Call.sendMessage("[accent]" + player.name + " has disconnected.");
                player.remove();
            }
        });

        Net.handleServer(ConnectPacket.class, (id, packet) -> {
            String uuid = new String(Base64Coder.encode(packet.uuid));

            if(Net.getConnection(id) == null ||
                    admins.isIPBanned(Net.getConnection(id).address)) return;

            TraceInfo trace = admins.getTraceByID(uuid);
            PlayerInfo info = admins.getInfo(uuid);
            trace.uuid = uuid;
            trace.android = packet.mobile;

            if(admins.isIDBanned(uuid)){
                kick(id, KickReason.banned);
                return;
            }

            if(TimeUtils.millis() - info.lastKicked < kickDuration){
                kick(id, KickReason.recentKick);
                return;
            }

            if(preventDuplicatNames) {
                for (Player player : playerGroup.all()) {
                    if (player.name.equalsIgnoreCase(packet.name)) {
                        kick(id, KickReason.nameInUse);
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
            player.isAdmin = admins.isAdmin(uuid, ip);
            player.clientid = id;
            player.name = packet.name;
            player.uuid = uuid;
            player.mech = packet.mobile ? Mechs.standardShip : Mechs.standard;
            player.dead = true;
            player.setNet(player.x, player.y);
            player.color.set(packet.color);
            connections.put(id, player);

            trace.playerid = player.id;

            //TODO try DeflaterOutputStream
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            NetworkIO.writeWorld(player, stream);
            WorldStream data = new WorldStream();
            data.stream = new ByteArrayInputStream(stream.toByteArray());
            Net.sendStream(id, data);

            Log.info("Packed {0} uncompressed bytes of WORLD data.", stream.size());

            Platform.instance.updateRPC();
        });

        //update last recieved snapshot based on client snapshot
        Net.handleServer(ClientSnapshotPacket.class, (id, packet) -> {
            Player player = connections.get(id);
            NetConnection connection = Net.getConnection(id);
            if(player == null || connection == null || packet.snapid < connection.lastRecievedSnapshot) return;

            boolean verifyPosition = !player.isDead() && !debug && !headless;

            if(connection.lastRecievedTime == 0) connection.lastRecievedTime = TimeUtils.millis() - 16;

            long elapsed = TimeUtils.timeSinceMillis(connection.lastRecievedTime);

            //extra 1.1x multiplicaton is added just in case
            float maxMove = elapsed / 1000f * 60f * player.mech.maxSpeed * 1.1f;

            player.pointerX = packet.pointerX;
            player.pointerY = packet.pointerY;

            vector.set(packet.x - player.getInterpolator().target.x, packet.y - player.getInterpolator().target.y);

            vector.limit(maxMove);

            float prevx = player.x, prevy = player.y;
            player.set(player.getInterpolator().target.x, player.getInterpolator().target.y);
            player.move(vector.x, vector.y);
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

            connection.lastSnapshotID = packet.lastSnapshot;
            connection.lastRecievedSnapshot = packet.snapid;
            connection.lastRecievedTime = TimeUtils.millis();
        });

        Net.handleServer(InvokePacket.class, (id, packet) -> RemoteReadServer.readPacket(packet.writeBuffer, packet.type, connections.get(id)));
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

    public void kick(int connection, KickReason reason){
        NetConnection con = Net.getConnection(connection);
        if(con == null){
            Log.err("Cannot kick unknown player!");
            return;
        }else{
            Log.info("Kicking connection #{0} / IP: {1}. Reason: {2}", connection, con.address, reason);
        }

        if((reason == KickReason.kick || reason == KickReason.banned) && admins.getTraceByID(getUUID(con.id)).uuid != null){
            PlayerInfo info = admins.getInfo(admins.getTraceByID(getUUID(con.id)).uuid);
            info.timesKicked ++;
            info.lastKicked = TimeUtils.millis();
        }

        //TODO kick player, send kick packet
        Call.onKick(connection, reason);

        Timers.runTask(2f, con::close);

        admins.save();
    }

    String getUUID(int connectionID){
        return connections.get(connectionID).uuid;
    }

    String fixName(String name){

        for(int i = 0; i < name.length(); i ++){
            if(name.charAt(i) == '[' && i != name.length() - 1 && name.charAt(i + 1) != '[' && (i == 0 || name.charAt(i - 1) != '[')){
                String prev = name.substring(0, i);
                String next = name.substring(i);
                String result = checkColor(next);

                name = prev + result;
            }
        }

        return name.substring(0, Math.min(name.length(), maxNameLength));
    }

    String checkColor(String str){

        for(int i = 1; i < str.length(); i ++){
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
                    }catch (Exception e){
                        return str;
                    }
                }
            }
        }
        return str;
    }

    void sync(){
        try {

            //iterate through each player
            for (Player player : connections.values()) {
                NetConnection connection = Net.getConnection(player.clientid);

                if(connection == null){
                    //player disconnected, ignore them
                    connections.remove(player.clientid);
                    player.remove();
                    return;
                }

                if(!player.timer.get(Player.timeSync, serverSyncTime)) continue;

                //if the player hasn't acknowledged that it has recieved the packet, send the same thing again
                if(connection.lastSentSnapshotID > connection.lastSnapshotID){
                    Call.onSnapshot(connection.id, connection.lastSentSnapshot, connection.lastSentSnapshotID);
                    return;
                }else{
                    //set up last confirmed snapshot to the last one that was sent, otherwise
                    connection.lastSnapshot = connection.lastSentSnapshot;
                }

                //reset stream to begin writing
                syncStream.reset();

                int totalGroups = 0;

                for (EntityGroup<?> group : Entities.getAllGroups()) {
                    if (!group.isEmpty() && (group.all().get(0) instanceof SyncTrait)) totalGroups ++;
                }

                Array<Tile> cores = state.teams.get(player.getTeam()).cores;

                dataStream.writeByte(cores.size);

                //write all core inventory data
                for(Tile tile : cores){
                    dataStream.writeInt(tile.packedPosition());
                    tile.entity.items.write(dataStream);
                }

                //write timestamp
                dataStream.writeLong(TimeUtils.millis());

                //write total amount of serializable groups
                dataStream.writeByte(totalGroups);

                //check for syncable groups
                for (EntityGroup<?> group : Entities.getAllGroups()) {
                    //TODO range-check sync positions to optimize?
                    if (group.isEmpty() || !(group.all().get(0) instanceof SyncTrait)) continue;

                    //make sure mapping is enabled for this group
                    if(!group.mappingEnabled()){
                        throw new RuntimeException("Entity group '" + group.getType() + "' contains SyncTrait entities, yet mapping is not enabled. In order for syncing to work, you must enable mapping for this group.");
                    }

                    //write group ID + group size
                    dataStream.writeByte(group.getID());
                    dataStream.writeShort(group.size());

                    for(Entity entity : group.all()){
                        int position = syncStream.position();
                        //write all entities now
                        dataStream.writeInt(entity.getID()); //write id
                        dataStream.writeByte(((SyncTrait)entity).getTypeID()); //write type ID
                        ((SyncTrait)entity).write(dataStream); //write entity
                        int length = syncStream.position() - position; //length must always be less than 127 bytes
                        if(length > 127) throw new RuntimeException("Write size for entity of type " + group.getType() + " must not exceed 127!");
                        dataStream.writeByte(length);
                    }
                }

                byte[] bytes = syncStream.toByteArray();
                connection.lastSentSnapshot = bytes;
                if(connection.lastSnapshotID == -1){
                    //no snapshot to diff, send it all
                    Call.onSnapshot(connection.id, bytes, 0);
                    connection.lastSnapshotID = 0;
                }else{
                    //send diff, otherwise
                    byte[] diff = ByteDeltaEncoder.toDiff(new ByteMatcherHash(connection.lastSnapshot, bytes), encoder);
                    Call.onSnapshot(connection.id, diff, connection.lastSnapshotID + 1);
                    //increment snapshot ID
                    connection.lastSentSnapshotID ++;
                }
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Remote(targets = Loc.client)
    public static void connectConfirm(Player player){
        player.add();
        Call.sendMessage("[accent]" + player.name + " has connected.");
        Log.info("&y{0} has connected.", player.name);
    }
}
