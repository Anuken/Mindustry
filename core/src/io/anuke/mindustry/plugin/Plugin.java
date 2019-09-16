package io.anuke.mindustry.plugin;

import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;

public abstract class Plugin{

    /** @return the config file for this plugin, as the file 'plugins/[plugin-name]/config.json'.*/
    public FileHandle getConfig(){
        return Vars.plugins.getConfig(this);
    }

    /** Called after all plugins have been created and commands have been registered.*/
    public void init(){

    }

    /** Register any commands to be used on the server side, e.g. from the console. */
    public void registerServerCommands(CommandHandler handler){

    }

    /** Register any commands to be used on the client side, e.g. sent from an in-game player.. */
    public void registerClientCommands(CommandHandler handler){

    }
}
