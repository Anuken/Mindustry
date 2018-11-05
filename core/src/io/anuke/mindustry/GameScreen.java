package io.anuke.mindustry;

import io.anuke.mindustry.core.Control;
import io.anuke.mindustry.core.Logic;
import io.anuke.mindustry.core.NetClient;
import io.anuke.mindustry.core.NetServer;
import io.anuke.mindustry.core.Renderer;
import io.anuke.mindustry.core.UI;
import io.anuke.mindustry.core.World;
import io.anuke.mindustry.game.EventType.GameLoadEvent;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.control;
import static io.anuke.mindustry.Vars.logic;
import static io.anuke.mindustry.Vars.netClient;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.renderer;
import static io.anuke.mindustry.Vars.threads;
import static io.anuke.mindustry.Vars.ui;
import static io.anuke.mindustry.Vars.world;

public class GameScreen extends ModuleCore{

    @Override
    public void init(){
        Timers.mark();

        Vars.init();

        Log.setUseColors(false);
        BundleLoader.load();
        content.load();

        module(logic = new Logic());
        module(world = new World());
        module(control = new Control());
        module(renderer = new Renderer());
        module(ui = new UI());
        module(netServer = new NetServer());
        module(netClient = new NetClient());
    }

    @Override
    public void postInit(){
        Log.info("Time to load [total]: {0}", Timers.elapsed());
        Events.fire(new GameLoadEvent());
    }

    @Override
    public void render(){
        threads.handleBeginRender();
        super.render();
        threads.handleEndRender();
    }

}
