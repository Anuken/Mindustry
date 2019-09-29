package io.anuke.mindustry.mod;

import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.reflect.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.arc.util.serialization.Json.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

@SuppressWarnings("unchecked")
public class ContentParser{
    private static final boolean ignoreUnknownFields = true;
    private ObjectMap<Class<?>, ContentType> contentTypes = new ObjectMap<>();

    private Json parser = new Json(){
        public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData){
            try{
                if(type == BulletType.class){
                    BulletType b = (BulletType)Bullets.class.getField(jsonData.asString()).get(null);
                    if(b == null) throw new IllegalArgumentException("Bullet type not found: " + jsonData.asString());
                    return (T)b;
                }

                if(type != null && Content.class.isAssignableFrom(type)){
                    return (T)Vars.content.getByName(contentTypes.getThrow(type, () -> new IllegalArgumentException("No content type for class: " + type.getSimpleName())), jsonData.asString());
                }

                return super.readValue(type, elementType, jsonData);
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    };

    private ObjectMap<ContentType, TypeParser<?>> parsers = ObjectMap.of(
        ContentType.block, (TypeParser<Block>)(mod, name, value) -> {
            String clas = value.getString("type");
            Class<Block> type = resolve("io.anuke.mindustry.world." + clas, "io.anuke.mindustry.world.blocks." + clas, "io.anuke.mindustry.world.blocks.defense" + clas);
            Block block = type.getDeclaredConstructor(String.class).newInstance(mod + "-" + name);
            value.remove("type");
            readFields(block, value);

            //make block visible
            if(block.buildRequirements != null){
                block.buildVisibility = () -> true;
            }

            return block;
        },
        ContentType.item, (TypeParser<Item>)(mod, name, value) -> {
            Item item = new Item(mod + "-" + name, new Color(Color.black));
            readFields(item, value);
            return item;
        },
        ContentType.unit, (TypeParser<UnitType>)(mod, name, value) -> {
            String clas = value.getString("type");
            Class<BaseUnit> type = resolve("io.anuke.mindustry.entities.type.base." + clas);
            java.lang.reflect.Constructor<BaseUnit> cons = type.getDeclaredConstructor();
            UnitType unit = new UnitType(mod + "-" + name, type, () -> {
                try{
                    return cons.newInstance();
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            });
            value.remove("type");
            readFields(unit, value);

            return unit;
        }
    );

    private void init(){
        for(ContentType type : ContentType.all){
            Array<Content> arr = Vars.content.getBy(type);
            if(!arr.isEmpty()){
                Class<?> c = arr.first().getClass();
                //get base content class, skipping intermediates
                while(!(c.getSuperclass() == Content.class || c.getSuperclass() == UnlockableContent.class || c.getSuperclass() == UnlockableContent.class)){
                    c = c.getSuperclass();
                }

                contentTypes.put(c, type);
            }
        }
    }

    /**
     * Parses content from a json file.
     * @param name the name of the file without its extension
     * @param json the json to parse
     * @param type the type of content this is
     * @return the content that was parsed
     */
    public Content parse(String mod, String name, String json, ContentType type) throws Exception{
        if(contentTypes.isEmpty()){
            init();
        }

        JsonValue value = parser.fromJson(null, json);
        if(!parsers.containsKey(type)){
            throw new SerializationException("No parsers for content type '" + type + "'");
        }

        Content c = parsers.get(type).parse(mod, name, value);
        checkNulls(c);
        return c;
    }

    private void checkNulls(Object object){
        checkNulls(object, new ObjectSet<>());
    }

    private void checkNulls(Object object, ObjectSet<Object> checked){
        checked.add(object);

        parser.getFields(object.getClass()).each((name, field) -> {
            try{
                if(field.field.getType().isPrimitive()) return;

                Object obj = field.field.get(object);
                if(field.field.isAnnotationPresent(NonNull.class) && field.field.get(object) == null){
                    throw new RuntimeException("Field '" + name + "' in " + object.getClass().getSimpleName() + " is missing!");
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

    private void readFields(Object object, JsonValue jsonMap){
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
                field.set(object, parser.readValue(field.getType(), metadata.elementType, child));
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
    private <T> Class<T> resolve(String... potentials) throws Exception{
        for(String type : potentials){
            try{
                return (Class<T>)Class.forName(type);
            }catch(Exception ignored){
            }
        }
        throw new IllegalArgumentException("Type not found: " + potentials[0]);
    }

    public interface TypeParser<T extends Content>{
        T parse(String mod, String name, JsonValue value) throws Exception;
    }

}
