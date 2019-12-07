package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.mod.Mods.*;
import org.mozilla.javascript.*;

public class Scripts implements Disposable{
    private final Context context;
    private final String wrapper;
    private Scriptable scope;

    public Scripts(){
        Time.mark();

        context = Vars.platform.getScriptContext();
        context.setClassShutter(type -> ClassAccess.allowedClassNames.contains(type) || type.startsWith("adapter") || type.contains("PrintStream"));

        scope = new ImporterTopLevel(context);//context.initStandardObjects();
        wrapper = Core.files.internal("scripts/wrapper.js").readString();

        run(Core.files.internal("scripts/global.js").readString(), "global.js");
        Log.info("Time to load script engine: {0}", Time.elapsed());
    }

    public String runConsole(String text){
        try{
            return String.valueOf(context.evaluateString(scope, text, "console.js", 1, null));
        }catch(Throwable t){
            return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
        }
    }

    public void run(LoadedMod mod, FileHandle file){
        run(wrapper.replace("$SCRIPT_NAME$", mod.name + "_" + file.nameWithoutExtension().replace("-", "_").replace(" ", "_")).replace("$CODE$", file.readString()).replace("$MOD_NAME$", mod.name), file.name());
    }

    private void run(String script, String file){
        context.evaluateString(scope, script, file, 1, null);
    }

    @Override
    public void dispose(){
        Context.exit();
    }
}
