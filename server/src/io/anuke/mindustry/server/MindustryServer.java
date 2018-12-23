package io.anuke.mindustry.server;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Logic;
import io.anuke.mindustry.core.NetServer;
import io.anuke.mindustry.core.World;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.io.BundleLoader;

import static io.anuke.mindustry.Vars.*;

public class MindustryServer implements ApplicationListener{
    private String[] args;

    public MindustryServer(String[] args){
        this.args = args;
    }

    @Override
    public void init(){
        Vars.init();

        headless = true;

        BundleLoader.load();
        content.verbose(false);
        content.load();
        content.initialize(Content::init);

        Core.app.addListener(logic = new Logic());
        Core.app.addListener(world = new World());
        Core.app.addListener(netServer = new NetServer());
        Core.app.addListener(new ServerControl(args));
    }
}
