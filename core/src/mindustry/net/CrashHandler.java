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

public class CrashHandler{

    public static String createReport(Throwable exception){
        String error = writeException(exception);
        LoadedMod cause = getModCause(exception);

        String report = cause == null ? "Mindustry has crashed. How unfortunate.\n" : "The mod '" +  cause.meta.displayName + "' (" + cause.name + ")" + " has caused Mindustry to crash.\n";
        if(mods != null && mods.list().size == 0 && Version.build != -1){
            report += "Report this at " + Vars.reportIssueURL + "\n\n";
        }

        var enabledMods = mods == null ? null : mods.list().select(m -> m.shouldBeEnabled() && m.isSupported());

        return report
        + "Version: " + Version.combined() + (Version.buildDate.equals("unknown") ? "" : " (Built " + Version.buildDate + ")") + (Vars.headless ? " (Server)" : "") + "\n"
        + "Date: " + new SimpleDateFormat("MMMM d, yyyy HH:mm:ss a", Locale.getDefault()).format(new Date()) + "\n"
        + "OS: " + OS.osName + (OS.osArchBits != null ? " x" + (OS.osArchBits) : "") + " (" + OS.osArch + ")\n" +
        (graphics == null || graphics.getGLVersion() == null ? "" : "GL Version: " + graphics.getGLVersion() + "\n")
        + ((OS.isAndroid || OS.isIos) && app != null ? "Android API level: " + Core.app.getVersion() + "\n" : "")
        + "Java Version: " + OS.javaVersion + "\n"
        + "Runtime Available Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "mb\n"
        + "Cores: " + OS.cores + "\n"
        + (cause == null ? "" : "Likely Cause: " + cause.meta.displayName + " (" + cause.name + " v" + cause.meta.version + ")\n")
        + (enabledMods == null ? "<no mod init>" : "Mods: " + (enabledMods.isEmpty() ? "none (vanilla)" : enabledMods.toString(", ", mod -> mod.name + ":" + mod.meta.version))) + "\n"
        + (state != null && state.patcher != null && state.patcher.patches != null && state.patcher.patches.size > 0 ? "Patches: \n" + state.patcher.patches.toString("\n---\n", p -> p.patch) + "\n" : "")
        + "\n\n" + error;
    }

    public static void log(Throwable exception){
        try{
            Core.settings.getDataDirectory().child("crashes").child("crash_" + System.currentTimeMillis() + ".txt")
            .writeString(createReport(exception));
        }catch(Throwable ignored){
        }
    }

    public static void handle(Throwable exception, Cons<File> writeListener){
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
                System.exit(1);
            }

            //attempt to load version regardless
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
                }catch(Throwable e){
                    e.printStackTrace();
                    Log.err("Failed to parse version.");
                }
            }

            try{
                File file = new File(OS.getAppDataDirectoryString(Vars.appName), "crashes/crash-report-" + new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss").format(new Date()) + ".txt");
                new Fi(OS.getAppDataDirectoryString(Vars.appName)).child("crashes").mkdirs();
                new Fi(file).writeString(createReport(exception));
                writeListener.get(file);
            }catch(Throwable e){
                Log.err("Failed to save local crash report.", e);
            }

            //attempt to close connections, if applicable
            try{
                net.dispose();
            }catch(Throwable ignored){
            }

        }catch(Throwable death){
            death.printStackTrace();
        }

        System.exit(1);
    }

    /** @return the mod that is likely to have caused the supplied crash */
    public static @Nullable LoadedMod getModCause(Throwable e){
        if(Vars.mods == null) return null;
        try{
            for(var element : e.getStackTrace()){
                String name = element.getClassName();
                if(!name.matches("(mindustry|arc|java|javax|sun|jdk)\\..*")){
                    for(var mod : mods.list()){
                        if(mod.meta.main != null && getMatches(mod.meta.main, name) > 0){
                            return mod;
                        }else if(element.getFileName() != null && element.getFileName().endsWith(".js") && element.getFileName().startsWith(mod.name + "/")){
                            return mod;
                        }
                    }
                }
            }
        }catch(Throwable ignored){}
        return null;
    }

    private static int getMatches(String name1, String name2){
        String[] arr1 = name1.split("\\."), arr2 = name2.split("\\.");
        int matches = 0;
        for(int i = 0; i < Math.min(arr1.length, arr2.length); i++){

            if(!arr1[i].equals(arr2[i])){
                return i;
            }else if(!arr1[i].matches("net|org|com|io")){ //ignore common domain prefixes, as that's usually not enough to call something a "match"
                matches ++;
            }
        }
        return matches;
    }

    private static String writeException(Throwable e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
