package io.anuke.mindustry.core;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.modules.Module;

public class NetClient extends Module {

    public NetClient(){

    }

    public void update(){
        if(!Net.client()) return;

        if(!GameState.is(State.menu) && Net.active()){

        }else{
            Net.disconnect();
        }
    }
}
