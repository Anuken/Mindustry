package io.anuke.mindustry.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.Net;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry");
		config.setMaximized(true);
		config.setWindowedMode(960, 540);
		config.setWindowIcon("sprites/icon.png");

        Platform.instance = new DesktopPlatform(arg);

		Net.setClientProvider(new KryoClient());
		Net.setServerProvider(new KryoServer());

		try {
			new Lwjgl3Application(new Mindustry(), config);
		}catch (Throwable e){
		    CrashHandler.handle(e);
		}
	}
}
