package io.anuke.mindustry.core;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.modules.Module;

import java.io.IOException;

public class NetServer extends Module{

    public NetServer(){

    }

    public void update(){
        if(!Net.server()) return;

        if(!GameState.is(State.menu) && Net.active()){

        }else{
            Net.closeServer();
        }
    }
}
