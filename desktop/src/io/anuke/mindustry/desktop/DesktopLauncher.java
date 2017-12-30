package io.anuke.mindustry.desktop;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Array;

import com.esotericsoftware.kryonet.Client;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Net;
import io.anuke.mindustry.Net.ClientProvider;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.ucore.scene.ui.TextField;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry");
		config.setMaximized(true);
		config.setWindowedMode(960, 540);
		config.setWindowIcon("sprites/icon.png");

		Mindustry.platforms = new PlatformFunction(){
			DateFormat format = SimpleDateFormat.getDateTimeInstance();
			
			@Override
			public String format(Date date){
				return format.format(date);
			}

			@Override
			public String format(int number){
				return NumberFormat.getIntegerInstance().format(number);
			}
			
			@Override
			public void openLink(String link){
				try{
					Desktop.getDesktop().browse(URI.create(link));
				}catch(IOException e){
					e.printStackTrace();
					Vars.ui.showError("Error opening link.");
				}
			}

			@Override
			public void addDialog(TextField field){
				
			}
		};
		
		Mindustry.args = Array.with(arg);

		Net.setClientProvider(new ClientProvider() {
			Client client;

			{
				client = new Client();
			}

			@Override
			public void connect(String ip, String port) throws IOException {

			}

			@Override
			public void send(Object object) {

			}

			@Override
			public void register(Class<?>... types) {
				for(Class<?> c : types){
					client.getKryo().register(c);
				}
			}
		});
		
		new Lwjgl3Application(new Mindustry(), config);
	}
}
