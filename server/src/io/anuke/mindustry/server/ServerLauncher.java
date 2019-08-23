package io.anuke.mindustry.server;


import io.anuke.arc.backends.headless.HeadlessApplication;
import io.anuke.mindustry.net.*;

public class ServerLauncher{

    public static void main(String[] args){
        try{
            Net.setClientProvider(new MClient());
            Net.setServerProvider(new MServer());
            new HeadlessApplication(new MindustryServer(args), null, throwable -> CrashSender.send(throwable, f -> {}));
        }catch(Throwable t){
            CrashSender.send(t, f -> {});
        }
    }
}