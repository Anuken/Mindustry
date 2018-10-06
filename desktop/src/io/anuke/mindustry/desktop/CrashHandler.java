package io.anuke.mindustry.desktop;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler{

    public static void handle(Throwable e){
        if(e.getMessage() != null && (e.getMessage().contains("Couldn't create window") || e.getMessage().contains("OpenGL 2.0 or higher"))){
            try{
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            }catch(Throwable ignored){}
            javax.swing.JOptionPane.showMessageDialog(null, "Your graphics card does not support OpenGL 2.0!\n" +
                "Try to update your graphics drivers.\n\n" +
                "(If that doesn't work, your computer just doesn't support Mindustry.)",
                "oh no", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }

        e.printStackTrace();

        //don't create crash logs for me (anuke), as it's expected
        //also don't create logs for custom builds
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
        ex(() -> value.addChild("gamemode", new JsonValue(Vars.state.mode.name())));
        ex(() -> value.addChild("state", new JsonValue(Vars.state.getState().name())));
        ex(() -> value.addChild("os", new JsonValue(System.getProperty("os.name"))));
        ex(() -> value.addChild("multithreading", new JsonValue(Settings.getBool("multithread"))));
        ex(() -> value.addChild("trace", new JsonValue(parseException(e))));

        Log.info("Sending crash report.");
        //post to crash report URL
        Net.http(Vars.crashReportURL, "POST", value.toJson(OutputType.json), r -> {
            Log.info("Crash sent successfully.");
            System.exit(1);
        }, t -> {
            t.printStackTrace();
            System.exit(1);
        });

        //sleep forever
        try{ Thread.sleep(Long.MAX_VALUE); }catch(InterruptedException ignored){}
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
