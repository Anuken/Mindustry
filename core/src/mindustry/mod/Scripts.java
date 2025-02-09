package mindustry.mod;

import arc.*;
import arc.files.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import rhino.*;
import rhino.module.*;
import rhino.module.provider.*;

import java.net.*;
import java.util.*;
import java.util.regex.*;

public class Scripts implements Disposable{
    public final Context context;
    public final Scriptable scope;

    private boolean errored;
    LoadedMod currentMod = null;

    public Scripts(){
        Time.mark();

        context = Vars.platform.getScriptContext();
        scope = new ImporterTopLevel(context);

        new RequireBuilder()
            .setModuleScriptProvider(new SoftCachingModuleScriptProvider(new ScriptModuleProvider()))
            .setSandboxed(true).createRequire(context, scope).install(scope);

        if(!run(Core.files.internal("scripts/global.js").readString(), "global.js", false)){
            errored = true;
        }
        Log.debug("Time to load script engine: @", Time.elapsed());
    }

    public boolean hasErrored(){
        return errored;
    }

    public String runConsole(String text){
        try{
            Object o = context.evaluateString(scope, text, "console.js", 1);
            if(o instanceof NativeJavaObject n) o = n.unwrap();
            if(o == null) o = "null";

            else if(o instanceof Undefined) o = "undefined";

            else if(o instanceof Object[] arr) o = Arrays.toString(arr);
            else if(o instanceof int[] arr) o = Arrays.toString(arr);
            else if(o instanceof float[] arr) o = Arrays.toString(arr);
            else if(o instanceof byte[] arr) o = Arrays.toString(arr);
            else if(o instanceof double[] arr) o = Arrays.toString(arr);
            else if(o instanceof long[] arr) o = Arrays.toString(arr);
            else if(o instanceof char[] arr) o = Arrays.toString(arr);
            else if(o instanceof boolean[] arr) o = Arrays.toString(arr);

            var out = o.toString();
            return out == null ? "null" : out;
        }catch(Throwable t){
            return getError(t, false);
        }
    }

    private String getError(Throwable t, boolean log){
        if(log) Log.err(t);
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }

    public void log(String source, String message){
        log(LogLevel.info, source, message);
    }

    public void log(LogLevel level, String source, String message){
        Log.log(level, "[@]: @", source, message);
    }

    public float[] newFloats(int capacity){
        return new float[capacity];
    }

    public void run(LoadedMod mod, Fi file){
        currentMod = mod;
        run(file.readString(), mod.name + "/" + file.name(), true);
        currentMod = null;
    }

    private boolean run(String script, String file, boolean wrap){
        try{
            if(currentMod != null){
                //inject script info into file
                context.evaluateString(scope, "modName = \"" + currentMod.name + "\"\nscriptName = \"" + file + "\"", "initscript.js", 1);
            }
            context.evaluateString(scope, wrap ? "(function(){'use strict';\n" + script + "\n})();" : script, file, 0);
            return true;
        }catch(Throwable t){
            log(LogLevel.err, file, getError(t, true));
            return false;
        }
    }

    @Override
    public void dispose(){
        Context.exit();
    }

    private class ScriptModuleProvider extends UrlModuleSourceProvider{
        private final Pattern directory = Pattern.compile("^(.+?)/(.+)");

        public ScriptModuleProvider(){
            super(null, null);
        }

        @Override
        public ModuleSource loadSource(String moduleId, Scriptable paths, Object validator) throws URISyntaxException{
            if(currentMod == null) return null;
            return loadSource(currentMod, moduleId, currentMod.root.child("scripts"), validator);
        }

        private ModuleSource loadSource(LoadedMod sourceMod, String moduleId, Fi root, Object validator) throws URISyntaxException{
            Matcher matched = directory.matcher(moduleId);
            if(matched.find()){
                LoadedMod required = Vars.mods.locateMod(matched.group(1));
                String script = matched.group(2);
                if(required == null){ // Mod not found, treat it as a folder
                    Fi dir = root.child(matched.group(1));
                    if(!dir.exists()) return null; // Mod and folder not found
                    return loadSource(sourceMod, script, dir, validator);
                }

                currentMod = required;
                return loadSource(sourceMod, script, required.root.child("scripts"), validator);
            }

            Fi module = root.child(moduleId + ".js");
            if(!module.exists() || module.isDirectory()) return null;
            return new ModuleSource(module.reader(Vars.bufferSize), new URI(sourceMod.name + "/" + moduleId + ".js"), root.file().toURI(), validator);
        }
    }
}
