package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.Log.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.mod.Mods.*;
import org.mozilla.javascript.*;

public class Scripts implements Disposable{
    private final Context context;
    private final String wrapper;
    private Scriptable scope;
    private boolean errored;

    public Scripts(){
        Time.mark();

        context = Vars.platform.getScriptContext();
        context.setClassShutter(type -> (ClassAccess.allowedClassNames.contains(type) || type.startsWith("$Proxy") ||
            type.startsWith("adapter") || type.contains("PrintStream") ||
            type.startsWith("io.anuke.mindustry")) && !type.equals("io.anuke.mindustry.mod.ClassAccess"));

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
