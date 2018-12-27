package io.anuke.mindustry.server;


import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.backends.headless.HeadlessApplication;
import io.anuke.arc.backends.headless.HeadlessApplicationConfiguration;
import io.anuke.net.KryoClient;
import io.anuke.net.KryoServer;
import io.anuke.mindustry.net.Net;

public class ServerLauncher extends HeadlessApplication{

    public ServerLauncher(ApplicationListener listener, HeadlessApplicationConfiguration config){
        super(listener, config);
    }

    public static void main(String[] args){
        try{

            Net.setClientProvider(new KryoClient());
            Net.setServerProvider(new KryoServer());

            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            Core.settings.setDataDirectory(Core.files.local("config"));

            new ServerLauncher(new MindustryServer(args), config);
        }catch(Throwable t){
            CrashHandler.handle(t);
        }

        //find and handle uncaught exceptions in libGDX thread
        for(Thread thread : Thread.getAllStackTraces().keySet()){
            if(thread.getName().equals("HeadlessApplication")){
                thread.setUncaughtExceptionHandler((t, throwable) -> {
                    try{
                        CrashHandler.handle(throwable);
                        System.exit(-1);
                    }catch(Throwable crashCrash){
                        crashCrash.printStackTrace();
                        System.exit(-1);
                    }
                });
                break;
            }
        }
    }
}