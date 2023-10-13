package mindustry.type;

import arc.struct.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;

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
            put(value.name, json.readValue(StringMap.class, value));
        }
    }
}
