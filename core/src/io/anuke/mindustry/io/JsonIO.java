package io.anuke.mindustry.io;

import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.collection.LongQueue;
import io.anuke.arc.util.serialization.Json;
import io.anuke.arc.util.serialization.JsonValue;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Teams.TeamData;
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

        setSerializer(TeamData.class, new Serializer<TeamData>(){
            @Override
            public void write(Json json, TeamData object, Class knownType){
                json.writeObjectStart();
                json.writeValue("brokenBlocks", object.brokenBlocks.toArray());
                json.writeValue("team", object.team.ordinal());
                json.writeObjectEnd();
            }

            @Override
            public TeamData read(Json json, JsonValue jsonData, Class type){
                long[] blocks = jsonData.get("brokenBlocks").asLongArray();
                Team team = Team.all[jsonData.getInt("team", 0)];
                TeamData out = new TeamData(team, EnumSet.of(new Team[]{}));
                out.brokenBlocks = new LongQueue(blocks);
                return out;
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
