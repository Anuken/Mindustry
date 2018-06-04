package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Timer;

import static io.anuke.mindustry.Vars.*;

public class NetClient extends Module {
    private final static float dataTimeout = 60*18;
    private final static float playerSyncTime = 2;
    private final static int maxRequests = 50;

    private Timer timer = new Timer(5);
    /**Whether the client is currently conencting.*/
    private boolean connecting = false;
    /**If true, no message will be shown on disconnect.*/
    private boolean quiet = false;
    /**List of all recieved entitity IDs, to prevent duplicates.*/
    private IntSet recieved = new IntSet();
    /**List of recently recieved entities that have not been added to the queue yet.*/
    private IntMap<SyncTrait> recent = new IntMap<>();
    /**Counter for data timeout.*/
    private float timeoutTime = 0f;
    private int requests = 0;

    public NetClient(){

        Net.handleClient(Connect.class, packet -> {
            Player player = players[0];

            player.isAdmin = false;

            Net.setClientLoaded(false);
            recieved.clear();
            recent.clear();
            timeoutTime = 0f;
            connecting = true;
            quiet = false;

            ui.chatfrag.clearMessages();
            ui.loadfrag.hide();
            ui.loadfrag.show("$text.connecting.data");

            Entities.clear();

            //TODO send connect packet here
        });

        Net.handleClient(Disconnect.class, packet -> {
            if (quiet) return;

            Timers.runTask(3f, ui.loadfrag::hide);

            state.set(State.menu);

            ui.showError("$text.disconnect");
            connecting = false;

            Platform.instance.updateRPC();
        });

        Net.handleClient(WorldStream.class, data -> {
            Log.info("Recieved world data: {0} bytes.", data.stream.available());
            NetworkIO.loadWorld(data.stream);

            finishConnecting();
        });

        Net.handleClient(InvokePacket.class, packet -> {
            //TODO invoke it
        });
    }

    @Override
    public void update(){
        if(!Net.client()) return;

        if(!state.is(State.menu)){
            if(!connecting) sync();
        }else if(!connecting){
            Net.disconnect();
        }else{ //...must be connecting
            timeoutTime += Timers.delta();
            if(timeoutTime > dataTimeout){
                Log.err("Failed to load data!");
                ui.loadfrag.hide();
                quiet = true;
                ui.showError("$text.disconnect.data");
                Net.disconnect();
                timeoutTime = 0f;
            }
        }
    }

    public boolean isConnecting(){
        return connecting;
    }

    private void finishConnecting(){
        state.set(State.playing);
        connecting = false;
        ui.loadfrag.hide();
        ui.join.hide();
        Net.setClientLoaded(true);
        //send connect ACK packet
        //Timers.runTask(1f, () -> Net.send(new ConnectConfirmPacket(), SendMode.tcp));
        Timers.runTask(40f, Platform.instance::updateRPC);
    }

    public void beginConnecting(){
        connecting = true;
    }

    public void disconnectQuietly(){
        quiet = true;
        Net.disconnect();
    }

    void sync(){
        requests = 0;

        if(timer.get(0, playerSyncTime)){
            Player player = players[0];

            ClientSnapshotPacket packet = new ClientSnapshotPacket();
            packet.player = player;
            Net.send(packet, SendMode.udp);
        }

        if(timer.get(1, 60)){
            Net.updatePing();
        }
    }
}