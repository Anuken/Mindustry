package io.anuke.mindustry.server;


import io.anuke.arc.backends.headless.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.net.*;

import static io.anuke.mindustry.Vars.platform;

public class ServerLauncher{

    public static void main(String[] args){
        try{
            Vars.platform = new Platform(){};
            Vars.net = new Net(platform.getNet());
            new HeadlessApplication(new MindustryServer(args), null, throwable -> CrashSender.send(throwable, f -> {}));
        }catch(Throwable t){
            CrashSender.send(t, f -> {});
        }
    }
}