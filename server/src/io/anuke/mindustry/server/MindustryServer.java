package io.anuke.mindustry.server;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.ucore.modules.ModuleCore;

import static io.anuke.mindustry.Vars.*;

public class MindustryServer extends ModuleCore {
    private String[] args;

    public MindustryServer(String[] args){
        this.args = args;
    }

    @Override
    public void init(){
        Vars.init();

        headless = true;

        BundleLoader.load();
        ContentLoader.load();
        ContentLoader.initialize(Content::init);

        module(logic = new Logic());
        module(world = new World());
        module(netServer = new NetServer());
        module(netCommon = new NetCommon());
        module(new ServerControl(args));
    }
}
