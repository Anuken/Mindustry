package io.anuke.mindustry.server;

import io.anuke.arc.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.mod.*;

import static io.anuke.mindustry.Vars.*;

public class MindustryServer implements ApplicationListener{
    private String[] args;

    public MindustryServer(String[] args){
        this.args = args;
    }

    @Override
    public void init(){
        Core.settings.setDataDirectory(Core.files.local("config"));
        loadLocales = false;
        headless = true;

        Vars.loadSettings();
        Vars.init();
        content.createContent();
        content.init();

        Core.app.addListener(logic = new Logic());
        Core.app.addListener(netServer = new NetServer());
        Core.app.addListener(new ServerControl(args));

        mods.each(Mod::init);
    }


}
