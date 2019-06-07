package io.anuke.mindustry.net;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.OS;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.io.PropertiesUtils;
import io.anuke.arc.util.serialization.JsonValue;
import io.anuke.arc.util.serialization.JsonValue.ValueType;
import io.anuke.arc.util.serialization.JsonWriter.OutputType;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Version;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CrashSender{

    public static void send(Throwable exception, Consumer<File> writeListener){
        try{
            exception.printStackTrace();

            //don't create crash logs for me (anuke) or custom builds, as it's expected
            if(System.getProperty("user.name").equals("anuke") || Version.build == -1) return;

            //attempt to load version regardless
            if(Version.number == 0){
                try{
                    ObjectMap<String, String> map = new ObjectMap<>();
                    PropertiesUtils.load(map, new InputStreamReader(CrashSender.class.getResourceAsStream("version.properties")));

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
                }catch(Throwable ignored){
                    Log.err("Failed to parse version.");
                }
            }

            try{
                //check crash report setting
                if(!Core.settings.getBool("crashreport", true)){
                    return;
                }
            }catch(Throwable ignored){
                //if there's no settings init we don't know what the user wants but chances are it's an important crash, so send it anyway
            }

            //do not send exceptions that occur for versions that can't be parsed
            if(Version.number == 0){
                return;
            }

            try{
                File file = new File(OS.getAppDataDirectoryString(Vars.appName), "crashes/crash-report-" + DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss").format(LocalDateTime.now()) + ".txt");
                new File(OS.getAppDataDirectoryString(Vars.appName)).mkdir();
                new BufferedOutputStream(new FileOutputStream(file), 2048).write(parseException(exception).getBytes());
                Files.createDirectories(Paths.get(OS.getAppDataDirectoryString(Vars.appName), "crashes"));

                writeListener.accept(file);
            }catch(Throwable ignored){
                Log.err("Failed to save local crash report.");
            }

            boolean netActive = false, netServer = false;

            //attempt to close connections, if applicable
            try{
                netActive = Net.active();
                netServer = Net.server();
                Net.dispose();
            }catch(Throwable ignored){
            }

            JsonValue value = new JsonValue(ValueType.object);

            boolean fn = netActive, fs = netServer;

            //add all relevant info, ignoring exceptions
            ex(() -> value.addChild("versionType", new JsonValue(Version.type)));
            ex(() -> value.addChild("versionNumber", new JsonValue(Version.number)));
            ex(() -> value.addChild("versionModifier", new JsonValue(Version.modifier)));
            ex(() -> value.addChild("build", new JsonValue(Version.build)));
            ex(() -> value.addChild("net", new JsonValue(fn)));
            ex(() -> value.addChild("server", new JsonValue(fs)));
            ex(() -> value.addChild("players", new JsonValue(Vars.playerGroup.size())));
            ex(() -> value.addChild("state", new JsonValue(Vars.state.getState().name())));
            ex(() -> value.addChild("os", new JsonValue(System.getProperty("os.name"))));
            ex(() -> value.addChild("trace", new JsonValue(parseException(exception))));

            boolean[] sent = {false};

            Log.info("Sending crash report.");
            //post to crash report URL
            Net.http(Vars.crashReportURL, "POST", value.toJson(OutputType.json), r -> {
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
            }catch(InterruptedException ignored){
            }
        }catch(Throwable death){
            death.printStackTrace();
            System.exit(1);
        }
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
        }catch(Throwable t){
            t.printStackTrace();
        }
    }
}
