package mindustry.type;

import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;

import java.util.*;

import static arc.Core.*;

/** Class for storing map-specific locale bundles */
public class MapLocales extends ObjectMap<String, StringMap> implements JsonSerializable{

    @Override
    public void write(Json json){
        for(var entry : entries()){
            json.writeValue(entry.key, entry.value, StringMap.class, String.class);
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        for(JsonValue value : jsonData){
            StringMap map = new StringMap();

            for(JsonValue child = value.child; child != null; child = child.next){
                map.put(child.name, json.readValue(String.class, null, child));
            }

            put(value.name, map);
        }
    }

    @Override
    public MapLocales copy(){
        MapLocales out = new MapLocales();

        for(var entry : this.entries()){
            StringMap map = new StringMap();
            map.putAll(entry.value);
            out.put(entry.key, map);
        }

        return out;
    }

    public String getProperty(String key){
        if(!containsProperty(currentLocale(), key)){
            if(containsProperty("en", key)) return get("en").get(key);
            return "???" + key + "???";
        }
        return get(currentLocale()).get(key);
    }

    private String getProperty(String locale, String key){
        if(!containsProperty(locale, key)){
            if(containsProperty("en", key)) return get("en").get(key);
            return "???" + key + "???";
        }
        return get(locale).get(key);
    }

    public boolean containsProperty(String key){
        return containsProperty(currentLocale(), key) || containsProperty("en", key);
    }

    private boolean containsProperty(String locale, String key){
        if(!containsKey(locale)) return false;
        return get(locale).containsKey(key);
    }

    public String getFormatted(String key, Object... args){
        StringBuilder result = new StringBuilder();
        if(!containsProperty(currentLocale(), key)){
            if(containsProperty("en", key)){
                result.append(getProperty("en", key));
            }else{
                return "???" + key + "???";
            }
        }else{
            result.append(getProperty(currentLocale(), key));
        }

        for(var arg : args){
            int placeholderIndex = result.indexOf("@");

            if(placeholderIndex == -1) break;

            result.replace(placeholderIndex, placeholderIndex + 1, arg.toString());
        }

        return result.toString();
    }

    // To handle default locale properly
    public static String currentLocale(){
        String locale = settings.getString("locale");
        if(locale.equals("default")){
            locale = Locale.getDefault().getLanguage();
        }
        return locale;
    }
}
