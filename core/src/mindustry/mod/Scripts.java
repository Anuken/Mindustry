package mindustry.mod;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import org.mozilla.javascript.*;

public class Scripts implements Disposable{
    private final Array<String> blacklist = Array.with("net", "files", "reflect", "javax", "rhino", "file", "channels", "jdk",
        "runtime", "util.os", "rmi", "security", "org.", "sun.", "beans", "sql", "http", "exec", "compiler", "process", "system",
        ".awt", "socket", "classloader", "oracle", "invoke");
    private final Array<String> whitelist = Array.with("mindustry.net");
    private final Context context;
    private final String wrapper;
    private Scriptable scope;
    private boolean errored;

    public Scripts(){
        Time.mark();

        context = Vars.platform.getScriptContext();
        context.setClassShutter(type -> !blacklist.contains(type.toLowerCase()::contains) || whitelist.contains(type.toLowerCase()::contains));
        context.getWrapFactory().setJavaPrimitiveWrap(false);
        
        scope = new ImporterTopLevel(context);
        wrapper = Core.files.internal("scripts/wrapper.js").readString();

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
        run(wrapper.replace("$SCRIPT_NAME$", mod.name + "/" + file.nameWithoutExtension()).replace("$CODE$", file.readString()).replace("$MOD_NAME$", mod.name), file.name());
    }

    private boolean run(String script, String file){
        try{
            context.evaluateString(scope, script, file, 1, null);
            return true;
        }catch(Throwable t){
            log(LogLevel.err, file, "" + getError(t));
            return false;
        }
    }

    @Override
    public void dispose(){
        Context.exit();
    }
}
