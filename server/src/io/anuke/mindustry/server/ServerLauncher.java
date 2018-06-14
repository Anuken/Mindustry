package io.anuke.mindustry.server;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.net.Net;

public class ServerLauncher extends HeadlessApplication{

    public static void main(String[] args){

        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());

        new ServerLauncher(new MindustryServer(args));

        //find and handle uncaught exceptions in libGDX thread
        for(Thread thread : Thread.getAllStackTraces().keySet()){
            if(thread.getName().equals("HeadlessApplication")){
                thread.setUncaughtExceptionHandler((t, throwable) ->{
                    throwable.printStackTrace();
                    System.exit(-1);
                });
                break;
            }
        }
    }

    public ServerLauncher(ApplicationListener listener) {
        super(listener);

        //don't do anything at all for GDX logging: don't want controller info and such
        Gdx.app.setApplicationLogger(new ApplicationLogger() {
            @Override public void log(String tag, String message) { }
            @Override public void log(String tag, String message, Throwable exception) { }
            @Override public void error(String tag, String message) { }
            @Override public void error(String tag, String message, Throwable exception) { }
            @Override public void debug(String tag, String message) { }
            @Override public void debug(String tag, String message, Throwable exception) { }
        });
    }
}