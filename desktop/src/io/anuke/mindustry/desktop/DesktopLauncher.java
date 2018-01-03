package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Array;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.util.Strings;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
			public void addDialog(TextField field){}

			@Override
			public void openDonations(){}

			@Override
			public void onSceneChange(String state, String details, String icon) {
				DiscordRPC lib = DiscordRPC.INSTANCE;

				if(lib == null) return;

				String applicationId = "397335883319083018";

				DiscordEventHandlers handlers = new DiscordEventHandlers();

				lib.Discord_Initialize(applicationId, handlers, true, "");

				DiscordRichPresence presence = new DiscordRichPresence();
				presence.startTimestamp = System.currentTimeMillis() / 1000; // epoch second
				presence.state = state;
				presence.details = details;
				presence.largeImageKey = "logo";
				presence.largeImageText = details;
				lib.Discord_UpdatePresence(presence);
      		}

      		@Override
      		public void onGameExit() {
        DiscordRPC.INSTANCE.Discord_Shutdown();
      }
		};
		
		Mindustry.args = Array.with(arg);

		Net.setClientProvider(new KryoClient());
		Net.setServerProvider(new KryoServer());

		try {
			new Lwjgl3Application(new Mindustry(), config);
		}catch (Exception e){
			e.printStackTrace();

			//don't create crash logs for me, as it's expected
			if(System.getProperty("user.name").equals("anuke")) return;

		    String result = Strings.parseException(e, true);
		    boolean failed = false;

		    String filename = "crash-report-" + DateFormat.getDateTimeInstance().format(new Date()) + ".txt";

		    try{
                Files.write(Paths.get(filename), result.getBytes());
            }catch (IOException i){
                i.printStackTrace();
                failed = true;
            }

			JOptionPane.showMessageDialog(null, "An error has occured: \n" + result + "\n\n" +
                    (!failed ? "A crash report has been written to " + new File(filename).getAbsolutePath() + ".\nPlease send this file to the developer!"
                            : "Failed to generate crash report.\nPlease send an image of this crash log to the developer!"));
		}
	}
}
