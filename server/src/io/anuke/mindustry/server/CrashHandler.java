package io.anuke.mindustry.server;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.OS;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CrashHandler{

    public static void handle(Throwable e){
        e.printStackTrace();

        //don't create crash logs for me (anuke), as it's expected
        //also don't create logs for custom builds
        if(System.getProperty("user.name").equals("anuke") || Version.build == -1) return;

        //if getting the crash report property failed, OR if it set to false... don't send it
        try{
            if(!Settings.getBool("crashreport")) return;
        }catch(Throwable ignored){
            return;
        }

        //attempt to close connections, if applicable
        try{
            Net.dispose();
        }catch(Throwable p){
            p.printStackTrace();
        }

        JsonValue value = new JsonValue(ValueType.object);

        //add all relevant info, ignoring exceptions
        ex(() -> value.addChild("versionType", new JsonValue(Version.type)));
        ex(() -> value.addChild("versionNumber", new JsonValue(Version.number)));
        ex(() -> value.addChild("versionModifier", new JsonValue(Version.modifier)));
        ex(() -> value.addChild("build", new JsonValue(Version.build)));
        ex(() -> value.addChild("mode", new JsonValue(Vars.state.mode.name())));
        ex(() -> value.addChild("state", new JsonValue(Vars.state.getState().name())));
        ex(() -> value.addChild("difficulty", new JsonValue(Vars.state.difficulty.name())));
        ex(() -> value.addChild("players", new JsonValue(Vars.playerGroup.size())));
        ex(() -> value.addChild("os", new JsonValue(System.getProperty("os.name"))));
        ex(() -> value.addChild("trace", new JsonValue(parseException(e))));

        try{
            Path path = Paths.get(OS.getAppDataDirectoryString(Vars.appName), "crashes",
                "crash-report-" + DateTimeFormatter.ofPattern("MM-dd-yyyy-HH:mm:ss").format(LocalDateTime.now()) + ".txt");
            Files.createDirectories(Paths.get(OS.getAppDataDirectoryString(Vars.appName), "crashes"));
            Files.write(path, parseException(e).getBytes());

            Log.info("Saved crash report at {0}", path.toAbsolutePath().toString());
        }catch(Throwable t){
            Log.err("Failure saving crash report: ");
            t.printStackTrace();
        }

        Log.info("&lcSending crash report.");
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
