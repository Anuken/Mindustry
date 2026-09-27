package mindustry.game.markers;

import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class TextureHolder implements JsonSerializable{
    public Object value = "white";

    @Override
    public void write(Json json, JsonWriter writer){
        if(value instanceof String s){
            json.writeValue(writer, "string", s);
        }else if(value instanceof UnlockableContent c){
            json.writeValue(writer, "content", c.name);
        }else if(value instanceof Building b){
            json.writeValue(writer, "building", b.pos());
        }
    }

    @Override
    public void read(Json json, Jval jsonData){
        if(jsonData.has("string")){
            value = jsonData.get("string").asString();
        }else if(jsonData.has("content")){
            value = content.byName(jsonData.get("content").asString());
        }else if(jsonData.has("building")){
            value = world.build(jsonData.get("building").asInt());
        }else{
            value = "white";
        }
    }
}
