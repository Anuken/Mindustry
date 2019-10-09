package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.reflect.Field;
import io.anuke.arc.util.reflect.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.arc.util.serialization.Json.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.Effects.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Objectives.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.mod.Mods.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.consumers.*;

import java.lang.reflect.*;

@SuppressWarnings("unchecked")
public class ContentParser{
    private static final boolean ignoreUnknownFields = true;
    private ObjectMap<Class<?>, ContentType> contentTypes = new ObjectMap<>();
    private ObjectMap<Class<?>, FieldParser> classParsers = new ObjectMap<Class<?>, FieldParser>(){{
        put(BulletType.class, (type, data) -> field(Bullets.class, data));
        put(Effect.class, (type, data) -> field(Fx.class, data));
        put(StatusEffect.class, (type, data) -> field(StatusEffects.class, data));
        put(Loadout.class, (type, data) -> field(Loadouts.class, data));
        put(Color.class, (type, data) -> Color.valueOf(data.asString()));
        put(BulletType.class, (type, data) -> {
            Class<? extends BulletType> bc = data.has("type") ? resolve(data.getString("type"), "io.anuke.mindustry.entities.bullets") : BasicBulletType.class;
            data.remove("type");
            BulletType result = make(bc);
            readFields(result, data);
            return result;
        });
        put(Music.class, (type, data) -> {
            if(fieldOpt(Musics.class, data) != null) return fieldOpt(Musics.class, data);

            String path = "music/" + data.asString() + (Vars.ios ? ".mp3" : ".ogg");
            Core.assets.load(path, Music.class);
            Core.assets.finishLoadingAsset(path);
            return Core.assets.get(path);
        });
        put(Sound.class, (type, data) -> {
            if(fieldOpt(Sounds.class, data) != null) return fieldOpt(Sounds.class, data);

            String path = "sounds/" + data.asString() + (Vars.ios ? ".mp3" : ".ogg");
            Core.assets.load(path, Sound.class);
            Core.assets.finishLoadingAsset(path);
            Log.info(Core.assets.get(path));
            return Core.assets.get(path);
        });
        put(Objective.class, (type, data) -> {
            Class<? extends Objective> oc = data.has("type") ? resolve(data.getString("type"), "io.anuke.mindustry.game.Objectives") : ZoneWave.class;
            data.remove("type");
            Objective obj = make(oc);
            readFields(obj, data);
            return obj;
        });
    }};
    /** Stores things that need to be parsed fully, e.g. reading fields of content.
     * This is done to accomodate binding of content names first.*/
    private Array<Runnable> reads = new Array<>();
    private LoadedMod currentMod;
    private Content currentContent;

    private Json parser = new Json(){
        @Override
        public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData, Class keyType){
            if(type != null){
                if(classParsers.containsKey(type)){
                    try{
                        return (T)classParsers.get(type).parse(type, jsonData);
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                }

                if(Content.class.isAssignableFrom(type)){
                    ContentType ctype = contentTypes.getThrow(type, () -> new IllegalArgumentException("No content type for class: " + type.getSimpleName()));
                    String prefix = currentMod != null ? currentMod.name + "-" : "";
                    T one = (T)Vars.content.getByName(ctype, prefix + jsonData.asString());
                    if(one != null) return one;
                    T two = (T)Vars.content.getByName(ctype, jsonData.asString());

                    if(two != null) return two;
                    throw new IllegalArgumentException("No " + ctype + " found with name: '" + jsonData.asString() + "'");
                }
            }

            return super.readValue(type, elementType, jsonData, keyType);
        }
    };

    private ObjectMap<ContentType, TypeParser<?>> parsers = ObjectMap.of(
        ContentType.block, (TypeParser<Block>)(mod, name, value) -> {
            readBundle(ContentType.block, name, value);

            Block block;

            if(Vars.content.getByName(ContentType.block, name) != null){
                block = Vars.content.getByName(ContentType.block, name);

                if(value.has("type")){
                    throw new IllegalArgumentException("When overwriting an existing block, you may not declared its type. The original type will be used. Block: " + name);
                }
            }else{
                //TODO generate dynamically instead of doing.. this
                Class<? extends Block> type = resolve(value.getString("type"),
                "io.anuke.mindustry.world",
                "io.anuke.mindustry.world.blocks",
                "io.anuke.mindustry.world.blocks.defense",
                "io.anuke.mindustry.world.blocks.defense.turrets",
                "io.anuke.mindustry.world.blocks.distribution",
                "io.anuke.mindustry.world.blocks.logic",
                "io.anuke.mindustry.world.blocks.power",
                "io.anuke.mindustry.world.blocks.production",
                "io.anuke.mindustry.world.blocks.sandbox",
                "io.anuke.mindustry.world.blocks.storage",
                "io.anuke.mindustry.world.blocks.units"
                );

                block = type.getDeclaredConstructor(String.class).newInstance(mod + "-" + name);
            }

            currentContent = block;
            read(() -> {
                if(value.has("consumes")){
                    for(JsonValue child : value.get("consumes")){
                        if(child.name.equals("item")){
                            block.consumes.item(Vars.content.getByName(ContentType.item, child.asString()));
                        }else if(child.name.equals("items")){
                            block.consumes.add((Consume)parser.readValue(ConsumeItems.class, child));
                        }else if(child.name.equals("liquid")){
                            block.consumes.add((Consume)parser.readValue(ConsumeLiquid.class, child));
                        }else if(child.name.equals("power")){
                            if(child.isDouble()){
                                block.consumes.power(child.asFloat());
                            }else{
                                block.consumes.add((Consume)parser.readValue(ConsumePower.class, child));
                            }
                        }else if(child.name.equals("powerBuffered")){
                            block.consumes.powerBuffered(child.asFloat());
                        }else{
                            throw new IllegalArgumentException("Unknown consumption type: '" + child.name + "' for block '" + block.name + "'.");
                        }
                    }
                    value.remove("consumes");
                }

                readFields(block, value, true);

                //add research tech node
                if(value.has("research")){
                    TechTree.create(Vars.content.getByName(ContentType.block, value.get("research").asString()), block);
                }

                //make block visible
                if(value.has("requirements")){
                    block.buildVisibility = () -> true;
                }
            });

            return block;
        },
        ContentType.unit, (TypeParser<UnitType>)(mod, name, value) -> {
            readBundle(ContentType.unit, name, value);

            Class<BaseUnit> type = resolve(value.getString("type"), "io.anuke.mindustry.entities.type.base");
            UnitType unit = new UnitType(mod + "-" + name, supply(type));
            currentContent = unit;
            read(() -> readFields(unit, value, true));

            return unit;
        },
        ContentType.item, parser(ContentType.item, Item::new),
        ContentType.liquid, parser(ContentType.liquid, Liquid::new),
        ContentType.mech, parser(ContentType.mech, Mech::new),
        ContentType.zone, parser(ContentType.zone, Zone::new)
    );

    private <T extends Content> TypeParser<T> parser(ContentType type, Function<String, T> constructor){
        return (mod, name, value) -> {
            T item;
            if(Vars.content.getByName(type, name) != null){
                item = (T)Vars.content.getByName(type, name);
            }else{
                readBundle(type, name, value);

                item = constructor.get(mod + "-" + name);
            }
            currentContent = item;
            read(() -> readFields(item, value));
            return item;
        };
    }

    private void readBundle(ContentType type, String name, JsonValue value){
        String entryName = type + "." + currentMod.name + "-" + name + ".";
        I18NBundle bundle = Core.bundle;
        while(bundle.getParent() != null) bundle = bundle.getParent();

        if(value.has("name")){
            bundle.getProperties().put(entryName + "name", value.getString("name"));
            value.remove("name");
        }

        if(value.has("description")){
            bundle.getProperties().put(entryName + "description", value.getString("description"));
            value.remove("description");
        }
    }

    /** Call to read a content's extra info later.*/
    private void read(Runnable run){
        LoadedMod mod = currentMod;
        reads.add(() -> {
            this.currentMod = mod;
            run.run();
        });
    }

    private void init(){
        for(ContentType type : ContentType.all){
            Array<Content> arr = Vars.content.getBy(type);
            if(!arr.isEmpty()){
                Class<?> c = arr.first().getClass();
                //get base content class, skipping intermediates
                while(!(c.getSuperclass() == Content.class || c.getSuperclass() == UnlockableContent.class || Modifier.isAbstract(c.getSuperclass().getModifiers()))){
                    c = c.getSuperclass();
                }

                contentTypes.put(c, type);
            }
        }
    }

    public void finishParsing(){
        try{
            reads.each(Runnable::run);
        }catch(Exception e){
            throw new RuntimeException("Error occurred parsing content: " + currentContent, e);
        }
        reads.clear();
    }

    /**
     * Parses content from a json file.
     * @param name the name of the file without its extension
     * @param json the json to parse
     * @param type the type of content this is
     * @return the content that was parsed
     */
    public Content parse(LoadedMod mod, String name, String json, ContentType type) throws Exception{
        if(contentTypes.isEmpty()){
            init();
        }

        JsonValue value = parser.fromJson(null, json);
        if(!parsers.containsKey(type)){
            throw new SerializationException("No parsers for content type '" + type + "'");
        }

        currentMod = mod;
        Content c = parsers.get(type).parse(mod.name, name, value);
        c.mod = mod;
        checkNulls(c);
        return c;
    }

    private <T> T make(Class<T> type){
        try{
            java.lang.reflect.Constructor<T> cons = type.getDeclaredConstructor();
            cons.setAccessible(true);
            return cons.newInstance();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private <T> Supplier<T> supply(Class<T> type){
        try{
            java.lang.reflect.Constructor<T> cons = type.getDeclaredConstructor();
            return () -> {
                try{
                    return cons.newInstance();
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            };
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private Object field(Class<?> type, JsonValue value){
        return field(type, value.asString());
    }

    /** Gets a field from a static class by name, throwing a descriptive exception if not found. */
    private Object field(Class<?> type, String name){
        try{
            Object b = type.getField(name).get(null);
            if(b == null) throw new IllegalArgumentException(type.getSimpleName() + ": not found: '" + name + "'");
            return b;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private Object fieldOpt(Class<?> type, JsonValue value){
        try{
            Object b = type.getField(value.asString()).get(null);
            if(b == null) return null;
            return b;
        }catch(Exception e){
            return null;
        }
    }

    /** Checks all @NonNull fields in this object, recursively.
     * Throws an exception if any are null.*/
    private void checkNulls(Object object){
        checkNulls(object, new ObjectSet<>());
    }

    private void checkNulls(Object object, ObjectSet<Object> checked){
        checked.add(object);

        parser.getFields(object.getClass()).values().toArray().each(field -> {
            try{
                if(field.field.getType().isPrimitive()) return;

                Object obj = field.field.get(object);
                if(field.field.isAnnotationPresent(NonNull.class) && field.field.get(object) == null){
                    throw new RuntimeException("Field '" + field.field.getName() + "' in " + object.getClass().getSimpleName() + " is missing!");
                }

                if(obj != null && !checked.contains(obj)){
                    checkNulls(obj, checked);
                    checked.add(obj);
                }
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        });
    }

    private void readFields(Object object, JsonValue jsonMap, boolean stripType){
        if(stripType) jsonMap.remove("type");
        readFields(object, jsonMap);
    }

    private void readFields(Object object, JsonValue jsonMap){
        Class type = object.getClass();
        ObjectMap<String, FieldMetadata> fields = parser.getFields(type);
        for(JsonValue child = jsonMap.child; child != null; child = child.next){
            FieldMetadata metadata = fields.get(child.name().replace(" ", "_"));
            if(metadata == null){
                if(ignoreUnknownFields){
                    if(!child.name.equals("research")){
                        Log.err("{0}: Ignoring unknown field: " + child.name + " (" + type.getName() + ")", object);
                    }
                    continue;
                }else{
                    SerializationException ex = new SerializationException("Field not found: " + child.name + " (" + type.getName() + ")");
                    ex.addTrace(child.trace());
                    throw ex;
                }
            }
            Field field = metadata.field;
            try{
                field.set(object, parser.readValue(field.getType(), metadata.elementType, child, metadata.keyType));
            }catch(ReflectionException ex){
                throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
            }catch(SerializationException ex){
                ex.addTrace(field.getName() + " (" + type.getName() + ")");
                throw ex;
            }catch(RuntimeException runtimeEx){
                SerializationException ex = new SerializationException(runtimeEx);
                ex.addTrace(child.trace());
                ex.addTrace(field.getName() + " (" + type.getName() + ")");
                throw ex;
            }
        }
    }

    /** Tries to resolve a class from a list of potential class names. */
    private <T> Class<T> resolve(String base, String... potentials){
        for(String type : potentials){
            try{
                return (Class<T>)Class.forName(type + '.' + base);
            }catch(Exception ignored){
                try{
                    return (Class<T>)Class.forName(type + '$' + base);
                }catch(Exception ignored2){
                }
            }
        }
        throw new IllegalArgumentException("Types not found: " + base + "." + potentials[0]);
    }

    private interface FieldParser{
        Object parse(Class<?> type, JsonValue value) throws Exception;
    }

    private interface TypeParser<T extends Content>{
        T parse(String mod, String name, JsonValue value) throws Exception;
    }

}
