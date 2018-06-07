package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.RemoteReadServer;
import io.anuke.mindustry.net.Administration;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.ClientSnapshotPacket;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.InvokePacket;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Timer;

import java.nio.ByteBuffer;

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
    private ByteBuffer writeBuffer = ByteBuffer.allocate(32);

    public NetServer(){

        Net.handleServer(Connect.class, (id, connect) -> {
            if(admins.isIPBanned(connect.addressTCP)){
                kick(id, KickReason.banned);
            }
        });

        Net.handleServer(ClientSnapshotPacket.class, (id, packet) -> {
            //...don't do anything here as it's already handled by the packet itself
        });

        Net.handleServer(InvokePacket.class, (id, packet) -> {
            //TODO implement
            RemoteReadServer.readPacket(packet.writeBuffer, packet.type, connections.get(id));
        });
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

    void sendMessageTo(int id, String message){
        //TODO implement
    }

    void sync(){
        //TODO implement snapshot packets w/ delta compression
    }
}
