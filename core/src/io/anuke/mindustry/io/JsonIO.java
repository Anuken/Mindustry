package io.anuke.mindustry.io;

import io.anuke.arc.util.serialization.*;
import io.anuke.arc.util.serialization.Json.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

@SuppressWarnings("unchecked")
public class JsonIO{
    private static CustomJson jsonBase = new CustomJson();
    private static Json json = new Json(){{
        apply(this);
    }};

    public static String write(Object object){
        return json.toJson(object, object.getClass());
    }

    public static <T> T copy(T object){
        return read((Class<T>)object.getClass(), write(object));
    }

    public static <T> T read(Class<T> type, String string){
        return json.fromJson(type, string);
    }

    public static <T> T read(Class<T> type, T base, String string){
        return jsonBase.fromBaseJson(type, base, string);
    }

    public static String print(String in){
        return json.prettyPrint(in);
    }

    private static void apply(Json json){
        json.setIgnoreUnknownFields(true);
        json.setElementType(Rules.class, "spawns", SpawnGroup.class);
        json.setElementType(Rules.class, "loadout", ItemStack.class);

        json.setSerializer(Zone.class, new Serializer<Zone>(){
            @Override
            public void write(Json json, Zone object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Zone read(Json json, JsonValue jsonData, Class type){
                return Vars.content.getByName(ContentType.zone, jsonData.asString());
            }
        });

        json.setSerializer(Item.class, new Serializer<Item>(){
            @Override
            public void write(Json json, Item object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Item read(Json json, JsonValue jsonData, Class type){
                if(jsonData.asString() == null) return Items.copper;
                Item i =  Vars.content.getByName(ContentType.item, jsonData.asString());
                return i == null ? Items.copper : i;
            }
        });

        //TODO extremely hacky and disgusting
        for(Block block : Vars.content.blocks()){
            Class type = block.getClass();
            if(type.isAnonymousClass()) type = type.getSuperclass();

            json.setSerializer(type, new Serializer<Block>(){
                @Override
                public void write(Json json, Block object, Class knownType){
                    json.writeValue(object.name);
                }

                @Override
                public Block read(Json json, JsonValue jsonData, Class type){
                    return Vars.content.getByName(ContentType.block, jsonData.asString());
                }
            });
        }

        json.setSerializer(Block.class, new Serializer<Block>(){
            @Override
            public void write(Json json, Block object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Block read(Json json, JsonValue jsonData, Class type){
                return Vars.content.getByName(ContentType.block, jsonData.asString());
            }
        });

        /*
        json.setSerializer(TeamData.class, new Serializer<TeamData>(){
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
        });*/

        json.setSerializer(ItemStack.class, new Serializer<ItemStack>(){
            @Override
            public void write(Json json, ItemStack object, Class knownType){
                json.writeObjectStart();
                json.writeValue("item", object.item);
                json.writeValue("amount", object.amount);
                json.writeObjectEnd();
            }

            @Override
            public ItemStack read(Json json, JsonValue jsonData, Class type){
                return new ItemStack(json.getSerializer(Item.class).read(json, jsonData.get("item"), Item.class), jsonData.getInt("amount"));
            }
        });
    }

    static class CustomJson extends Json{
        private Object baseObject;

        {
            apply(this);
        }

        @Override
        public <T> T fromJson(Class<T> type, String json){
            return fromBaseJson(type, null, json);
        }

        public <T> T fromBaseJson(Class<T> type, T base, String json){
            this.baseObject = base;
            return readValue(type, null, new JsonReader().parse(json));
        }

        @Override
        protected Object newInstance(Class type){
            if(baseObject == null || baseObject.getClass() != type){
                return super.newInstance(type);
            }
            return baseObject;
        }
    }
}
