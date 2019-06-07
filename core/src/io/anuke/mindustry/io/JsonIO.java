package io.anuke.mindustry.io;

import io.anuke.arc.util.serialization.Json;
import io.anuke.arc.util.serialization.JsonValue;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.type.*;

@SuppressWarnings("unchecked")
public class JsonIO{
    private static Json json = new Json(){{
        setIgnoreUnknownFields(true);
        setElementType(Rules.class, "spawns", SpawnGroup.class);

        setSerializer(Zone.class, new Serializer<Zone>(){
            @Override
            public void write(Json json, Zone object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Zone read(Json json, JsonValue jsonData, Class type){
                return Vars.content.getByName(ContentType.zone, jsonData.asString());
            }
        });

        setSerializer(Item.class, new Serializer<Item>(){
            @Override
            public void write(Json json, Item object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Item read(Json json, JsonValue jsonData, Class type){
                return Vars.content.getByName(ContentType.item, jsonData.asString());
            }
        });
    }};

    public static String write(Object object){
        return json.toJson(object);
    }

    public static <T> T copy(T object){
        return read((Class<T>)object.getClass(), write(object));
    }

    public static <T> T read(Class<T> type, String string){
        return json.fromJson(type, string);
    }

    public static String print(String in){
        return json.prettyPrint(in);
    }
}
