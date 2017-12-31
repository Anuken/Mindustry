package io.anuke.mindustry.core;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Net;
import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Packets.WorldData;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.Module;

public class NetClient extends Module {
    boolean connecting = false;

    public NetClient(){

        Net.handle(Connect.class, packet -> {
            connecting = true;
            Gdx.app.postRunnable(() -> {
                Vars.ui.hideLoading();
                Vars.ui.showLoading("$text.connecting.data");
            });
        });

        Net.handle(Disconnect.class, packet -> {
            Gdx.app.postRunnable(() -> {
                Timers.runFor(3f, () -> {
                    Vars.ui.hideLoading();
                });

                Vars.ui.showError("$text.disconnect");
                connecting = false;
            });
        });

        Net.handle(WorldData.class, data -> {
            Gdx.app.postRunnable(() -> {
                UCore.log("Recieved world data: " + data.stream.available() + " bytes.");
                SaveIO.load(data.stream);
                GameState.set(State.playing);
                connecting = false;
                Vars.ui.hideLoading();
                Vars.ui.hideJoinGame();
            });
        });
    }

    public void update(){
        if(!Net.client()) return;

        if(!GameState.is(State.menu) && Net.active()){

        }else if(!connecting){
            Net.disconnect();
        }
    }
}
