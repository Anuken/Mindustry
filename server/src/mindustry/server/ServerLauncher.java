package mindustry.server;


import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import mindustry.net.Net;
import mindustry.net.*;

import java.time.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;
import static mindustry.server.ServerControl.*;

public class ServerLauncher implements ApplicationListener{
    static String[] args;

    public static void main(String[] args){
        try{
            ServerLauncher.args = args;
            Vars.platform = new Platform(){};
            Vars.net = new Net(platform.getNet());

            Log.setLogger((level, text) -> {
                String result = "[" + dateTime.format(LocalDateTime.now()) + "] " + format(tags[level.ordinal()] + " " + text + "&fr");
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
                    Log.err("| &ly[@]", mod.name);
                    for(Content cont : mod.erroredContent){
                        Log.err("| | &y@: &c@", cont.minfo.sourceFile.name(), Strings.getSimpleMessage(cont.minfo.baseError).replace("\n", " "));
                    }
                }
            }
            Log.err("The server will now exit.");
            System.exit(1);
        }

        bases.load();

        Core.app.addListener(new ApplicationListener(){public void update(){ asyncCore.begin(); }});
        Core.app.addListener(logic = new Logic());
        Core.app.addListener(netServer = new NetServer());
        Core.app.addListener(new ServerControl(args));
        Core.app.addListener(new ApplicationListener(){public void update(){ asyncCore.end(); }});

        mods.eachClass(Mod::init);

        Events.fire(new ServerLoadEvent());
    }

}
