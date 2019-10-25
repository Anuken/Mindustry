package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.audio.mock.*;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
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
import io.anuke.mindustry.content.TechTree.*;
import io.anuke.mindustry.ctype.*;
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
import io.anuke.mindustry.world.meta.*;

import java.lang.reflect.*;

@SuppressWarnings("unchecked")
public class ContentParser{
    private static final boolean ignoreUnknownFields = true;
    private ObjectMap<Class<?>, ContentType> contentTypes = new ObjectMap<>();
    private ObjectMap<Class<?>, FieldParser> classParsers = new ObjectMap<Class<?>, FieldParser>(){{
        put(Effect.class, (type, data) -> field(Fx.class, data));
        put(StatusEffect.class, (type, data) -> field(StatusEffects.class, data));
        put(Loadout.class, (type, data) -> field(Loadouts.class, data));
        put(Color.class, (type, data) -> Color.valueOf(data.asString()));
        put(BulletType.class, (type, data) -> {
            if(data.isString()){
                return field(Bullets.class, data);
            }
            Class<? extends BulletType> bc = data.has("type") ? resolve(data.getString("type"), "io.anuke.mindustry.entities.bullet") : BasicBulletType.class;
            data.remove("type");
            BulletType result = make(bc);
            readFields(result, data);
            return result;
        });
        put(Sound.class, (type, data) -> {
            if(fieldOpt(Sounds.class, data) != null) return fieldOpt(Sounds.class, data);
            if(Vars.headless) return new MockSound();

            String name = "sounds/" + data.asString();
            String path = Vars.tree.get(name + ".ogg").exists() && !Vars.ios ? name + ".ogg" : name + ".mp3";
            ModLoadingSound sound = new ModLoadingSound();
            Core.assets.load(path, Sound.class).loaded = result -> {
                sound.sound = (Sound)result;
            };
            return sound;
        });
        put(Objective.class, (type, data) -> {
            Class<? extends Objective> oc = data.has("type") ? resolve(data.getString("type"), "io.anuke.mindustry.game.Objectives") : ZoneWave.class;
            data.remove("type");
            Objective obj = make(oc);
            readFields(obj, data);
            return obj;
        });
        put(Weapon.class, (type, data) -> {
            Weapon weapon = new Weapon();
            readFields(weapon, data);
            weapon.name = currentMod.name + "-" + weapon.name;
            return weapon;
        });
    }};
    /** Stores things that need to be parsed fully, e.g. reading fields of content.
     * This is done to accomodate binding of content names first.*/
    private Array<Runnable> reads = new Array<>();
    private Array<Runnable> postreads = new Array<>();
    private ObjectSet<Object> toBeParsed = new ObjectSet<>();
    private LoadedMod currentMod;
    private Content currentContent;

    private Json parser = new Json(){
        @Override
        public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData, Class keyType){
            T t = internalRead(type, elementType, jsonData, keyType);
            if(t != null) checkNullFields(t);
            return t;
        }

        private <T> T  internalRead(Class<T> type, Class elementType, JsonValue jsonData, Class keyType){
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
                    throw new IllegalArgumentException("\"" + jsonData.name + "\": No " + ctype + " found with name '" + jsonData.asString() + "'.");
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
                    throw new IllegalArgumentException("When overwriting an existing block, you must not re-declare its type. The original type will be used. Block: " + name);
                }
            }else{
                //TODO generate dynamically instead of doing.. this
                Class<? extends Block> type = resolve(getType(value),
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

                block = make(type, mod + "-" + name);
            }

            currentContent = block;

            String[] research = {null};

            //add research tech node
            if(value.has("research")){
                research[0] = value.get("research").asString();
                value.remove("research");
            }

            read(() -> {
                if(value.has("consumes")){
                    for(JsonValue child : value.get("consumes")){
                        if(child.name.equals("item")){
                            block.consumes.item(find(ContentType.item, child.asString()));
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
                if(research[0] != null){
                    Block parent = find(ContentType.block, research[0]);
                    TechNode baseNode = TechTree.create(parent, block);

                    postreads.add(() -> {
                        TechNode parnode = TechTree.all.find(t -> t.block == parent);
                        if(!parnode.children.contains(baseNode)){
                            parnode.children.add(baseNode);
                        }
                    });
                }

                //make block visible by default if there are requirements and no visibility set
                if(value.has("requirements") && block.buildVisibility == BuildVisibility.hidden){
                    block.buildVisibility = BuildVisibility.shown;
                }
            });

            return block;
        },
        ContentType.unit, (TypeParser<UnitType>)(mod, name, value) -> {
            readBundle(ContentType.unit, name, value);

            Class<BaseUnit> type = resolve(getType(value), "io.anuke.mindustry.entities.type.base");
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

    private String getString(JsonValue value, String key){
        if(value.has(key)){
            return value.getString(key);
        }else{
            throw new IllegalArgumentException((currentContent == null ? "" : currentContent.sourceFile + ": ") + "You are missing a \"" + key + "\". It must be added before the file can be parsed.");
        }
    }

    private String getType(JsonValue value){
        return getString(value, "type");
    }

    private <T extends Content> T find(ContentType type, String name){
        Content c = Vars.content.getByName(type, name);
        if(c == null) c = Vars.content.getByName(type, currentMod.name + "-" + name);
        if(c == null) throw new IllegalArgumentException("No " + type + " found with name '" + name + "'");
        return (T)c;
    }

    private <T extends Content> TypeParser<T> parser(ContentType type, Function<String, T> constructor){
        return (mod, name, value) -> {
            T item;
            if(Vars.content.getByName(type, name) != null){
                item = (T)Vars.content.getByName(type, name);
                readBundle(type, name, value);
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
        UnlockableContent cont = Vars.content.getByName(type, name) instanceof UnlockableContent ?
                                Vars.content.getByName(type, name) : null;

        String entryName = cont == null ? type + "." + currentMod.name + "-" + name + "." : type + "." + cont.name + ".";
        I18NBundle bundle = Core.bundle;
        while(bundle.getParent() != null) bundle = bundle.getParent();

        if(value.has("name")){
            bundle.getProperties().put(entryName + "name", value.getString("name"));
            if(cont != null) cont.localizedName = value.getString("name");
            value.remove("name");
        }

        if(value.has("description")){
            bundle.getProperties().put(entryName + "description", value.getString("description"));
            if(cont != null) cont.description = value.getString("description");
            value.remove("description");
        }
    }

    /** Call to read a content's extra info later.*/
    private void read(Runnable run){
        Content cont = currentContent;
        LoadedMod mod = currentMod;
        reads.add(() -> {
            this.currentMod = mod;
            this.currentContent = cont;
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
            postreads.each(Runnable::run);
        }catch(Exception e){
            Vars.mods.handleError(new ModLoadException("Error occurred parsing content: " + currentContent, currentContent, e), currentMod);
        }
        reads.clear();
        postreads.clear();
        toBeParsed.clear();
    }

    /**
     * Parses content from a json file.
     * @param name the name of the file without its extension
     * @param json the json to parse
     * @param type the type of content this is
     * @param file file that this content is being parsed from
     * @return the content that was parsed
     */
    public Content parse(LoadedMod mod, String name, String json, FileHandle file, ContentType type) throws Exception{
        if(contentTypes.isEmpty()){
            init();
        }

        JsonValue value = parser.fromJson(null, json);
        if(!parsers.containsKey(type)){
            throw new SerializationException("No parsers for content type '" + type + "'");
        }

        currentMod = mod;
        boolean exists = Vars.content.getByName(type, name) != null;
        Content c = parsers.get(type).parse(mod.name, name, value);
        toBeParsed.add(c);
        if(!exists){
            c.sourceFile = file;
            c.mod = mod;
        }
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

    private <T> T make(Class<T> type, String name){
        try{
            java.lang.reflect.Constructor<T> cons = type.getDeclaredConstructor(String.class);
            cons.setAccessible(true);
            return cons.newInstance(name);
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
            return type.getField(value.asString()).get(null);
        }catch(Exception e){
            return null;
        }
    }

    private void checkNullFields(Object object){
        if(object instanceof Number || object instanceof String || toBeParsed.contains(object)) return;

        parser.getFields(object.getClass()).values().toArray().each(field -> {
            try{
                if(field.field.getType().isPrimitive()) return;

                if(field.field.isAnnotationPresent(NonNull.class) && field.field.get(object) == null){
                    throw new RuntimeException("'" + field.field.getName() + "' in " + object.getClass().getSimpleName() + " is missing!");
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
        toBeParsed.remove(object);
        Class type = object.getClass();
        ObjectMap<String, FieldMetadata> fields = parser.getFields(type);
        for(JsonValue child = jsonMap.child; child != null; child = child.next){
            FieldMetadata metadata = fields.get(child.name().replace(" ", "_"));
            if(metadata == null){
                if(ignoreUnknownFields){
                    Log.err("{0}: Ignoring unknown field: " + child.name + " (" + type.getName() + ")", object);
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
        if(!base.isEmpty() && Character.isLowerCase(base.charAt(0))) base = Strings.capitalize(base);

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
