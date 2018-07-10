package io.anuke.mindustry.desktop;

import com.apple.eawt.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3FileHandle;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.Saves.SaveSlot;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.io.BinaryPreferences;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.OS;
import io.anuke.ucore.util.Strings;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.anuke.mindustry.Vars.*;

public class DesktopLauncher extends Lwjgl3Application{
    ObjectMap<String, Preferences> prefmap;
	
	public static void main (String[] arg) {
        try {
            Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
            config.setTitle("Mindustry");
            config.setMaximized(true);
            config.setWindowedMode(960, 540);
            config.setWindowIcon("sprites/icon.png");

            if(OS.isMac) {
                Application.getApplication().setOpenFileHandler(e -> {
                    List list = e.getFiles();

                    File target = (File)list.get(0);

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
            }

            Platform.instance = new DesktopPlatform(arg);

            Net.setClientProvider(new KryoClient());
            Net.setServerProvider(new KryoServer());
			new DesktopLauncher(new Mindustry(), config);
		}catch (Throwable e){
		    CrashHandler.handle(e);
		}
	}

    public DesktopLauncher(ApplicationListener listener, Lwjgl3ApplicationConfiguration config) {
        super(listener, config);
    }

    @Override
    public Preferences getPreferences(String name) {
	    String prefsDirectory = OS.getAppDataDirectoryString("Mindustry");

	    if(prefmap == null){
	        prefmap = new ObjectMap<>();
        }

	    if(prefmap.containsKey(name)){
	        return prefmap.get(name);
        }else{
            Preferences prefs = new BinaryPreferences(new Lwjgl3FileHandle(new File(prefsDirectory, name), FileType.Absolute));
            prefmap.put(name, prefs);
            return prefs;
        }
    }
}
