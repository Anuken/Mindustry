package io.anuke.mindustry.server;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.util.EmptyLogger;
import io.anuke.ucore.util.OS;

public class ServerLauncher extends HeadlessApplication{
    ObjectMap<String, Preferences> prefmap;

    public ServerLauncher(ApplicationListener listener, HeadlessApplicationConfiguration config){
        super(listener, config);

        //don't do anything at all for GDX logging: don't want controller info and such
        Gdx.app.setApplicationLogger(new EmptyLogger());
    }

    public static void main(String[] args){
        try{

            Net.setClientProvider(new KryoClient());
            Net.setServerProvider(new KryoServer());

            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            config.preferencesDirectory = OS.getAppDataDirectoryString("Mindustry");

            new ServerLauncher(new MindustryServer(args), config);
        }catch(Throwable t){
            CrashHandler.handle(t);
        }

        //find and handle uncaught exceptions in libGDX thread
        for(Thread thread : Thread.getAllStackTraces().keySet()){
            if(thread.getName().equals("HeadlessApplication")){
                thread.setUncaughtExceptionHandler((t, throwable) -> {
                    CrashHandler.handle(throwable);
                    System.exit(-1);
                });
                break;
            }
        }
    }
}