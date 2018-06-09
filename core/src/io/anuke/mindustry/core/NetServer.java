package io.anuke.mindustry.core;

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
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.trait.Entity;
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

    public final Administration admins = new Administration();

    /**Maps connection IDs to players.*/
    private IntMap<Player> connections = new IntMap<>();
    private boolean closing = false;

    /**Stream for writing player sync data to.*/
    private ByteArrayOutputStream syncStream = new ByteArrayOutputStream();
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
        Net.handleServer(ClientSnapshotPacket.class, (id, packet) -> Net.getConnection(id).lastSnapshotID = packet.lastSnapshot);

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

    void sync(){
        try {

            //iterate through each player
            for (Player player : connections.values()) {
                NetConnection connection = Net.getConnection(player.clientid);

                if(connection == null){
                    Log.err("Player {0} failed to connect.", player.name);
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

                    int size = group.size();

                    if(group.getType() == Player.class){
                        size --;
                    }

                    //write group ID + group size
                    dataStream.writeByte(group.getID());
                    dataStream.writeShort(size);
                    //write timestamp
                    dataStream.writeLong(TimeUtils.millis());

                    for(Entity entity : group.all()){
                        if(entity == player) continue;
                        //write all entities now
                        dataStream.writeInt(entity.getID());
                        ((SyncTrait)entity).write(dataStream);
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
