package io.anuke.mindustry.io;

import io.anuke.arc.Core;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.input.Binding;

import java.util.Locale;

import static io.anuke.mindustry.Vars.headless;

public class BundleLoader{

    public static void load(){
        Core.settings.defaults("locale", "default");
        Core.keybinds.setDefaults(Binding.values());
        Core.settings.load();
        loadBundle();
    }

    private static Locale getLocale(){
        String loc = Core.settings.getString("locale");
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
        if(headless) return;

        try{
            //try loading external bundle
            FileHandle handle = Core.files.local("bundle");

            Locale locale = Locale.ENGLISH;
            Core.bundle = I18NBundle.createBundle(handle, locale);

            Log.info("NOTE: external translation bundle has been loaded.");
            if(!headless){
                Time.run(10f, () -> Vars.ui.showInfo("Note: You have successfully loaded an external translation bundle."));
            }
        }catch(Throwable e){
            //no external bundle found

            FileHandle handle = Core.files.internal("bundles/bundle");

            Locale locale = getLocale();
            Locale.setDefault(locale);
            Core.bundle = I18NBundle.createBundle(handle, locale);
        }
    }
}
