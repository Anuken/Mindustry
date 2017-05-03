package io.anuke.moment.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import io.anuke.mindustry.Moment;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Moment");
		config.setMaximized(true);
		config.useVsync(false);
		config.setWindowedMode(800, 600);
		new Lwjgl3Application(new Moment(), config);
	}
}
