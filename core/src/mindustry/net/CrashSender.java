package mindustry.net;

import arc.*;
import arc.Net.*;
import arc.struct.*;
import arc.files.*;
import arc.func.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.JsonWriter.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.net;

public class CrashSender{

    public static void log(Throwable exception){
        try{
            Core.settings.getDataDirectory().child("crashes").child("crash_" + System.currentTimeMillis() + ".txt").writeString(Strings.neatError(exception));
        }catch(Throwable ignored){
        }
    }

    public static void send(Throwable exception, Cons<File> writeListener){

        try{
            exception.printStackTrace();

            //try saving game data
            try{
                settings.manualSave();
            }catch(Throwable ignored){}

            //don't create crash logs for custom builds, as it's expected
            if(Version.build == -1 || (System.getProperty("user.name").equals("anuke") && "release".equals(Version.modifier))){
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
                new Fi(file).writeString(parseException(exception));
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
            ex(() -> value.addChild("os", new JsonValue(System.getProperty("os.name") + "x" + (OS.is64Bit ? "64" : "32"))));
            ex(() -> value.addChild("trace", new JsonValue(parseException(exception))));
            ex(() -> value.addChild("javaVersion", new JsonValue(System.getProperty("java.version"))));
            ex(() -> value.addChild("javaArch", new JsonValue(System.getProperty("sun.arch.data.model"))));

            boolean[] sent = {false};

            Log.info("Sending crash report.");
            //post to crash report URL
            httpPost(Vars.crashReportURL, value.toJson(OutputType.json), r -> {
                Log.info("Crash sent successfully.");
                sent[0] = true;
                System.exit(1);
            }, t -> {
                t.printStackTrace();
                sent[0] = true;
                System.exit(1);
            });

            //sleep until report is sent
            try{
                while(!sent[0]){
                    Thread.sleep(30);
                }
            }catch(InterruptedException ignored){}
        }catch(Throwable death){
            death.printStackTrace();
        }

        ret();
    }

    private static void ret(){
        System.exit(1);
    }

    private static void httpPost(String url, String content, Cons<HttpResponse> success, Cons<Throwable> failure){
        new NetJavaImpl().http(new HttpRequest().method(HttpMethod.POST).content(content).url(url), success, failure);
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
