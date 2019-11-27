package io.anuke.mindustry.mod;

import org.graalvm.polyglot.*;

public class Scripts{
    //TODO allowHostAccess(...) is obviously insecure
    private Context context = Context.newBuilder("js").allowHostClassLookup(s -> s.startsWith("io.anuke.mindustry")).allowHostAccess(HostAccess.ALL).build();

    public Scripts(){
        context.eval("js", "console.log(\"Initialized JS context.\")");
    }

    public void run(String script){
        context.eval("js", script);
    }
}
