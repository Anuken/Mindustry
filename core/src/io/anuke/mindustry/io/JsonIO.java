package io.anuke.mindustry.io;

import io.anuke.arc.util.serialization.Json;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.game.SpawnGroup;

public class JsonIO{
    private static Json json = new Json(){{
        setIgnoreUnknownFields(true);
        setElementType(Rules.class, "spawns", SpawnGroup.class);
    }};

    public static String write(Object object){
        return json.toJson(object);
    }

    public static <T> T read(Class<T> type, String string){
        return json.fromJson(type, string);
    }

    public static String print(String in){
        return json.prettyPrint(in);
    }
}
