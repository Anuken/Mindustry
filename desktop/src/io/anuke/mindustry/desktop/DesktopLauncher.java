package io.anuke.mindustry.desktop;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.SaveIO.FormatProvider;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry");
		config.setMaximized(true);
		//config.useVsync(false);
		config.setWindowedMode(800, 600);
		
		SaveIO.setFormatProvider(new FormatProvider(){
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			
			public String format(Date date){
				return format.format(date);
			}
		});
		
		Mindustry.args = arg;
		
		new Lwjgl3Application(new Mindustry(), config);
	}
}
