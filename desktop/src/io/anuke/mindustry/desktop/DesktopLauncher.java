package io.anuke.mindustry.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.io.BinaryPreferences;
import io.anuke.ucore.util.OS;

import java.io.File;

public class DesktopLauncher extends Lwjgl3Application{
    ObjectMap<String, Preferences> prefmap;

    public DesktopLauncher(ApplicationListener listener, Lwjgl3ApplicationConfiguration config){
        super(listener, config);
    }

    public static void main(String[] arg){
        try{
            Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
            config.setTitle("Mindustry");
            config.setMaximized(true);
            config.setWindowedMode(960, 540);
            config.setWindowIcon("sprites/icon.png");

            Platform.instance = new DesktopPlatform(arg);

            Net.setClientProvider(new KryoClient());
            Net.setServerProvider(new KryoServer());
            new DesktopLauncher(new Mindustry(), config);
        }catch(Throwable e){
            CrashHandler.handle(e);
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
            Preferences prefs = new BinaryPreferences(new Lwjgl3FileHandle(new File(prefsDirectory, name), FileType.Absolute));
            prefmap.put(name, prefs);
            return prefs;
        }
    }
}
