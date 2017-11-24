package io.anuke.mindustry.desktop;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.io.Formatter;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry");
		config.setMaximized(true);
		config.useVsync(false);
		config.setWindowedMode(800, 600);
		config.setWindowIcon("sprites/icon.png");
		
		Mindustry.formatter = new Formatter(){
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm");
			
			@Override
			public String format(Date date){
				return format.format(date);
			}

			@Override
			public String format(int number){
				return NumberFormat.getIntegerInstance().format(number);
			}
		};
		
		Mindustry.args = Array.with(arg);
		
		new Lwjgl3Application(new Mindustry(), config);
	}
}
