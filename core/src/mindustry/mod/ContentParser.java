package mindustry.mod;

import arc.*;
import arc.assets.*;
import arc.audio.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.mock.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.game.Objectives.*;
import mindustry.gen.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;

@SuppressWarnings("unchecked")
public class ContentParser{
    private static final boolean ignoreUnknownFields = true;
    private ObjectMap<Class<?>, ContentType> contentTypes = new ObjectMap<>();

    private ObjectMap<Class<?>, FieldParser> classParsers = new ObjectMap<Class<?>, FieldParser>(){{
        put(Effect.class, (type, data) -> field(Fx.class, data));
        put(Schematic.class, (type, data) -> {
            Object result = fieldOpt(Loadouts.class, data);
            if(result != null){
                return result;
            }else{
                String str = data.asString();
                if(str.startsWith(Schematics.base64Header)){
                    return Schematics.readBase64(str);
                }else{
                    return Schematics.read(Vars.tree.get("schematics/" + str + "." + Vars.schematicExtension));
                }
            }
        });
        put(StatusEffect.class, (type, data) -> {
            Object result = fieldOpt(StatusEffects.class, data);
            if(result != null){
                return result;
            }
            StatusEffect effect = new StatusEffect(currentMod.name + "-" + data.getString("name"));
            readFields(effect, data);
            return effect;
        });
        put(Color.class, (type, data) -> Color.valueOf(data.asString()));
        put(BulletType.class, (type, data) -> {
            if(data.isString()){
                return field(Bullets.class, data);
            }
            Class<? extends BulletType> bc = data.has("type") ? resolve(data.getString("type"), "mindustry.entities.bullet") : BasicBulletType.class;
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

            if(Core.assets.contains(path, Sound.class)) return Core.assets.get(path, Sound.class);
            ModLoadingSound sound = new ModLoadingSound();
            AssetDescriptor<?> desc = Core.assets.load(path, Sound.class);
            desc.loaded = result -> sound.sound = (Sound)result;
            desc.errored = Throwable::printStackTrace;
            return sound;
        });
        put(Objectives.Objective.class, (type, data) -> {
            Class<? extends Objectives.Objective> oc = data.has("type") ? resolve(data.getString("type"), "mindustry.game.Objectives") : SectorWave.class;
            data.remove("type");
            Objectives.Objective obj = make(oc);
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
    private Seq<Runnable> reads = new Seq<>();
    private Seq<Runnable> postreads = new Seq<>();
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

        private <T> T internalRead(Class<T> type, Class elementType, JsonValue jsonData, Class keyType){
            if(type != null){
                if(classParsers.containsKey(type)){
                    try{
                        return (T)classParsers.get(type).parse(type, jsonData);
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                }

                //try to parse "item/amount" syntax
                if(type == ItemStack.class && jsonData.isString() && jsonData.asString().contains("/")){
                    String[] split = jsonData.asString().split("/");

                    return (T)fromJson(ItemStack.class, "{item: " + split[0] + ", amount: " + split[1] + "}");
                }

                //try to parse "liquid/amount" syntax
                if(jsonData.isString() && jsonData.asString().contains("/")){
                    String[] split = jsonData.asString().split("/");
                    if(type == LiquidStack.class){
                        return (T)fromJson(LiquidStack.class, "{liquid: " + split[0] + ", amount: " + split[1] + "}");
                    }else if(type == ConsumeLiquid.class){
                        return (T)fromJson(ConsumeLiquid.class, "{liquid: " + split[0] + ", amount: " + split[1] + "}");
                    }
                }

                //try to load DrawBlock by instantiating it
                if(type == DrawBlock.class && jsonData.isString()){
                    return Reflect.make("mindustry.world.draw." + Strings.capitalize(jsonData.asString()));
                }

                if(Content.class.isAssignableFrom(type)){
                    ContentType ctype = contentTypes.getThrow(type, () -> new IllegalArgumentException("No content type for class: " + type.getSimpleName()));
                    String prefix = currentMod != null ? currentMod.name + "-" : "";
                    T one = (T)Vars.content.getByName(ctype, prefix + jsonData.asString());
                    if(one != null) return one;
                    T two = (T)Vars.content.getByName(ctype, jsonData.asString());

                    if(two != null) return two;
                    throw new IllegalArgumentException("\"" + jsonData.name + "\": No " + ctype + " found with name '" + jsonData.asString() + "'.\nMake sure '" + jsonData.asString() + "' is spelled correctly, and that it really exists!\nThis may also occur because its file failed to parse.");
                }
            }

            return super.readValue(type, elementType, jsonData, keyType);
        }
    };

    private ObjectMap<ContentType, TypeParser<?>> parsers = ObjectMap.of(
        ContentType.block, (TypeParser<Block>)(mod, name, value) -> {
            readBundle(ContentType.block, name, value);

            Block block;
            boolean exists;

            if(locate(ContentType.block, name) != null){
                block = locate(ContentType.block, name);
                exists = true;

                if(value.has("type")){
                    throw new IllegalArgumentException("When defining properties for an existing block, you must not re-declare its type. The original type will be used. Block: " + name);
                }
            }else{
                //TODO generate dynamically instead of doing.. this
                Class<? extends Block> type;
                exists = false;

                try{
                    type = resolve(getType(value),
                    "mindustry.world",
                    "mindustry.world.blocks",
                    "mindustry.world.blocks.defense",
                    "mindustry.world.blocks.defense.turrets",
                    "mindustry.world.blocks.distribution",
                    "mindustry.world.blocks.liquid",
                    "mindustry.world.blocks.logic",
                    "mindustry.world.blocks.power",
                    "mindustry.world.blocks.production",
                    "mindustry.world.blocks.sandbox",
                    "mindustry.world.blocks.storage",
                    "mindustry.world.blocks.units"
                    );
                }catch(IllegalArgumentException e){
                    type = Block.class;
                }

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
                            if(child.isNumber()){
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

                if(block.size > BuildBlock.maxSize){
                    throw new IllegalArgumentException("Blocks cannot be larger than " + BuildBlock.maxSize);
                }

                //add research tech node
                if(research[0] != null){
                    //TODO only works with blocks
                    Block parent = find(ContentType.block, research[0]);
                    TechNode baseNode = exists && TechTree.all.contains(t -> t.content == block) ? TechTree.all.find(t -> t.content == block) : TechTree.create(parent, block);
                    LoadedMod cur = currentMod;

                    postreads.add(() -> {
                        currentContent = block;
                        currentMod = cur;

                        if(baseNode.parent != null){
                            baseNode.parent.children.remove(baseNode);
                        }

                        TechNode parnode = TechTree.all.find(t -> t.content == parent);
                        if(parnode == null){
                            throw new IllegalArgumentException("Block '" + parent.name + "' isn't in the tech tree, but '" + block.name + "' requires it to be researched.");
                        }
                        if(!parnode.children.contains(baseNode)){
                            parnode.children.add(baseNode);
                        }
                        baseNode.parent = parnode;
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

            UnitType unit;
            if(locate(ContentType.unit, name) == null){
                unit = new UnitType(mod + "-" + name);
                unit.constructor = Reflect.cons(resolve(Strings.capitalize(getType(value)), "mindustry.gen"));
            }else{
                unit = locate(ContentType.unit, name);
            }

            currentContent = unit;
            read(() -> readFields(unit, value, true));

            return unit;
        },
        ContentType.item, parser(ContentType.item, Item::new),
        ContentType.liquid, parser(ContentType.liquid, Liquid::new)
        //ContentType.sector, parser(ContentType.sector, SectorPreset::new)
    );

    private String getString(JsonValue value, String key){
        if(value.has(key)){
            return value.getString(key);
        }else{
            throw new IllegalArgumentException("You are missing a \"" + key + "\". It must be added before the file can be parsed.");
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

    private <T extends Content> TypeParser<T> parser(ContentType type, Func<String, T> constructor){
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
        UnlockableContent cont = locate(type, name) instanceof UnlockableContent ? locate(type, name) : null;

        String entryName = cont == null ? type + "." + currentMod.name + "-" + name + "." : type + "." + cont.name + ".";
        I18NBundle bundle = Core.bundle;
        while(bundle.getParent() != null) bundle = bundle.getParent();

        if(value.has("name")){
            if(!Core.bundle.has(entryName + "name")){
                bundle.getProperties().put(entryName + "name", value.getString("name"));
                if(cont != null) cont.localizedName = value.getString("name");
            }
            value.remove("name");
        }

        if(value.has("description")){
            if(!Core.bundle.has(entryName + "description")){
                bundle.getProperties().put(entryName + "description", value.getString("description"));
                if(cont != null) cont.description = value.getString("description");
            }
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
            Seq<Content> arr = Vars.content.getBy(type);
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

    private void attempt(Runnable run){
        try{
            run.run();
        }catch(Throwable t){
            //don't overwrite double errors
            markError(currentContent, t);
        }
    }

    public void finishParsing(){
        reads.each(this::attempt);
        postreads.each(this::attempt);
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
    public Content parse(LoadedMod mod, String name, String json, Fi file, ContentType type) throws Exception{
        if(contentTypes.isEmpty()){
            init();
        }

        //remove extra # characters to make it valid json... apparently some people have *unquoted* # characters in their json
        if(file.extension().equals("json")){
            json = json.replace("#", "\\#");
        }

        JsonValue value = parser.fromJson(null, Jval.read(json).toString(Jformat.plain));

        if(!parsers.containsKey(type)){
            throw new SerializationException("No parsers for content type '" + type + "'");
        }

        currentMod = mod;
        boolean located = locate(type, name) != null;
        Content c = parsers.get(type).parse(mod.name, name, value);
        c.minfo.sourceFile = file;
        toBeParsed.add(c);

        if(!located){
            c.minfo.mod = mod;
        }
        return c;
    }

    public void markError(Content content, LoadedMod mod, Fi file, Throwable error){
        content.minfo.mod = mod;
        content.minfo.sourceFile = file;
        content.minfo.error = makeError(error, file);
        content.minfo.baseError = error;
        if(mod != null){
            mod.erroredContent.add(content);
        }
    }

    public void markError(Content content, Throwable error){
        if(content.minfo != null && !content.hasErrored()){
            markError(content, content.minfo.mod, content.minfo.sourceFile, error);
        }
    }

    private String makeError(Throwable t, Fi file){
        StringBuilder builder = new StringBuilder();
        builder.append("[lightgray]").append("File: ").append(file.name()).append("[]\n\n");

        if(t.getMessage() != null && t instanceof JsonParseException){
            builder.append("[accent][[JsonParse][] ").append(":\n").append(t.getMessage());
        }else if(t instanceof NullPointerException){
            builder.append(Strings.neatError(t));
        }else{
            Seq<Throwable> causes = Strings.getCauses(t);
            for(Throwable e : causes){
                builder.append("[accent][[").append(e.getClass().getSimpleName().replace("Exception", ""))
                .append("][] ")
                .append(e.getMessage() != null ?
                e.getMessage().replace("mindustry.", "").replace("arc.", "") : "").append("\n");
            }
        }
        return builder.toString();
    }

    private <T extends MappableContent> T locate(ContentType type, String name){
        T first = Vars.content.getByName(type, name); //try vanilla replacement
        return first != null ? first : Vars.content.getByName(type, currentMod.name + "-" + name);
    }

    private <T> T make(Class<T> type){
        try{
            Constructor<T> cons = type.getDeclaredConstructor();
            cons.setAccessible(true);
            return cons.newInstance();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private <T> T make(Class<T> type, String name){
        try{
            Constructor<T> cons = type.getDeclaredConstructor(String.class);
            cons.setAccessible(true);
            return cons.newInstance(name);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private <T> Prov<T> supply(Class<T> type){
        try{
            Constructor<T> cons = type.getDeclaredConstructor();
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

        parser.getFields(object.getClass()).values().toSeq().each(field -> {
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
                    Log.warn("@: Ignoring unknown field: " + child.name + " (" + type.getName() + ")", object);
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
            }catch(IllegalAccessException ex){
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
