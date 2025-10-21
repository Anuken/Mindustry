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

@SuppressWarnings("unchecked")
public class ContentPatcher{
    private static final Object root = new Object();
    private static final ObjectMap<String, ContentType> nameToType = new ObjectMap<>();

    private Json json; //TODO ContentParser.json??
    private boolean applied;
    private ContentLoader contentLoader;
    private ObjectSet<PatchRecord> usedpatches = new ObjectSet<>();
    private Seq<PatchRecord> patches = new Seq<>();
    private Seq<Runnable> resetters = new Seq<>();

    static{
        for(var type : ContentType.all){
            if(type.name().indexOf('_') == -1) nameToType.put(type.toString().toLowerCase(Locale.ROOT), type);
        }
    }

    public void apply(String patch) throws Exception{
        json = Vars.mods.getContentParser().getJson();

        applied = true;
        contentLoader = Vars.content.copy();

        JsonValue value = json.fromJson(null, Jval.read(patch).toString(Jformat.plain));
        for(var child : value){
            assign(root, child.name, child, null, null, null);
        }
    }

    public void unapply() throws Exception{
        if(!applied) return;

        Vars.content = contentLoader;
        applied = false;

        for(var record : patches){
            assign(record.target, record.field, record.value, record.data, null, null);
        }

        resetters.each(Runnable::run);
        resetters.clear();
    }

    void assign(Object object, String field, Object value, @Nullable FieldMetadata metadata, @Nullable Object parentObject, @Nullable String parentField) throws Exception{
        if(field == null || field.isEmpty()) return;

        //fetch modifier (+ or -) and concat it to the end, turning `+array` into `array.+`
        if(field.charAt(0) == '-' || field.charAt(0) == '+'){
            char prefix = field.charAt(0);
            field = field.substring(1) + "." + prefix;
        }

        //field.field2.field3 nested syntax
        if(field.indexOf('.') != -1){
            //resolve the field chain until the final field is reached
            String[] path = field.split("\\.");
            for(int i = 0; i < path.length - 1; i++){
                Object[] result = resolve(object, path[i], null, null);
                if(result == null){
                    //TODO report error
                    return;
                }
                object = result[0];
                metadata = (FieldMetadata)result[1];
            }
            field = path[path.length - 1];
        }

        if(object == root){
            warn("Content cannot be assigned.");
        }else if(object instanceof Seq<?> || object.getClass().isArray()){ //TODO

            if(field.length() == 1 && (field.charAt(0) == '+')){
                //handle array addition syntax
                if(object instanceof Seq s){
                    modified(parentObject, parentField, s.copy(), null);

                    assignValue(object, field, metadata, () -> null, val -> s.add(val), value, false);
                }else{
                    modified(parentObject, parentField, copyArray(object), null);

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
                    modified(parentObject, parentField, s.copy(), null);

                    assignValue(object, field, metadata, () -> s.get(i), val -> s.set(i, val), value, false);
                }else{
                    modified(parentObject, parentField, copyArray(object), null);

                    var fobj = object;
                    assignValue(object, field, metadata, () -> Array.get(fobj, i), val -> Array.set(fobj, i, val), value, false);
                }
            }
        }else if(object instanceof ObjectMap map){ //TODO
            if(metadata == null){
                warn("ObjectMap cannot be parsed without metadata.");
                return;
            }
            Object key = convertKeyType(field, metadata.keyType);
            if(key == null){
                warn("Null key: '@'", field);
                return;
            }
            modified(parentObject, parentField, map.copy(), metadata);
            assignValue(object, field, metadata, () -> map.get(key), val -> map.put(key, val), value, false);
        }else{
            Class<?> actualType = object.getClass();
            if(actualType.isAnonymousClass()) actualType = actualType.getSuperclass();

            var fields = json.getFields(actualType);
            var fdata = fields.get(field);
            if(fdata != null){
                if(checkField(fdata.field)) return;

                var fobj = object;
                assignValue(object, field, metadata, () -> Reflect.get(fobj, fdata.field), fv -> Reflect.set(fobj, fdata.field, fv), value, true);
            }else{
                warn("Unknown field: '@' for '@'", field, actualType.getName());
            }
        }
    }

    void assignValue(Object object, String field, FieldMetadata metadata, Prov getter, Cons setter, Object value, boolean modify) throws Exception{
        Object prevValue = getter.get();

        if(value instanceof JsonValue jsv){ //setting values from object
            if(prevValue == null){
                if(modify) modified(object, field, null, metadata);
                setter.get(json.readValue(metadata.field.getType(), metadata.elementType, jsv));
            }else{
                //assign each field manually
                var childFields = json.getFields(prevValue.getClass().isAnonymousClass() ? prevValue.getClass().getSuperclass() : prevValue.getClass());
                for(var child : jsv){
                    if(child.name != null){
                        assign(prevValue, child.name, child, childFields.get(child.name), object, field);
                    }
                }
            }
        }else{
            //direct value is set
            if(modify) modified(object, field, prevValue, metadata);

            setter.get(value);
        }
    }

    /**
     * 0: the object
     * 1: the field metadata for the object to use with deserializing collection types
     * */
    Object[] resolve(Object object, String field, Object value, @Nullable FieldMetadata metadata) throws Exception{
        if(object == null) return null;

        if(object == root){
            ContentType ctype = nameToType.get(field);
            if(ctype == null){
                warn("Invalid content type: " + field);
                return null;
            }
            return new Object[]{Vars.content.getNamesBy(ctype), new FieldMetadata(null, MappableContent.class, String.class)};
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

                return new Object[]{fdata.field.get(object), fdata};
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

    void modified(Object target, String field, Object value, FieldMetadata data){
        if(!applied) return;

        //TODO
        var record = new PatchRecord(target, field, value, data);
        if(usedpatches.add(record)){
            patches.add(record);
        }
    }

    Object convertKeyType(String string, Class<?> type){
        return json.fromJson(type, string);
    }

    //TODO crash?
    void warn(String error, Object... fmt){
        Log.warn(error, fmt);
    }

    void reset(Runnable run){
        resetters.add(run);
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

    private static class PatchRecord{
        Object target;
        String field;
        Object value;
        FieldMetadata data;

        PatchRecord(Object target, String field, Object value, FieldMetadata data){
            this.target = target;
            this.field = field;
            this.value = value;
            this.data = data;
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
