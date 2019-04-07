package io.anuke.mindustry.desktop;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.PropertiesUtils;
import io.anuke.arc.util.serialization.JsonValue;
import io.anuke.arc.util.serialization.JsonValue.ValueType;
import io.anuke.arc.util.serialization.JsonWriter.OutputType;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.net.Net;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CrashHandler{

    public static void handle(Throwable e){
        e.printStackTrace();
        if(Version.number == 0){
            try{
                ObjectMap<String, String> map = new ObjectMap<>();
                PropertiesUtils.load(map, new InputStreamReader(CrashHandler.class.getResourceAsStream("/version.properties")));

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

        boolean badGPU = false;

        if(e.getMessage() != null && (e.getMessage().contains("Couldn't create window") || e.getMessage().contains("OpenGL 2.0 or higher"))){

            dialog(() -> TinyFileDialogs.tinyfd_messageBox("oh no",
            e.getMessage().contains("Couldn't create window") ? "A graphics initialization error has occured! Try to update your graphics drivers.\nReport this to the developer." :
            "Your graphics card does not support OpenGL 2.0!\n" +
                "Try to update your graphics drivers.\n\n" +
                "(If that doesn't work, your computer just doesn't support Mindustry.)", "ok", "error", true));
            badGPU = true;
        }

        //don't create crash logs for me (anuke) or custom builds, as it's expected
        if(System.getProperty("user.name").equals("anuke") || Version.build == -1) return;

        boolean netActive = false, netServer = false;

        //attempt to close connections, if applicable
        try{
            netActive = Net.active();
            netServer = Net.server();
            Net.dispose();
        }catch(Throwable p){
            p.printStackTrace();
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
        ex(() -> value.addChild("state", new JsonValue(Vars.state.getState().name())));
        ex(() -> value.addChild("os", new JsonValue(System.getProperty("os.name"))));
        ex(() -> value.addChild("trace", new JsonValue(parseException(e))));

        try{
            Path path = Paths.get(OS.getAppDataDirectoryString(Vars.appName), "crashes",
                "crash-report-" + DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss").format(LocalDateTime.now()) + ".txt");
            Files.createDirectories(Paths.get(OS.getAppDataDirectoryString(Vars.appName), "crashes"));
            Files.write(path, parseException(e).getBytes());

            if(!badGPU){
                dialog(() ->  TinyFileDialogs.tinyfd_messageBox("oh no", "A crash has occured. It has been saved in:\n" + path.toAbsolutePath().toString(), "ok", "error", true));
            }
        }catch(Throwable t){
            Log.err("Failed to save local crash report.");
            t.printStackTrace();
        }

        Log.info("Sending crash report.");
        //post to crash report URL
        Net.http(Vars.crashReportURL, "POST", value.toJson(OutputType.json), r -> {
            Log.info("Crash sent successfully.");
            System.exit(1);
        }, t -> {
            t.printStackTrace();
            System.exit(1);
        });

        //sleep for 10 seconds or until crash report is sent
        try{ Thread.sleep(10000); }catch(InterruptedException ignored){}
    }

    private static void dialog(Runnable r){
        new Thread(r).start();
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
