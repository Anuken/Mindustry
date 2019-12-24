package io.anuke.mindustry.server;


import io.anuke.arc.*;
import io.anuke.arc.backends.headless.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.mod.*;
import io.anuke.mindustry.mod.Mods.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.*;

import java.time.*;

import static io.anuke.arc.util.Log.format;
import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.server.ServerControl.*;

public class ServerLauncher implements ApplicationListener{
    static String[] args;

    public static void main(String[] args){
        try{
            ServerLauncher.args = args;
            Vars.platform = new Platform(){};
            Vars.net = new Net(platform.getNet());

            Log.setLogger((level, text, args1) -> {
                String result = "[" + dateTime.format(LocalDateTime.now()) + "] " + format(tags[level.ordinal()] + " " + text + "&fr", args1);
                System.out.println(result);
            });
            new HeadlessApplication(new ServerLauncher(), null, throwable -> CrashSender.send(throwable, f -> {}));
        }catch(Throwable t){
            CrashSender.send(t, f -> {});
        }
    }

    @Override
    public void init(){
        Core.settings.setDataDirectory(Core.files.local("config"));
        loadLocales = false;
        headless = true;

        Fi plugins = Core.settings.getDataDirectory().child("plugins");
        if(plugins.isDirectory() && plugins.list().length > 0 && !plugins.sibling("mods").exists()){
            Log.warn("[IMPORTANT NOTICE] &lrPlugins have been detected.&ly Automatically moving all contents of the plugin folder into the 'mods' folder. The original folder will not be removed; please do so manually.");
            plugins.sibling("mods").mkdirs();
            for(Fi file : plugins.list()){
                file.copyTo(plugins.sibling("mods"));
            }
        }

        Vars.loadSettings();
        Vars.init();
        content.createBaseContent();
        mods.loadScripts();
        content.createModContent();
        content.init();
        if(mods.hasContentErrors()){
            Log.err("Error occurred loading mod content:");
            for(LoadedMod mod : mods.list()){
                if(mod.hasContentErrors()){
                    Log.err("| &ly[{0}]", mod.name);
                    for(Content cont : mod.erroredContent){
                        Log.err("| | &y{0}: &c{1}", cont.minfo.sourceFile.name(), Strings.getSimpleMessage(cont.minfo.baseError).replace("\n", " "));
                    }
                }
            }
            Log.err("The server will now exit.");
            System.exit(1);
        }

        Core.app.addListener(logic = new Logic());
        Core.app.addListener(netServer = new NetServer());
        Core.app.addListener(new ServerControl(args));

        mods.eachClass(Mod::init);

        Events.fire(new ServerLoadEvent());
    }
}