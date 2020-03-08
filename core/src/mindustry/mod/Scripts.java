package mindustry.mod;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import org.mozilla.javascript.*;
import org.mozilla.javascript.commonjs.module.*;
import org.mozilla.javascript.commonjs.module.provider.*;

import java.io.*;
import java.net.*;
import java.util.regex.*;

public class Scripts implements Disposable{
    private final Array<String> blacklist = Array.with("net", "files", "reflect", "javax", "rhino", "file", "channels", "jdk",
        "runtime", "util.os", "rmi", "security", "org.", "sun.", "beans", "sql", "http", "exec", "compiler", "process", "system",
        ".awt", "socket", "classloader", "oracle", "invoke");
    private final Array<String> whitelist = Array.with("mindustry.net");
    private final Context context;
    private Scriptable scope;
    private boolean errored;
    private LoadedMod currentMod = null;

    public Scripts(){
        Time.mark();

        context = Vars.platform.getScriptContext();
        context.setClassShutter(type -> !blacklist.contains(type.toLowerCase()::contains) || whitelist.contains(type.toLowerCase()::contains));
        context.getWrapFactory().setJavaPrimitiveWrap(false);

        scope = new ImporterTopLevel(context);

        new RequireBuilder()
            .setModuleScriptProvider(new SoftCachingModuleScriptProvider(new ScriptModuleProvider()))
            .setSandboxed(true).createRequire(context, scope).install(scope);

        if(!run(Core.files.internal("scripts/global.js").readString(), "global.js")){
            errored = true;
        }
        Log.debug("Time to load script engine: {0}", Time.elapsed());
    }

    public boolean hasErrored(){
        return errored;
    }

    public String runConsole(String text){
        try{
            Object o = context.evaluateString(scope, text, "console.js", 1, null);
            if(o instanceof NativeJavaObject){
                o = ((NativeJavaObject)o).unwrap();
            }
            if(o instanceof Undefined){
                o = "undefined";
            }
            return String.valueOf(o);
        }catch(Throwable t){
            return getError(t);
        }
    }

    private String getError(Throwable t){
        t.printStackTrace();
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }

    public void log(String source, String message){
        log(LogLevel.info, source, message);
    }

    public void log(LogLevel level, String source, String message){
        Log.log(level, "[{0}]: {1}", source, message);
    }

    public void run(LoadedMod mod, Fi file){
        currentMod = mod;
        run(file.readString(), file.name());
        currentMod = null;
    }

    private boolean run(String script, String file){
        try{
            if(currentMod != null){
                //inject script info into file (TODO maybe rhino handles this?)
                context.evaluateString(scope, "modName = \"" + currentMod.name + "\"\nscriptName = \"" + file + "\"", "initscript.js", 1, null);
            }
            context.evaluateString(scope, script, file, 1, null);
            return true;
        }catch(Throwable t){
            if(currentMod != null){
                file = currentMod.name + "/" + file;
            }
            log(LogLevel.err, file, "" + getError(t));
            return false;
        }
    }

    @Override
    public void dispose(){
        Context.exit();
    }

    private class ScriptModuleProvider extends UrlModuleSourceProvider{
        private Pattern directory = Pattern.compile("^(.+?)/(.+)");

        public ScriptModuleProvider(){
            super(null, null);
        }

        @Override
        public ModuleSource loadSource(String moduleId, Scriptable paths, Object validator) throws IOException, URISyntaxException{
            if(currentMod == null) return null;
            return loadSource(moduleId, currentMod.root.child("scripts"), validator);
        }

        private ModuleSource loadSource(String moduleId, Fi root, Object validator) throws URISyntaxException{
            Matcher matched = directory.matcher(moduleId);
            if(matched.find()){
                LoadedMod required = Vars.mods.locateMod(matched.group(1));
                String script = matched.group(2);
                if(required == null || root.equals(required.root.child("scripts"))){ // Mod not found, or already using a mod
                    Fi dir = root.child(matched.group(1));
                    if(!dir.exists()) return null; // Mod and folder not found
                    return loadSource(script, dir, validator);
                }

                currentMod = required;
                return loadSource(script, required.root.child("scripts"), validator);
            }

            Fi module = root.child(moduleId + ".js");
            if(!module.exists() || module.isDirectory()) return null;
            return new ModuleSource(
                new InputStreamReader(new ByteArrayInputStream((module.readString()).getBytes())),
                null, new URI(moduleId), root.file().toURI(), validator);
        }
    }
}
