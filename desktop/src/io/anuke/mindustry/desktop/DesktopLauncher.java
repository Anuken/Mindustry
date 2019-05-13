package io.anuke.mindustry.desktop;

import io.anuke.arc.backends.lwjgl3.Lwjgl3Application;
import io.anuke.arc.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.*;

public class DesktopLauncher{

    public static void main(String[] arg){
        try{
            Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
            config.setTitle("Mindustry");
            config.setMaximized(true);
            config.setBackBufferConfig(8, 8, 8, 8, 0, 0, 0);
            config.setWindowedMode(960, 540);
            config.setWindowIcon("sprites/icon.png");

            Platform.instance = new DesktopPlatform(arg);

            Net.setClientProvider(new ArcNetClient());
            Net.setServerProvider(new ArcNetServer());
            new Lwjgl3Application(new Mindustry(), config);
        }catch(Throwable e){
            DesktopPlatform.handleCrash(e);
        }
    }
}
