package io.anuke.mindustry.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import io.anuke.mindustry.editor.Editor;

public class EditorLauncher{
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry Editor");
		config.setMaximized(true);
		config.setWindowedMode(800, 600);
		config.setWindowIcon("sprites/icon.png");
		
		new Lwjgl3Application(new Editor(), config);
	}
}
