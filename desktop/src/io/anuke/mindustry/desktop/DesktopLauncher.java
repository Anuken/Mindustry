package io.anuke.mindustry.desktop;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Settings;
import io.anuke.arc.backends.lwjgl3.Lwjgl3Application;
import io.anuke.arc.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.OS;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.ArcNetClient;
import io.anuke.mindustry.net.ArcNetServer;

public class DesktopLauncher extends Lwjgl3Application{

    public DesktopLauncher(ApplicationListener listener, Lwjgl3ApplicationConfiguration config){
        super(listener, config);
    }

    public static void main(String[] arg){
        try{
            Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
            config.setTitle("Mindustry");
            config.setMaximized(true);
            config.setBackBufferConfig(8, 8, 8, 8, 0, 0, 0);
            config.setWindowedMode(960, 540);
            config.setWindowIcon("sprites/icon.png");

            try{
                Settings settings = new Settings(){
                    @Override
                    public FileHandle getDataDirectory(){
                        return dataDirectory == null ? new FileHandle(OS.getAppDataDirectoryString(appName)) : dataDirectory;
                    }
                };
                settings.setAppName(Vars.appName);
                settings.loadValues();
                int level = settings.getInt("antialias", 0);
                config.setBackBufferConfig(8, 8, 8, 8, 0, 0, level == 0 ? 0 : 1 << level);
            }catch(Throwable t){
                t.printStackTrace();
            }

            Platform.instance = new DesktopPlatform(arg);

            Net.setClientProvider(new ArcNetClient());
            Net.setServerProvider(new ArcNetServer());
            new DesktopLauncher(new Mindustry(), config);
        }catch(Throwable e){
            DesktopPlatform.handleCrash(e);
        }
    }
}
