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
        ex(() -> value.addChild("build", new JsonValue(Version.build)));
        ex(() -> value.addChild("net", new JsonValue(fn)));
        ex(() -> value.addChild("server", new JsonValue(fs)));
        ex(() -> value.addChild("gamemode", new JsonValue(Vars.state.mode.toString())));
        ex(() -> value.addChild("os", new JsonValue(System.getProperty("os.name"))));
        ex(() -> value.addChild("multithreading", new JsonValue(Settings.getBool("multithread"))));
        ex(() -> value.addChild("trace", new JsonValue(parseException(e))));

        Log.info("Sending crash report.");
        //post to crash report URL
        Net.http(Vars.crashReportURL, "POST", value.toJson(OutputType.json), r -> System.exit(1), t -> System.exit(1));

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
