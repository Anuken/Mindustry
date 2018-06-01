package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.core.Platform;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Log;

import java.util.Locale;

import static io.anuke.mindustry.Vars.headless;

public class BundleLoader {
    private static final boolean externalBundle = false;

    public static void load(){
        Settings.defaults("locale", "default");
        Settings.load("io.anuke.moment");
        loadBundle();
    }

    private static Locale getLocale(){
        String loc = Settings.getString("locale");
        if(loc.equals("default")){
            return Locale.getDefault();
        }else{
            Locale lastLocale;
            if (loc.contains("_")) {
                String[] split = loc.split("_");
                lastLocale = new Locale(split[0], split[1]);
            } else {
                lastLocale = new Locale(loc);
            }

            return lastLocale;
        }
    }

    private static void loadBundle(){
        I18NBundle.setExceptionOnMissingKey(false);

        if(externalBundle){
            try {
                FileHandle handle = Gdx.files.local("bundle");

                Locale locale = Locale.ENGLISH;
                Core.bundle = I18NBundle.createBundle(handle, locale);
            }catch (Exception e){
                Log.err(e);
                Platform.instance.showError("Failed to find bundle!\nMake sure you have bundle.properties in the same directory\nas the jar file.\n\nIf the problem persists, try running it through the command prompt:\n" +
                        "Hold left-shift, then right click and select 'open command prompt here'.\nThen, type in 'java -jar mindustry.jar' without quotes.");
                Gdx.app.exit();
            }
        }else{
            FileHandle handle = Gdx.files.internal("bundles/bundle");

            Locale locale = getLocale();
            Locale.setDefault(locale);
            if(!headless) Log.info("Got locale: {0}", locale);
            Core.bundle = I18NBundle.createBundle(handle, locale);
        }
    }
}
