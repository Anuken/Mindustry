package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.mod.Mods.*;
import org.graalvm.polyglot.*;

public class Scripts{
    private final Context context;
    private final String wrapper;

    public Scripts(){
        Time.mark();
        Context.Builder builder = Context.newBuilder("js").allowHostClassLookup(ClassAccess.allowedClassNames::contains);

        HostAccess.Builder hb = HostAccess.newBuilder();
        for(Class c : ClassAccess.allowedClasses){
            hb.allowImplementations(c);
            Structs.each(hb::allowAccess, c.getConstructors());
            Structs.each(hb::allowAccess, c.getFields());
            Structs.each(hb::allowAccess, c.getMethods());
        }
        builder.allowHostAccess(hb.build());

        context = builder.build();
        wrapper = Core.files.internal("scripts/wrapper.js").readString();

        run(Core.files.internal("scripts/global.js").readString());
        Log.info("Time to load script engine: {0}", Time.elapsed());
    }

    public void run(LoadedMod mod, FileHandle file){
        run(wrapper.replace("$SCRIPT_NAME$", mod.name + "_" +file.nameWithoutExtension().replace("-", "_").replace(" ", "_")).replace("$CODE$", file.readString()));
    }

    private void run(String script){
        context.eval("js", script);
    }
}
