package mindustry.io;

import arc.graphics.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

@SuppressWarnings("unchecked")
public class JsonIO{
    public static final Json json = new Json(){
        @Override
        public void writeValue(JsonWriter writer, Object value, Class knownType, Class elementType){
            if(value instanceof MappableContent c){
                writer.value(c.name);
            }else{
                super.writeValue(writer, value, knownType, elementType);
            }
        }

        @Override
        protected String convertToString(Object object){
            if(object instanceof MappableContent c) return c.name;
            return super.convertToString(object);
        }
    };

    public static void writeBytes(Object value, Class<?> elementType, DataOutputStream output){
        json.toUBJson(value, value == null ? null : value.getClass(), elementType, output);
    }

    public static <T> T readBytes(Class<T> type, Class<?> elementType, DataInputStream input) throws IOException{
        return json.readValue(type, elementType, UBJson.read(input));
    }

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

    public static <T> T read(T base, String string){
        json.readFields(base, Jval.read(string.replace("io.anuke.", "")));
        return base;
    }

    public static String print(String in){
        return Jval.read(in).toString(Jformat.hjson);
    }

    public static void classTag(String tag, Class<?> type){
        json.addClassTag(tag, type);
    }

    static{
        json.setElementType(Rules.class, "spawns", SpawnGroup.class);
        json.setElementType(Rules.class, "loadout", ItemStack.class);

        json.setSerializer(MusicContainer.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, MusicContainer object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public MusicContainer read(Json json, Jval jsonData, Class type){
                return new MusicContainer(jsonData.isString() ? jsonData.asString() : "");
            }
        });

        json.setSerializer(Color.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Color object, Class knownType){
                json.writeValue(writer, object.toString());
            }

            @Override
            public Color read(Json json, Jval jsonData, Class type){
                if(jsonData.isString()){
                    return Color.valueOf(jsonData.asString());
                }
                Color out = new Color();
                json.readFields(out, jsonData);
                return out;
            }
        });

        json.setSerializer(Sector.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Sector object, Class knownType){
                json.writeValue(writer, object.planet.name + "-" + object.id);
            }

            @Override
            public Sector read(Json json, Jval jsonData, Class type){
                String name = jsonData.asString();
                int idx = name.lastIndexOf('-');
                return Vars.content.<Planet>getByName(ContentType.planet, name.substring(0, idx)).sectors.get(Integer.parseInt(name.substring(idx + 1)));
            }
        });

        json.setSerializer(SectorPreset.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, SectorPreset object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public SectorPreset read(Json json, Jval jsonData, Class type){
                return Vars.content.getByName(ContentType.sector, jsonData.asString());
            }
        });

        json.setSerializer(Liquid.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Liquid object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public Liquid read(Json json, Jval jsonData, Class type){
                if(jsonData.asString() == null) return Liquids.water;
                Liquid i = Vars.content.getByName(ContentType.liquid, jsonData.asString());
                return i == null ? Liquids.water : i;
            }
        });

        json.setSerializer(Attribute.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Attribute object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public Attribute read(Json json, Jval jsonData, Class type){
                return Attribute.get(jsonData.asString());
            }
        });

        json.setSerializer(Item.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Item object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public Item read(Json json, Jval jsonData, Class type){
                if(jsonData.asString() == null) return Items.copper;
                Item i = Vars.content.getByName(ContentType.item, jsonData.asString());
                return i == null ? Items.copper : i;
            }
        });

        json.setSerializer(Team.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Team object, Class knownType){
                json.writeValue(writer, object.id);
            }

            @Override
            public Team read(Json json, Jval jsonData, Class type){
                return Team.get(jsonData.asInt());
            }
        });

        json.setSerializer(Block.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Block object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public Block read(Json json, Jval jsonData, Class type){
                Block block = Vars.content.getByName(ContentType.block, jsonData.asString());
                if(block == null) block = Vars.content.getByName(ContentType.block, SaveVersion.fallback.get(jsonData.asString(), ""));
                return block == null ? Blocks.air : block;
            }
        });

        json.setSerializer(Planet.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Planet object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public Planet read(Json json, Jval jsonData, Class type){
                if(jsonData.asString() == null){
                    return null;
                }
                Planet block = Vars.content.getByName(ContentType.planet, jsonData.asString());
                return block == null ? Planets.serpulo : block;
            }
        });

        json.setSerializer(Weather.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, Weather object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public Weather read(Json json, Jval jsonData, Class type){
                return Vars.content.getByName(ContentType.weather, jsonData.asString());
            }
        });

        json.setSerializer(UnitType.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, UnitType object, Class knownType){
                json.writeValue(writer, object.name);
            }

            @Override
            public UnitType read(Json json, Jval jsonData, Class type){
                if(jsonData.asString() == null) return UnitTypes.dagger;
                UnitType u = Vars.content.getByName(ContentType.unit, jsonData.asString());
                return u == null ? UnitTypes.dagger : u;
            }
        });

        json.setSerializer(ItemStack.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, ItemStack object, Class knownType){
                writer.writeObjectStart();
                json.writeValue(writer, "item", object.item);
                json.writeValue(writer, "amount", object.amount);
                writer.writeObjectEnd();
            }

            @Override
            public ItemStack read(Json json, Jval jsonData, Class type){
                return new ItemStack(json.getSerializer(Item.class).read(json, jsonData.get("item"), Item.class), jsonData.getInt("amount"));
            }
        });

        json.setSerializer(UnlockableContent.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, UnlockableContent object, Class knownType){
                json.writeValue(writer, object == null ? null : object.name);
            }

            @Override
            public UnlockableContent read(Json json, Jval jsonData, Class type){
                if(jsonData.isNull()) return null;
                String str = jsonData.asString();
                var map = Vars.content.byName(str);
                return map instanceof UnlockableContent u ? u : null;
            }
        });

        json.setSerializer(MapObjectives.class, new JsonSerializer<>(){
            @Override
            public void write(Json json, JsonWriter writer, MapObjectives exec, Class knownType){
                writer.writeArrayStart();
                for(var obj : exec){
                    json.writeObjectStart(writer, obj.getClass().isAnonymousClass() ? obj.getClass().getSuperclass() : obj.getClass(), null);
                    json.writeFields(writer, obj);

                    writer.writeArrayStart("parents");
                    for(var parent : obj.parents){
                        json.writeValue(writer, exec.all.indexOf(parent));
                    }

                    writer.writeArrayEnd();

                    json.writeValue(writer, "editorPos", Point2.pack(obj.editorX, obj.editorY));
                    json.writeObjectEnd(writer);
                }

                writer.writeArrayEnd();
            }

            @Override
            public MapObjectives read(Json json, Jval data, Class type){
                var exec = new MapObjectives();
                // First iteration to instantiate the objectives.
                for(var value : data.asArray()){
                    //glenn why did you implement this in the least backwards compatible way possible
                    //the old objectives had lowercase class tags, now they're uppercase and either way I can't deserialize them without errors
                    if(value.has("class") && Character.isLowerCase(value.getString("class").charAt(0))){
                        return new MapObjectives();
                    }

                    MapObjective obj = json.readValue(MapObjective.class, value);

                    if(value.has("editorPos")){
                        int pos = value.getInt("editorPos");
                        obj.editorX = Point2.x(pos);
                        obj.editorY = Point2.y(pos);
                    }

                    exec.all.add(obj);
                    obj.validate();
                }

                // Second iteration to map the parents.
                int i = 0;
                for(var entry : data.asArray()){
                    for(var parent : entry.get("parents").asArray()){
                        int val = parent.asInt();
                        if(val >= 0 && val < exec.all.size){
                            exec.all.get(i).parents.add(exec.all.get(val));
                        }
                    }
                }

                return exec;
            }
        });

        //use short names for all filter types
        for(var filter : Maps.allFilterTypes){
            var i = filter.get();
            json.addClassTag(Strings.camelize(i.getClass().getSimpleName().replace("Filter", "")), i.getClass());
        }
    }
}