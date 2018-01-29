package io.anuke.mindustry;

import io.anuke.mindustry.core.*;
import io.anuke.mindustry.io.BlockLoader;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.ucore.modules.ModuleCore;

import static io.anuke.mindustry.Vars.*;

public class MindustryServer extends ModuleCore {

    @Override
    public void init(){
        headless = true;

        BundleLoader.load();
        BlockLoader.load();

        module(logic = new Logic());
        module(world = new World());
        module(netServer = new NetServer());
        module(netCommon = new NetCommon());
        module(serverControl = new ServerControl());
    }
}
