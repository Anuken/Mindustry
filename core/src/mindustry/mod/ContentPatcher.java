package mindustry.mod;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;

import java.lang.reflect.*;
import java.util.*;

/** The current implementation is awful. Consider it a proof of concept. */
@SuppressWarnings("unchecked")
public class ContentPatcher{
    private static final Object root = new Object();
    private static final ObjectMap<String, ContentType> nameToType = new ObjectMap<>();

    private Json json;
    private boolean applied;
    private ContentLoader contentLoader;
    private ObjectSet<PatchRecord> usedpatches = new ObjectSet<>();
    private Seq<Runnable> resetters = new Seq<>();
    private Seq<Runnable> afterCallbacks = new Seq<>();

    static{
        for(var type : ContentType.all){
            if(type.name().indexOf('_') == -1) nameToType.put(type.toString().toLowerCase(Locale.ROOT), type);
        }
    }

    public void apply(String patch) throws Exception{
        json = Vars.mods.getContentParser().getJson();

        applied = true;
        contentLoader = Vars.content.copy();

        try{
            JsonValue value = json.fromJson(null, Jval.read(patch).toString(Jformat.plain));
            for(var child : value){
                assign(root, child.name, child, null, null, null);
            }

            afterCallbacks.each(Runnable::run);
        }catch(Exception e){
            Log.err("Failed to apply patch: " + patch, e);
        }
    }

    public void unapply(){
        if(!applied) return;

        Vars.content = contentLoader;
        applied = false;

        resetters.reverse();
        for(var reset : resetters){
            try{
                reset.run();
            }catch(Throwable e){
                Log.err("Failed to un-apply patch!", e);
            }
        }
        resetters.clear();

        //this should never throw an exception
        afterCallbacks.each(Runnable::run);
        afterCallbacks.clear();
    }

    void assign(Object object, String field, Object value, @Nullable FieldData metadata, @Nullable Object parentObject, @Nullable String parentField) throws Exception{
        if(field == null || field.isEmpty()) return;

        char prefix = 0;

        //fetch modifier (+ or -) and concat it to the end, turning `+array` into `array.+`
        if(field.charAt(0) == '+'){
            prefix = field.charAt(0);
            field = field.substring(1);
        }else if(field.endsWith(".+")){
            prefix = field.charAt(field.length() - 1);
            field = field.substring(0, field.length() - 2);
        }

        //field.field2.field3 nested syntax
        if(field.indexOf('.') != -1){
            //resolve the field chain until the final field is reached
            String[] path = field.split("\\.");
            for(int i = 0; i < path.length - 1; i++){
                Object[] result = resolve(object, path[i], metadata);
                if(result == null){
                    //TODO report error
                    return;
                }
                object = result[0];
                metadata = (FieldData)result[1];
            }
            field = path[path.length - 1];
        }

        if(object instanceof Content c){
            after(c::afterPatch);
        }

        if(object == root){
            if(value instanceof JsonValue jval && jval.isObject()){
                for(var child : jval){
                    Object[] otherResolve = resolve(object, jval.name, null);
                    if(otherResolve != null && otherResolve[0] instanceof ObjectMap map && map.containsKey(child.name)){
                        assign(otherResolve[0], child.name, child, (FieldData)otherResolve[1], object, field);
                    }else{
                        Log.warn("Content not found: @.@", field, child.name);
                    }
                }
            }else{
                warn("Content '@' cannot be assigned.", field);
            }
        }else if(object instanceof Seq<?> || object.getClass().isArray()){ //TODO

            if(prefix == '+'){
                //handle array addition syntax
                if(object instanceof Seq s){
                    modifiedField(parentObject, parentField, s.copy());

                    assignValue(object, field, metadata, () -> null, val -> s.add(val), value, false);
                }else{
                    modifiedField(parentObject, parentField, copyArray(object));

                    var fobj = object;
                    assignValue(parentObject, parentField, metadata, () -> null, val -> {
                        try{
                            //create copy array, put the new object in the last slot, and assign the parent's field to it
                            int len = Array.getLength(fobj);
                            Object copy = Array.newInstance(fobj.getClass().getComponentType(), len + 1);
                            Array.set(copy, len - 1, val);
                            System.arraycopy(fobj, 0, copy, 0, len);

                            assign(parentObject, parentField, copy, null, null, null);
                        }catch(Exception e){
                            throw new RuntimeException(e);
                        }
                    }, value, false);
                }
            }else{
                int i = Strings.parseInt(field);
                int length = object instanceof Seq s ? s.size : Array.getLength(object);

                if(i == Integer.MIN_VALUE){
                    warn("Invalid number for array access: '@'", field);
                    return;
                }else if(i < 0 || i >= length){
                    warn("Number outside of array bounds: '" + field + "' (length is " + length + ")");
                    return;
                }

                if(object instanceof Seq s){
                    var copy = s.copy();
                    reset(() -> s.set(copy));

                    assignValue(object, field, metadata, () -> s.get(i), val -> s.set(i, val), value, false);
                }else{
                    modifiedField(parentObject, parentField, copyArray(object));

                    var fobj = object;
                    assignValue(object, field, metadata, () -> Array.get(fobj, i), val -> Array.set(fobj, i, val), value, false);
                }
            }
        }else if(object instanceof ObjectMap map){
            if(metadata == null){
                warn("ObjectMap cannot be parsed without metadata: @.@", parentObject, parentField);
                return;
            }
            Object key = convertKeyType(field, metadata.keyType);
            if(key == null){
                warn("Null key: '@'", field);
                return;
            }

            var copy = map.copy();
            reset(() -> map.set(copy));

            assignValue(object, field, new FieldData(metadata.elementType, null, null), () -> map.get(key), val -> map.put(key, val), value, false);
        }else{
            Class<?> actualType = object.getClass();
            if(actualType.isAnonymousClass()) actualType = actualType.getSuperclass();

            var fields = json.getFields(actualType);
            var fdata = fields.get(field);
            if(fdata != null){
                if(checkField(fdata.field)) return;

                var fobj = object;
                assignValue(object, field, new FieldData(fdata), () -> Reflect.get(fobj, fdata.field), fv -> Reflect.set(fobj, fdata.field, fv), value, true);
            }else{
                warn("Unknown field: '@' for '@'", field, actualType.getName());
            }
        }
    }

    void assignValue(Object object, String field, FieldData metadata, Prov getter, Cons setter, Object value, boolean modify) throws Exception{
        Object prevValue = getter.get();

        if(value instanceof JsonValue jsv){ //setting values from object
            if(prevValue == null || !jsv.isObject() || jsv.has("type")){
                if(modify) modifiedField(object, field, getter.get());
                try{
                    setter.get(json.readValue(metadata.type, metadata.elementType, jsv));
                }catch(Throwable e){
                    warn("Failed to read value @.@ = @: @ (type = @ elementType = @)", object, field, value, e.getMessage(), metadata.type, metadata.elementType);
                }
            }else{
                //assign each field manually
                var childFields = json.getFields(prevValue.getClass().isAnonymousClass() ? prevValue.getClass().getSuperclass() : prevValue.getClass());
                for(var child : jsv){
                    if(child.name != null){
                        assign(prevValue, child.name, child, !childFields.containsKey(child.name) ? null : new FieldData(childFields.get(child.name)), object, field);
                    }
                }
            }
        }else{
            //direct value is set
            if(modify) modifiedField(object, field, prevValue);

            setter.get(value);
        }
    }

    /**
     * 0: the object
     * 1: the field metadata for the object to use with deserializing collection types
     * */
    Object[] resolve(Object object, String field, @Nullable FieldData metadata) throws Exception{
        if(object == null) return null;

        if(object == root){
            ContentType ctype = nameToType.get(field);
            if(ctype == null){
                warn("Invalid content type: " + field);
                return null;
            }
            return new Object[]{Vars.content.getNamesBy(ctype), new FieldData(ObjectMap.class, MappableContent.class, String.class)};
        }else if(object instanceof Seq<?> || object.getClass().isArray()){
            int i = Strings.parseInt(field);
            int length = object instanceof Seq s ? s.size : Array.getLength(object);

            if(i == Integer.MIN_VALUE){
                warn("Invalid number for array access: '@'", field);
                return null;
            }else if(i < 0 || i >= length){
                warn("Number outside of array bounds: '" + field + "' (length is " + length + ")");
                return null;
            }

            return new Object[]{object instanceof Seq s ? s.get(i) : Array.get(object, i), null};
        }else if(object instanceof ObjectMap map){
            Object key = convertKeyType(field, metadata.keyType);
            if(key == null){
                warn("Null key: '@'", field);
                return null;
            }
            Object mapValue = map.get(key);
            if(mapValue == null){
                warn("No key found: '@'", field);
                return null;
            }
            return new Object[]{mapValue, null};
        }else{
            Class<?> actualType = object.getClass();
            if(actualType.isAnonymousClass()) actualType = actualType.getSuperclass();

            var fields = json.getFields(actualType);
            var fdata = fields.get(field);
            if(fdata != null){
                if(checkField(fdata.field)) return null;

                return new Object[]{fdata.field.get(object), new FieldData(fdata)};
            }else{
                warn("Unknown field: '@' for '@'", field, actualType.getName());
                return null;
            }
        }
    }

    boolean checkField(Field field){
        if(field.isAnnotationPresent(NoPatch.class) || field.getDeclaringClass().isAnnotationPresent(NoPatch.class)){
            warn("Field '@' cannot be edited.", field);
            return true;
        }
        return false;
    }

    void modifiedField(Object target, String field, Object value){
        if(!applied || target == null) return;

        var fields = json.getFields(target.getClass());
        var meta = fields.get(field);
        if(meta != null){

            var record = new PatchRecord(target, meta.field, value);
            if(usedpatches.add(record)){
                resetters.add(() -> {
                    try{
                        record.field.set(record.target, record.value);
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                });
            }
        }else{
            warn("Missing field " + field + " for object " + target);
        }
    }

    void reset(Runnable run){
        resetters.add(run);
    }

    Object convertKeyType(String string, Class<?> type){
        return json.fromJson(type, string);
    }

    //TODO crash?
    void warn(String error, Object... fmt){
        Log.warn(error, fmt);
    }

    void after(Runnable run){
        afterCallbacks.add(run);
    }

    static Object copyArray(Object object){
        if(object instanceof int[] i) return i.clone();
        if(object instanceof long[] i) return i.clone();
        if(object instanceof short[] i) return i.clone();
        if(object instanceof byte[] i) return i.clone();
        if(object instanceof boolean[] i) return i.clone();
        if(object instanceof char[] i) return i.clone();
        if(object instanceof float[] i) return i.clone();
        if(object instanceof double[] i) return i.clone();
        return ((Object[])object).clone();
    }

    private static class FieldData{
        Class type, elementType, keyType;

        public FieldData(Class type, Class elementType, Class keyType){
            this.type = type;
            this.elementType = elementType;
            this.keyType = keyType;
        }

        public FieldData(FieldMetadata data){
            this(data.field.getType(), data.elementType, data.keyType);
        }
    }

    private static class PatchRecord{
        Object target;
        Field field;
        Object value;

        PatchRecord(Object target, Field field, Object value){
            this.target = target;
            this.field = field;
            this.value = value;
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            PatchRecord that = (PatchRecord)o;
            return target.equals(that.target) && field.equals(that.field);
        }

        @Override
        public int hashCode(){
            int result = target.hashCode();
            result = 31 * result + field.hashCode();
            return result;
        }
    }
}
