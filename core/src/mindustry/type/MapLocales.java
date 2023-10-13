package mindustry.type;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;

import static arc.Core.*;

/** Class for storing map-specific locale bundles */
public class MapLocales extends ObjectMap<String, StringMap> implements JsonSerializable{
    private static TextFormatter formatter = new TextFormatter(null, false);
    public static String currentLocale = settings.getString("locale");

    @Override
    public void write(Json json){
        for(var entry : entries()){
            json.writeValue(entry.key, entry.value, StringMap.class, String.class);
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        for(JsonValue value : jsonData){
            put(value.name, json.readValue(StringMap.class, value));
        }
    }

    public String getProperty(String key){
        if(!containsProperty(key)){
            if(containsProperty("en", key)){
                return get("en").get(key);
            }
            return "???" + key + "???";
        }
        return get(currentLocale).get(key);
    }

    private String getProperty(String locale, String key){
        if(!containsProperty(locale, key)) return "???" + key + "???";
        return get(locale).get(key);
    }

    public boolean containsProperty(String key){
        return containsProperty(currentLocale, key);
    }

    private boolean containsProperty(String locale, String key){
        if(!containsKey(locale)) return false;
        return get(locale).containsKey(key);
    }

    public String getFormatted(String key, Object... args){
        if(!containsProperty(key)){
            if(containsProperty("en", key)){
                return formatter.format(getProperty("en", key), args);
            }
            return "???" + key + "???";
        }
        return formatter.format(getProperty(key), args);
    }
}
