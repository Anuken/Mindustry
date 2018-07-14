package io.anuke.mindustry.server;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.HeadlessFileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.io.BinaryPreferences;
import io.anuke.ucore.util.OS;

import java.io.File;

public class ServerLauncher extends HeadlessApplication{
    ObjectMap<String, Preferences> prefmap;

    public ServerLauncher(ApplicationListener listener, HeadlessApplicationConfiguration config){
        super(listener, config);

        //don't do anything at all for GDX logging: don't want controller info and such
        Gdx.app.setApplicationLogger(new ApplicationLogger(){
            @Override
            public void log(String tag, String message){
            }

            @Override
            public void log(String tag, String message, Throwable exception){
            }

            @Override
            public void error(String tag, String message){
            }

            @Override
            public void error(String tag, String message, Throwable exception){
            }

            @Override
            public void debug(String tag, String message){
            }

            @Override
            public void debug(String tag, String message, Throwable exception){
            }
        });
    }

    public static void main(String[] args){

        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.preferencesDirectory = OS.getAppDataDirectoryString("Mindustry");

        new ServerLauncher(new MindustryServer(args), config);

        //find and handle uncaught exceptions in libGDX thread
        for(Thread thread : Thread.getAllStackTraces().keySet()){
            if(thread.getName().equals("HeadlessApplication")){
                thread.setUncaughtExceptionHandler((t, throwable) -> {
                    throwable.printStackTrace();
                    System.exit(-1);
                });
                break;
            }
        }
    }

    @Override
    public Preferences getPreferences(String name){
        String prefsDirectory = OS.getAppDataDirectoryString("Mindustry");

        if(prefmap == null){
            prefmap = new ObjectMap<>();
        }

        if(prefmap.containsKey(name)){
            return prefmap.get(name);
        }else{
            Preferences prefs = new BinaryPreferences(new HeadlessFileHandle(new File(prefsDirectory, name), FileType.Absolute));
            prefmap.put(name, prefs);
            return prefs;
        }
    }
}