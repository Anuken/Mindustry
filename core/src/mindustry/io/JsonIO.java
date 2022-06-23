package mindustry.io;

import arc.graphics.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

@SuppressWarnings("unchecked")
public class JsonIO{
    private static final CustomJson jsonBase = new CustomJson();

    public static final Json json = new Json(){
        { apply(this); }

        @Override
        public void writeValue(Object value, Class knownType, Class elementType){
            if(value instanceof MappableContent){
                try{
                    getWriter().value(((MappableContent)value).name);
                }catch(IOException e){
                    throw new RuntimeException(e);
                }
            }else{
                super.writeValue(value, knownType, elementType);
            }
        }

        @Override
        protected String convertToString(Object object){
            if(object instanceof MappableContent){
                return ((MappableContent)object).name;
            }
            return super.convertToString(object);
        }
    };

    public static String write(Object object){
        return json.toJson(object, object.getClass());
    }

    public static <T> T copy(T object, T dest){
        json.copyFields(object, dest);
        return dest;
    }

    public static <T> T copy(T object){
        return read((Class<T>)object.getClass(), write(object));
    }

    public static <T> T read(Class<T> type, String string){
        return json.fromJson(type, string.replace("io.anuke.", ""));
    }

    public static <T> T read(Class<T> type, T base, String string){
        return jsonBase.fromBaseJson(type, base, string.replace("io.anuke.", ""));
    }

    public static String print(String in){
        return json.prettyPrint(in);
    }

    static void apply(Json json){
        json.setElementType(Rules.class, "spawns", SpawnGroup.class);
        json.setElementType(Rules.class, "loadout", ItemStack.class);

        json.setSerializer(Color.class, new Serializer<>(){
            @Override
            public void write(Json json, Color object, Class knownType){
                json.writeValue(object.toString());
            }

            @Override
            public Color read(Json json, JsonValue jsonData, Class type){
                if(jsonData.isString()){
                    return Color.valueOf(jsonData.asString());
                }
                Color out = new Color();
                json.readFields(out, jsonData);
                return out;
            }
        });

        json.setSerializer(Sector.class, new Serializer<>(){
            @Override
            public void write(Json json, Sector object, Class knownType){
                json.writeValue(object.planet.name + "-" + object.id);
            }

            @Override
            public Sector read(Json json, JsonValue jsonData, Class type){
                String name = jsonData.asString();
                int idx = name.lastIndexOf('-');
                return Vars.content.<Planet>getByName(ContentType.planet, name.substring(0, idx)).sectors.get(Integer.parseInt(name.substring(idx + 1)));
            }
        });

        json.setSerializer(SectorPreset.class, new Serializer<>(){
            @Override
            public void write(Json json, SectorPreset object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public SectorPreset read(Json json, JsonValue jsonData, Class type){
                return Vars.content.getByName(ContentType.sector, jsonData.asString());
            }
        });

        json.setSerializer(Liquid.class, new Serializer<>(){
            @Override
            public void write(Json json, Liquid object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Liquid read(Json json, JsonValue jsonData, Class type){
                if(jsonData.asString() == null) return Liquids.water;
                Liquid i = Vars.content.getByName(ContentType.liquid, jsonData.asString());
                return i == null ? Liquids.water : i;
            }
        });

        json.setSerializer(Attribute.class, new Serializer<>(){
            @Override
            public void write(Json json, Attribute object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Attribute read(Json json, JsonValue jsonData, Class type){
                return Attribute.get(jsonData.asString());
            }
        });

        json.setSerializer(Item.class, new Serializer<>(){
            @Override
            public void write(Json json, Item object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Item read(Json json, JsonValue jsonData, Class type){
                if(jsonData.asString() == null) return Items.copper;
                Item i = Vars.content.getByName(ContentType.item, jsonData.asString());
                return i == null ? Items.copper : i;
            }
        });

        json.setSerializer(Team.class, new Serializer<>(){
            @Override
            public void write(Json json, Team object, Class knownType){
                json.writeValue(object.id);
            }

            @Override
            public Team read(Json json, JsonValue jsonData, Class type){
                return Team.get(jsonData.asInt());
            }
        });

        json.setSerializer(Block.class, new Serializer<>(){
            @Override
            public void write(Json json, Block object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Block read(Json json, JsonValue jsonData, Class type){
                Block block = Vars.content.getByName(ContentType.block, jsonData.asString());
                return block == null ? Blocks.air : block;
            }
        });

        json.setSerializer(Planet.class, new Serializer<>(){
            @Override
            public void write(Json json, Planet object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Planet read(Json json, JsonValue jsonData, Class type){
                Planet block = Vars.content.getByName(ContentType.planet, jsonData.asString());
                return block == null ? Planets.serpulo : block;
            }
        });

        json.setSerializer(Weather.class, new Serializer<>(){
            @Override
            public void write(Json json, Weather object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public Weather read(Json json, JsonValue jsonData, Class type){
                return Vars.content.getByName(ContentType.weather, jsonData.asString());
            }
        });

        json.setSerializer(UnitType.class, new Serializer<>(){
            @Override
            public void write(Json json, UnitType object, Class knownType){
                json.writeValue(object.name);
            }

            @Override
            public UnitType read(Json json, JsonValue jsonData, Class type){
                return Vars.content.getByName(ContentType.unit, jsonData.asString());
            }
        });

        json.setSerializer(ItemStack.class, new Serializer<>(){
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

        json.setSerializer(UnlockableContent.class, new Serializer<>(){
            @Override
            public void write(Json json, UnlockableContent object, Class knownType){
                json.writeValue(object == null ? null : object.name);
            }

            @Override
            public UnlockableContent read(Json json, JsonValue jsonData, Class type){
                if(jsonData.isNull()) return null;
                String str = jsonData.asString();
                Item item = Vars.content.item(str);
                Liquid liquid = Vars.content.liquid(str);
                Block block = Vars.content.block(str);
                UnitType unit = Vars.content.unit(str);
                return
                    item != null ? item :
                    liquid != null ? liquid :
                    block != null ? block :
                    unit;
            }
        });

        //use short names for all filter types
        for(var filter : Maps.allFilterTypes){
            var i = filter.get();
            json.addClassTag(Strings.camelize(i.getClass().getSimpleName().replace("Filter", "")), i.getClass());
        }

        //use short names for all objective types
        for(var obj : MapObjectives.allObjectiveTypes){
            var i = obj.get();
            json.addClassTag(Strings.camelize(i.getClass().getSimpleName().replace("Objective", "")), i.getClass());
        }

        //use short names for all marker types
        for(var obj : MapObjectives.allMarkerTypes){
            var i = obj.get();
            json.addClassTag(Strings.camelize(i.getClass().getSimpleName().replace("Marker", "")), i.getClass());
        }
    }

    static class CustomJson extends Json{
        private Object baseObject;

        { apply(this); }

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
