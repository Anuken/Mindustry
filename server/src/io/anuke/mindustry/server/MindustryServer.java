package io.anuke.mindustry.server;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Logic;
import io.anuke.mindustry.core.NetServer;
import io.anuke.mindustry.core.World;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.ucore.modules.ModuleCore;

import static io.anuke.mindustry.Vars.*;

public class MindustryServer extends ModuleCore{
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

        module(logic = new Logic());
        module(world = new World());
        module(netServer = new NetServer());
        module(new ServerControl(args));
    }
}
