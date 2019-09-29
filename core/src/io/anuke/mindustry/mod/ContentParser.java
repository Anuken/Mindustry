package io.anuke.mindustry.mod;

import io.anuke.arc.collection.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;

public class ContentParser{
    private Json parser = new Json();
    private ObjectMap<ContentType, TypeParser<?>> parsers = ObjectMap.of(

    );

    /**
     * Parses content from a json file.
     * @param name the name of the file without its extension
     * @param json the json to parse
     * @param type the type of content this is
     * @return the content that was parsed
     */
    public Content parse(String name, String json, ContentType type) throws Exception{
        JsonValue value = parser.fromJson(null, json);
        if(!parsers.containsKey(type)){
            throw new SerializationException("No parsers for content type '" + type + "'");
        }

        return parsers.get(type).parse(name, value);
    }
}
