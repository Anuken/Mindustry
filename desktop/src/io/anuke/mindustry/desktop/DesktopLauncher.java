package io.anuke.mindustry.desktop;

import com.apple.eawt.Application;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.Saves.SaveSlot;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.OS;
import io.anuke.ucore.util.Strings;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.anuke.mindustry.Vars.*;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Mindustry");
		config.setMaximized(true);
		config.setWindowedMode(960, 540);
		config.setWindowIcon("sprites/icon.png");

		if(OS.isMac) {
            Application.getApplication().setOpenFileHandler(e -> {
                List<File> list = e.getFiles();

                File target = list.get(0);

                Gdx.app.postRunnable(() -> {
                    FileHandle file = OS.getAppDataDirectory("Mindustry").child("tmp").child(target.getName());

                    Gdx.files.absolute(target.getAbsolutePath()).copyTo(file);

                    if(file.extension().equalsIgnoreCase(saveExtension)){ //open save

                        if(SaveIO.isSaveValid(file)){
                            try{
                                SaveSlot slot = control.getSaves().importSave(file);
                                ui.load.runLoadSave(slot);
                            }catch (IOException e2){
                                ui.showError(Bundles.format("text.save.import.fail", Strings.parseException(e2, false)));
                            }
                        }else{
                            ui.showError("$text.save.import.invalid");
                        }

                    }else if(file.extension().equalsIgnoreCase(mapExtension)){ //open map
                        Gdx.app.postRunnable(() -> {
                            if (!ui.editor.isShown()) {
                                ui.editor.show();
                            }

                            ui.editor.beginEditMap(file.read());
                        });
                    }
                });
            });

            config.setPreferencesConfig(OS.getAppDataDirectoryString("Mindustry"), FileType.Absolute);
        }

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
