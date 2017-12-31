package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.ucore.modules.Module;

public class NetClient extends Module {

    public NetClient(){

        Net.handle(Connect.class, packet -> {
            Gdx.app.postRunnable(() -> {
                Vars.ui.hideLoading();
                Vars.ui.showLoading("$text.connecting.data");
            });
        });
    }

    public void update(){
        if(!Net.client()) return;

        if(!GameState.is(State.menu) && Net.active()){

        }else{
            Net.disconnect();
        }
    }
}
