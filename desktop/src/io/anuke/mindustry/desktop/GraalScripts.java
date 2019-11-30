package io.anuke.mindustry.desktop;

import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.mod.*;
import io.anuke.mindustry.mod.Mods.*;
import org.graalvm.polyglot.*;

import java.io.*;

public class GraalScripts extends Scripts{
    private static final Class[] denied = {FileHandle.class, InputStream.class, File.class, Scripts.class, Files.class, ClassAccess.class};
    private final Context context;
    private final String wrapper;

    public GraalScripts(){
        Time.mark();
        Context.Builder builder = Context.newBuilder("js").allowHostClassLookup(ClassAccess.allowedClassNames::contains);

        HostAccess.Builder hb = HostAccess.newBuilder();
        hb.allowPublicAccess(true);
        for(Class c : denied){
            hb.denyAccess(c);
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
