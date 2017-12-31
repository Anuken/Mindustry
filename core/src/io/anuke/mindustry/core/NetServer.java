package io.anuke.mindustry.core;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.WorldData;
import io.anuke.ucore.UCore;
import io.anuke.ucore.modules.Module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NetServer extends Module{

    public NetServer(){

        Net.handleServer(Connect.class, packet -> {
            UCore.log("Sending world data to client (ID="+packet.id+"/"+packet.addressTCP+")");

            WorldData data = new WorldData();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            SaveIO.write(stream);

            UCore.log("Packed " + stream.size() + " bytes of data.");

            data.stream = new ByteArrayInputStream(stream.toByteArray());
            Net.sendStream(packet.id, data);
        });
    }

    public void update(){
        if(!Net.server()) return;

        if(!GameState.is(State.menu) && Net.active()){

        }else{
            Net.closeServer();
        }
    }
}
