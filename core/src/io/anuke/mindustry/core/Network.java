package io.anuke.mindustry.core;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.modules.Module;

import java.io.IOException;

public class Network extends Module{
    private boolean isHosting;

    public Network(){

    }

    public void update(){
        if(isHosting && GameState.is(State.menu)){
            Net.closeServer();
            isHosting = false;
        }
    }

    public void hostServer(int port) throws IOException{
        if(isHosting){
            throw new IOException("Already hosting a server!");
        }

        Net.host(port);
        isHosting = true;
    }

    public boolean isHosting(){
        return isHosting;
    }
}
