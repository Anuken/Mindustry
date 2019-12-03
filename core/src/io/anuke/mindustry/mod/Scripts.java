package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.mod.Mods.*;
import org.mozilla.javascript.*;

import java.io.*;

public class Scripts{
    private static final Class[] denied = {FileHandle.class, InputStream.class, File.class, Scripts.class, Files.class, ClassAccess.class};
    private final Context context;
    private final String wrapper;
    private Context console;
    private Scriptable scope;

    public Scripts(){
        Time.mark();

        context = Context.enter();
        if(Vars.mobile){
            context.setOptimizationLevel(-1);
        }

        scope = context.initStandardObjects();
        wrapper = Core.files.internal("scripts/wrapper.js").readString();

        run(wrapper);
        Log.info("Time to load script engine: {0}", Time.elapsed());
    }

    public void run(LoadedMod mod, FileHandle file){
        run(wrapper.replace("$SCRIPT_NAME$", mod.name + "_" +file.nameWithoutExtension().replace("-", "_").replace(" ", "_")).replace("$CODE$", file.readString()));
    }

    private void run(String script){
        Log.info(context.evaluateString(scope, script, "???", 0, null));
    }
}
