package io.anuke.mindustry.server;

import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
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

        FileHandle plugins = Core.settings.getDataDirectory().child("plugins");
        if(plugins.isDirectory() && plugins.list().length > 0 && !plugins.sibling("mods").exists()){
            Log.warn("[IMPORTANT NOTICE] &lrPlugins have been detected.&ly Automatically moving all contents of the plugin folder into the 'mods' folder. The original folder will not be removed; please do so manually.");
            plugins.sibling("mods").mkdirs();
            for(FileHandle file : plugins.list()){
                file.copyTo(plugins.sibling("mods"));
            }
        }

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
