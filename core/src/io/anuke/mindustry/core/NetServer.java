package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.RemoteReadServer;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Timer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static io.anuke.mindustry.Vars.*;

public class NetServer extends Module{
    private final static float serverSyncTime = 4, itemSyncTime = 10, kickDuration = 30 * 1000;

    private final static int timerEntitySync = 0;
    private final static int timerStateSync = 1;

    public final Administration admins = new Administration();

    /**Maps connection IDs to players.*/
    private IntMap<Player> connections = new IntMap<>();
    private boolean closing = false;
    private Timer timer = new Timer(5);

    public NetServer(){

        Net.handleServer(Connect.class, (id, connect) -> {
            if(admins.isIPBanned(connect.addressTCP)){
                kick(id, KickReason.banned);
            }
        });

        Net.handleServer(Disconnect.class, (id, packet) -> {});

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

            for(Player player : playerGroup.all()){
                if(player.name.equalsIgnoreCase(packet.name)){
                    kick(id, KickReason.nameInUse);
                    return;
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


        Net.handleServer(ClientSnapshotPacket.class, (id, packet) -> {});

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

        Timers.runTask(2f, con::close);

        admins.save();
    }

    String getUUID(int connectionID){
        return connections.get(connectionID).uuid;
    }

    void sync(){
        //TODO implement snapshot packets w/ delta compression



        //iterate through each player
        for(Player player : connections.values()){

            //check for syncable groups.
            for(EntityGroup<?> group : Entities.getAllGroups()){
                if(group.isEmpty()) continue;
            }
        }

    }

    @Remote(server = false)
    public static void connectConfirm(Player player){
        player.add();
        Log.info("&y{0} has connected.", player.name);
        netCommon.sendMessage("[accent]" + player.name + " has connected.");
    }
}
