package mindustry.net;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.mod.Mods.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class CrashSender{

    public static String createReport(String error){
        String report = "Mindustry has crashed. How unfortunate.\n";
        if(mods != null && mods.list().size == 0 && Version.build != -1){
            report += "Report this at " + Vars.reportIssueURL + "\n\n";
        }
        return report
        + "Version: " + Version.combined() + (Vars.headless ? " (Server)" : "") + "\n"
        + "OS: " + OS.osName + " x" + (OS.osArchBits) + " (" + OS.osArch + ")\n"
        + ((OS.isAndroid || OS.isIos) && app != null ? "Android API level: " + Core.app.getVersion() + "\n" : "")
        + "Java Version: " + OS.javaVersion + "\n"
        + "Runtime Available Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "mb\n"
        + "Cores: " + Runtime.getRuntime().availableProcessors() + "\n"
        + (mods == null ? "<no mod init>" : "Mods: " + (!mods.list().contains(LoadedMod::shouldBeEnabled) ? "none (vanilla)" : mods.list().select(LoadedMod::shouldBeEnabled).toString(", ", mod -> mod.name + ":" + mod.meta.version)))
        + "\n\n" + error;
    }

    public static void log(Throwable exception){
        try{
            Core.settings.getDataDirectory().child("crashes").child("crash_" + System.currentTimeMillis() + ".txt")
            .writeString(createReport(Strings.neatError(exception)));
        }catch(Throwable ignored){
        }
    }

    public static void send(Throwable exception, Cons<File> writeListener){
        try{
            try{
                //log to file
                Log.err(exception);
            }catch(Throwable no){
                exception.printStackTrace();
            }

            //try saving game data
            try{
                settings.manualSave();
            }catch(Throwable ignored){}

            //don't create crash logs for custom builds, as it's expected
            if(OS.username.equals("anuke") && !"steam".equals(Version.modifier)){
                ret();
            }

            //attempt to load version regardless
            if(Version.number == 0){
                try{
                    ObjectMap<String, String> map = new ObjectMap<>();
                    PropertiesUtils.load(map, new InputStreamReader(CrashSender.class.getResourceAsStream("/version.properties")));

                    Version.type = map.get("type");
                    Version.number = Integer.parseInt(map.get("number"));
                    Version.modifier = map.get("modifier");
                    if(map.get("build").contains(".")){
                        String[] split = map.get("build").split("\\.");
                        Version.build = Integer.parseInt(split[0]);
                        Version.revision = Integer.parseInt(split[1]);
                    }else{
                        Version.build = Strings.canParseInt(map.get("build")) ? Integer.parseInt(map.get("build")) : -1;
                    }
                }catch(Throwable e){
                    e.printStackTrace();
                    Log.err("Failed to parse version.");
                }
            }

            try{
                File file = new File(OS.getAppDataDirectoryString(Vars.appName), "crashes/crash-report-" + new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss").format(new Date()) + ".txt");
                new Fi(OS.getAppDataDirectoryString(Vars.appName)).child("crashes").mkdirs();
                new Fi(file).writeString(createReport(parseException(exception)));
                writeListener.get(file);
            }catch(Throwable e){
                Log.err("Failed to save local crash report.", e);
            }

            try{
                //check crash report setting
                if(!Core.settings.getBool("crashreport", true)){
                    ret();
                }
            }catch(Throwable ignored){
                //if there's no settings init we don't know what the user wants but chances are it's an important crash, so send it anyway
            }

            try{
                //check any mods - if there are any, don't send reports
                if(Vars.mods != null && !Vars.mods.list().isEmpty()){
                    ret();
                }
            }catch(Throwable ignored){
            }

            //do not send exceptions that occur for versions that can't be parsed
            if(Version.number == 0){
                ret();
            }

            boolean netActive = false, netServer = false;

            //attempt to close connections, if applicable
            try{
                netActive = net.active();
                netServer = net.server();
                net.dispose();
            }catch(Throwable ignored){
            }

            //disabled until further notice.
            /*

            JsonValue value = new JsonValue(ValueType.object);

            boolean fn = netActive, fs = netServer;

            //add all relevant info, ignoring exceptions
            ex(() -> value.addChild("versionType", new JsonValue(Version.type)));
            ex(() -> value.addChild("versionNumber", new JsonValue(Version.number)));
            ex(() -> value.addChild("versionModifier", new JsonValue(Version.modifier)));
            ex(() -> value.addChild("build", new JsonValue(Version.build)));
            ex(() -> value.addChild("revision", new JsonValue(Version.revision)));
            ex(() -> value.addChild("net", new JsonValue(fn)));
            ex(() -> value.addChild("server", new JsonValue(fs)));
            ex(() -> value.addChild("players", new JsonValue(Groups.player.size())));
            ex(() -> value.addChild("state", new JsonValue(Vars.state.getState().name())));
            ex(() -> value.addChild("os", new JsonValue(OS.osName + " x" + OS.osArchBits + " " + OS.osVersion)));
            ex(() -> value.addChild("trace", new JsonValue(parseException(exception))));
            ex(() -> value.addChild("javaVersion", new JsonValue(OS.javaVersion)));
            ex(() -> value.addChild("javaArch", new JsonValue(OS.osArchBits)));
            
            Log.info("Sending crash report.");

            //post to crash report URL, exit code indicates send success
            Http.post(Vars.crashReportURL, value.toJson(OutputType.json)).error(t -> {
                Log.info("Crash report not sent.");
                System.exit(-1);
            }).block(r -> {
                Log.info("Crash sent successfully.");
                System.exit(1);
            });*/

            ret();
        }catch(Throwable death){
            death.printStackTrace();
        }

        ret();
    }

    private static void ret(){
        System.exit(1);
    }

    private static String parseException(Throwable e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private static void ex(Runnable r){
        try{
            r.run();
        }catch(Throwable ignored){
        }
    }
}
