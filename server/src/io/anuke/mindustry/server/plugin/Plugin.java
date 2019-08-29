package io.anuke.mindustry.server.plugin;

import io.anuke.arc.util.*;

public abstract class Plugin{

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
