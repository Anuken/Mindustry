package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.mod.Mods.*;
import org.mozilla.javascript.*;

import static io.anuke.mindustry.Vars.*;

public class Scripts implements Disposable{
    private final Context context;
    private final String wrapper;
    private Scriptable scope;
    private Array<String> logBuffer = new Array<>();

    public Scripts(){
        Time.mark();

        context = Vars.platform.getScriptContext();
        context.setClassShutter(type -> (ClassAccess.allowedClassNames.contains(type) || type.startsWith("adapter") || type.contains("PrintStream") || type.startsWith("io.anuke.mindustry")) && !type.equals("io.anuke.mindustry.mod.ClassAccess"));

        scope = new ImporterTopLevel(context);
        wrapper = Core.files.internal("scripts/wrapper.js").readString();

        run(Core.files.internal("scripts/global.js").readString(), "global.js");
        Log.info("Time to load script engine: {0}", Time.elapsed());
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
        t = Strings.getFinalCause(t);
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }

    public void log(String source, String message){
        Log.info("[{0}]: {1}", source, message);
        logBuffer.add("[accent][" + source + "]:[] " + message);
        if(!headless & ui.scriptfrag != null){
            onLoad();
        }
    }

    public void onLoad(){
        if(!headless){
            logBuffer.each(ui.scriptfrag::addMessage);
        }
        logBuffer.clear();
    }

    public void run(LoadedMod mod, FileHandle file){
        run(wrapper.replace("$SCRIPT_NAME$", mod.name + "/" + file.nameWithoutExtension()).replace("$CODE$", file.readString()).replace("$MOD_NAME$", mod.name), file.name());
    }

    private void run(String script, String file){
        try{
            context.evaluateString(scope, script, file, 1, null);
        }catch(Throwable t){
            log(file, "[scarlet]" + getError(t));
        }
    }

    @Override
    public void dispose(){
        Context.exit();
    }
}
