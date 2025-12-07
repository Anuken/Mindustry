package mindustry.mod;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.part.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;
import java.util.*;

/** The current implementation is awful. Consider it a proof of concept. */
@SuppressWarnings("unchecked")
public class DataPatcher{
    private static final Object root = new Object();
    private static final ObjectMap<String, ContentType> nameToType = new ObjectMap<>();
    private static ContentParser parser = createParser();

    private boolean applied;
    private ContentLoader contentLoader;
    private ObjectSet<Object> usedpatches = new ObjectSet<>();
    private Seq<Runnable> resetters = new Seq<>();
    private Seq<Runnable> afterCallbacks = new Seq<>();
    private @Nullable PatchSet currentlyApplying;

    /** Currently active patches. Note that apply() should be called after modification. */
    public Seq<PatchSet> patches = new Seq<>();

    static{
        for(var type : ContentType.all){
            if(type.name().indexOf('_') == -1) nameToType.put(type.toString().toLowerCase(Locale.ROOT), type);
        }
    }

    static ContentParser createParser(){
        ContentParser cont = new ContentParser(){
            @Override
            void warn(String string, Object... format){
                //forward warnings to the current patcher - this is a bit hacky, but I do not want to re-initialize the parser every time
                if(Vars.state.patcher != null){
                    Vars.state.patcher.warn(string, format);
                }
            }
        };
        cont.allowClassResolution = false;

        return cont;
    }

    /** Applies the specified patches. If patches were already applied, the previous ones are un-applied - they do not stack! */
    public void apply(Seq<String> patchArray) throws Exception{
        if(applied){
            unapply();
            applied = false;
        }

        applied = true;
        contentLoader = Vars.content.copy();
        patches.clear();

        for(String patch : patchArray){
            PatchSet set = new PatchSet(patch, new JsonValue("error"));
            patches.add(set);

            try{
                JsonValue value = parser.getJson().fromJson(null, Jval.read(patch).toString(Jformat.plain));
                set.json = value;
                currentlyApplying = set;

                set.name = value.getString("name", "");
                value.remove("name"); //patchsets can have a name, ignore it if present
                for(var child : value){
                    assign(root, child.name, child, null, null, null);
                }
                currentlyApplying = null;

            }catch(Exception e){
                set.error = true;
                set.warnings.add(Strings.getSimpleMessage(e));
                currentlyApplying = null;

                Log.err("Failed to apply patch: " + patch, e);
            }
        }

        afterCallbacks.each(Runnable::run);
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
        usedpatches.clear();
    }

    void visit(Object object){
        if(object instanceof Content c && usedpatches.add(c)){
            after(c::afterPatch);
        }
    }

    void created(Object object, Object parent){
        if(!Vars.headless){
            if(object instanceof DrawPart part && parent instanceof MappableContent cont){
                part.load(cont.name);
            }else if(object instanceof DrawPart part && parent instanceof Weapon w){
                part.load(w.name);
            }else if(object instanceof DrawBlock draw && parent instanceof Block block){
                draw.load(block);
            }else if(object instanceof Weapon weapon){
                weapon.load();
                weapon.init();
            }else if(object instanceof Content cont){
                cont.init();
                cont.postInit();
                cont.load();
            }
        }else{
            if(object instanceof Weapon weapon){
                weapon.init();
            }
        }
    }

    void assign(Object object, String field, Object value, @Nullable FieldData metadata, @Nullable Object parentObject, @Nullable String parentField) throws Exception{
        if(field == null || field.isEmpty()) return;

        //field.field2.field3 nested syntax
        if(field.indexOf('.') != -1){
            //resolve the field chain until the final field is reached
            String[] path = field.split("\\.");
            for(int i = 0; i < path.length - 1; i++){
                parentObject = object;
                parentField = path[i];
                Object[] result = resolve(object, path[i], metadata);
                if(result == null){
                    warn("Failed to resolve @.@", object, path[i]);
                    return;
                }
                object = result[0];
                metadata = (FieldData)result[1];

                if(i < path.length - 2){
                    visit(object);
                }
            }
            field = path[path.length - 1];
        }

        visit(object);

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
        }else if(object instanceof Seq<?> || object.getClass().isArray()){

            if(field.equals("+")){
                var meta = new FieldData(metadata.type.isArray() ? metadata.type.getComponentType() : metadata.elementType, null, null);
                boolean multiAdd;

                if(value instanceof JsonValue jval && jval.isArray()){
                    meta = metadata;
                    multiAdd = true;
                }else{
                    multiAdd = false;
                }

                //handle array addition syntax
                if(object instanceof Seq s){
                    modifiedField(parentObject, parentField, s.copy());

                    assignValue(object, field, meta, () -> null, val -> {
                        if(multiAdd){
                            s.addAll((Seq)val);
                        }else{
                            s.add(val);
                        }
                    }, value, false);
                }else{
                    modifiedField(parentObject, parentField, copyArray(object));

                    var fobj = object;
                    var fpo = parentObject;
                    var fpf = parentField;
                    assignValue(parentObject, parentField, meta, () -> null, val -> {
                        try{
                            //create copy array, put the new object in the last slot, and assign the parent's field to it
                            int len = Array.getLength(fobj);
                            Object copy;

                            if(multiAdd){
                                int otherLen = Array.getLength(val);
                                copy = Array.newInstance(fobj.getClass().getComponentType(), len + otherLen);
                                System.arraycopy(val, 0, copy, len, otherLen);
                                System.arraycopy(fobj, 0, copy, 0, len);
                            }else{
                                copy = Array.newInstance(fobj.getClass().getComponentType(), len + 1);
                                Array.set(copy, len, val);
                                System.arraycopy(fobj, 0, copy, 0, len);
                            }

                            assign(fpo, fpf, copy, null, null, null);
                        }catch(Exception e){
                            throw new RuntimeException(e);
                        }
                    }, value, false);
                }
            }else{
                if(metadata != null){
                    var meta = new FieldData(metadata.type.isArray() ? metadata.type.getComponentType() : metadata.elementType, null, null);
                    if(meta.type != null){
                        metadata = meta;
                    }
                }

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
        }else if(object instanceof ObjectSet set && field.equals("+")){
            modifiedField(parentObject, parentField, set.copy());

            var meta = new FieldData(metadata.elementType, null, null);
            boolean multiAdd;

            if(value instanceof JsonValue jval && jval.isArray()){
                meta = metadata;
                multiAdd = true;
            }else{
                multiAdd = false;
            }

            assignValue(object, field, multiAdd ? meta : metadata, () -> null, val -> {
                if(multiAdd){
                    set.addAll((ObjectSet)val);
                }else{
                    set.add(val);
                }
            }, value, false);
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

            if(value instanceof JsonValue jval && jval.isString() && (jval.asString().equals("-"))){
                //removal syntax:
                //"value": "-"
                map.remove(key);
            }else{
                assignValue(object, field, new FieldData(metadata.elementType, null, null), () -> map.get(key), val -> map.put(key, val), value, false);
            }
        }else if(object instanceof ObjectFloatMap map){
            if(metadata == null){
                warn("ObjectFloatMap cannot be parsed without metadata: @.@", parentObject, parentField);
                return;
            }
            Object key = convertKeyType(field, metadata.elementType);
            if(key == null){
                warn("Null key: '@'", field);
                return;
            }

            var copy = map.copy();
            reset(() -> map.set(copy));

            if(value instanceof JsonValue jval && jval.isString() && (jval.asString().equals("-"))){
                //removal syntax:
                //"value": "-"
                map.remove(key, 0f);
            }else{
                assignValue(object, field, new FieldData(float.class, null, null), () -> map.get(key, 0f), val -> map.put(key, (Float)val), value, false);
            }
        }else if(object instanceof Attributes map && value instanceof JsonValue jval){
            Attribute key = Attribute.getOrNull(field);
            if(key == null){
                warn("Unknown attribute: '@'", field);
                return;
            }
            if(!jval.isNumber()){
                warn("Attribute value must be a number: '@'", jval);
                return;
            }
            float prev = map.get(key);
            reset(() -> map.set(key, prev));
            map.set(key, jval.asFloat());
        }else{
            Class<?> actualType = object.getClass();
            if(actualType.isAnonymousClass()) actualType = actualType.getSuperclass();

            var fields = parser.getJson().getFields(actualType);
            var fdata = fields.get(field);
            var fobj = object;
            if(fdata != null){
                if(checkField(fdata.field)) return;

                assignValue(object, field, new FieldData(fdata), () -> Reflect.get(fobj, fdata.field), fv -> {
                    if(fv == null && !fdata.field.isAnnotationPresent(Nullable.class) && !(Vars.headless && ContentParser.implicitNullable.contains(fdata.field.getType()))){
                        warn("Field '@' cannot be null.", fdata.field);
                        return;
                    }
                    Reflect.set(fobj, fdata.field, fv);
                }, value, true);
            }else if(value instanceof JsonValue jsv && object instanceof Block bl && jsv.isObject() && field.equals("consumes")){
                modifiedField(bl, "consumeBuilder", Reflect.<Seq<Consume>>get(Block.class, bl, "consumeBuilder").copy());
                modifiedField(bl, "consumers", Reflect.<Consume[]>get(Block.class, bl, "consumers"));
                boolean hadItems = bl.hasItems, hadLiquids = bl.hasLiquids, hadPower = bl.hasPower, acceptedItems = bl.acceptsItems;
                reset(() -> {
                    bl.reinitializeConsumers();
                    bl.hasItems = hadItems;
                    bl.hasLiquids = hadLiquids;
                    bl.hasPower = hadPower;
                    bl.acceptsItems = acceptedItems;
                });

                try{
                    parser.readBlockConsumers(bl, jsv);
                    bl.reinitializeConsumers();
                }catch(Throwable e){
                    Log.err(e);
                    warn("Failed to read consumers for '@': @", bl, Strings.getSimpleMessage(e));
                }
            }else if(value instanceof JsonValue jsv && object instanceof UnitType && field.equals("type")){
                var fmeta = fields.get("constructor");
                assignValue(object, "constructor", new FieldData(fmeta), () -> Reflect.get(fobj, fmeta.field), val -> Reflect.set(fobj, fmeta.field, val), parser.unitType(jsv), true);
            }else{
                warn("Unknown field '@' for class '@'", field, actualType.getSimpleName());
            }
        }
    }

    void assignValue(Object object, String field, FieldData metadata, Prov getter, Cons setter, Object value, boolean modify) throws Exception{
        Object prevValue = getter.get();

        try{
            if(value instanceof JsonValue jsv){ //setting values from object
               if(prevValue == null || !jsv.isObject() || (jsv.has("type") && metadata.type != MappableContent.class) || (metadata != null && metadata.type == Attributes.class)){
                    if(UnlockableContent.class.isAssignableFrom(metadata.type) && jsv.isObject()){
                        warn("New content must not be instantiated: @", jsv);
                        return;
                    }

                    if(modify) modifiedField(object, field, getter.get());

                    //HACK: listen for creation of objects once
                    parser.listeners.add((type, jsonData, result) -> created(result, object));
                    try{
                        setter.get(parser.getJson().readValue(metadata.type, metadata.elementType, jsv));
                    }catch(Throwable e){
                        warn("Failed to read value @.@ = @: (type = @ elementType = @)\n@", object, field, value, metadata.type, metadata.elementType, Strings.getSimpleMessages(e));
                    }
                   parser.listeners.pop();
                }else{
                    //assign each field manually
                    var childFields = parser.getJson().getFields(prevValue.getClass().isAnonymousClass() ? prevValue.getClass().getSuperclass() : prevValue.getClass());

                    for(var child : jsv){
                        if(child.name != null){
                            assign(prevValue, child.name, child,
                            metadata != null && (metadata.type == ObjectMap.class || metadata.type == ObjectFloatMap.class) ? metadata :
                            metadata != null && metadata.type == Seq.class ? new FieldData(metadata.elementType, null, null) :
                            metadata != null && metadata.type.isArray() ? new FieldData(metadata.type.getComponentType(), null, null) :
                            !childFields.containsKey(child.name) ? null :
                            new FieldData(childFields.get(child.name)), object, field);
                        }
                    }
                }
            }else{
                //direct value is set
                if(modify) modifiedField(object, field, prevValue);

                setter.get(value);
            }
        }catch(Throwable e){
            warn("Failed to assign @.@ = @: @", object, field, value, Strings.getStackTrace(e));
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

            var fields = parser.getJson().getFields(actualType);
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

        var fields = parser.getJson().getFields(target.getClass());
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
        return parser.getJson().fromJson(type, string);
    }

    void warn(String error, Object... fmt){
        String formatted = Strings.format(error, fmt);
        if(currentlyApplying != null){
            currentlyApplying.warnings.add(formatted);
        }
        Log.warn("[ContentPatcher] " + formatted);
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

    public static class PatchSet{
        public String patch;
        public JsonValue json;
        public String name = "";
        public boolean error;
        public Seq<String> warnings = new Seq<>();

        public PatchSet(String patch, JsonValue json){
            this.patch = patch;
            this.json = json;
        }
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

        @Override
        public String toString(){
            return "FieldData{" +
            "type=" + type +
            ", elementType=" + elementType +
            ", keyType=" + keyType +
            '}';
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
