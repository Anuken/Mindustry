package mindustry.mod;

import arc.files.*;
import arc.util.*;
import mindustry.*;

public class Mod{
    /** @return the config file for this plugin, as the file 'mods/[plugin-name]/config.json'.*/
    public Fi getConfig(){
        return Vars.mods.getConfig(this);
    }

    /** Called after all plugins have been created and commands have been registered.*/
    public void init(){

    }

    /** Called on clientside mods. Load content here. */
    public void loadContent(){

    }

    /** Register any commands to be used on the server side, e.g. from the console. */
    public void registerServerCommands(CommandHandler handler){

    }

    /** Register any commands to be used on the client side, e.g. sent from an in-game player.. */
    public void registerClientCommands(CommandHandler handler){

    }
}
