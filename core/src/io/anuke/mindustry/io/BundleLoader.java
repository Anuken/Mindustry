package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Log;

import java.util.Locale;

import static io.anuke.mindustry.Vars.headless;

public class BundleLoader{

    public static void load(){
        Settings.defaults("locale", "default");
        Settings.load(Vars.appName, headless ? "io.anuke.mindustry.server" : "io.anuke.mindustry");
        loadGraphicSetting();
        loadBundle();
    }

    private static void loadGraphicSetting(){
        Unit.dp.product = Settings.prefs().getInteger("UIScale") / 10.0f;
        Vars.fontScale = Math.max(Unit.dp.scl(1f) / 2f, 0.5f) * Settings.prefs().getInteger("fontScale") / 10.0f;
        Vars.baseCameraScale = Math.round(Unit.dp.scl(Settings.prefs().getInteger("baseCameraScale")));
    }

    private static Locale getLocale(){
        String loc = Settings.getString("locale");
        if(loc.equals("default")){
            return Locale.getDefault();
        }else{
            Locale lastLocale;
            if(loc.contains("_")){
                String[] split = loc.split("_");
                lastLocale = new Locale(split[0], split[1]);
            }else{
                lastLocale = new Locale(loc);
            }

            return lastLocale;
        }
    }

    private static void loadBundle(){
        I18NBundle.setExceptionOnMissingKey(false);
        try{
            //try loading external bundle
            FileHandle handle = Gdx.files.local("bundle");

            Locale locale = Locale.ENGLISH;
            Core.bundle = I18NBundle.createBundle(handle, locale);

            Log.info("NOTE: external translation bundle has been loaded.");
            if(!headless){
                Timers.run(10f, () -> Vars.ui.showInfo("Note: You have successfully loaded an external translation bundle."));
            }
        }catch(Throwable e){
            //no external bundle found

            FileHandle handle = Gdx.files.internal("bundles/bundle");

            Locale locale = getLocale();
            Locale.setDefault(locale);
            if(!headless) Log.info("Got locale: {0}", locale);
            Core.bundle = I18NBundle.createBundle(handle, locale);
        }

    }
}
