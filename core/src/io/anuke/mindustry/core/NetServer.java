package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.EntityDataPacket;
import io.anuke.mindustry.net.Packets.SyncPacket;
import io.anuke.mindustry.net.Packets.WorldData;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.Module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class NetServer extends Module{
    IntMap<Player> connections = new IntMap<>();
    float serverSyncTime = 4;

    public NetServer(){

        Net.handleServer(Connect.class, packet -> {
            UCore.log("Sending world data to client (ID="+packet.id+"/"+packet.addressTCP+")");

            WorldData data = new WorldData();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            SaveIO.write(stream);

            UCore.log("Packed " + stream.size() + " uncompressed bytes of data.");

            //TODO compress and uncompress when sending
            data.stream = new ByteArrayInputStream(stream.toByteArray());

            Net.sendStream(packet.id, data);

            Gdx.app.postRunnable(() -> {
                EntityDataPacket dp = new EntityDataPacket();

                Player player = new Player();
                player.clientid = packet.id;
                player.add();
                connections.put(player.id, player);

                dp.playerid = player.id;
                dp.players = Vars.control.playerGroup.all().toArray(Player.class);

                Net.sendTo(packet.id, dp, SendMode.tcp);
            });
        });
    }

    public void update(){
        if(!Net.server()) return;

        if(!GameState.is(State.menu) && Net.active()){
            sync();
        }else{
            Net.closeServer();
        }
    }

    void sync(){
        if(Timers.get("serverSync", serverSyncTime)){
            SyncPacket packet = new SyncPacket();

            for(Player player : Vars.control.playerGroup.all()){

            }
        }
    }
}
