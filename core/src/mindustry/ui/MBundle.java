package mindustry.ui;

import arc.struct.*;
import arc.util.*;
import mindustry.core.*;

import java.util.*;

public class MBundle extends I18NBundle{
    private static final Locale ROOT_LOCALE = new Locale("", "", "");

    public static MBundle createBundle(I18NBundle parent){
        MBundle bundle = new MBundle();
        Reflect.set(I18NBundle.class, bundle, "locale", ROOT_LOCALE);
        ObjectMap<String, String> properties = new ObjectMap<>();
        Reflect.set(I18NBundle.class, bundle, "properties", properties);
        Reflect.set(I18NBundle.class, bundle, "parent", parent);
        return bundle;
    }

    @Override
    public String get(String key, String def){
        return UI.formatIcons(super.get(key, def));
    }

    @Override
    public String format(String key, Object... args){
        return UI.formatIcons(super.format(key, args));
    }

    @Override
    public String formatString(String string, Object... args){
        return UI.formatIcons(super.formatString(string, args));
    }

    @Override
    public String formatFloat(String key, float value, int places){
        return UI.formatIcons(super.formatFloat(key, value, places));
    }
}
