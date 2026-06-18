package mindustry.mod;

import arc.files.*;
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
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.mod.Mods.*;
import mindustry.mod.data.*;
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
    public static final int maxImageSize = 2000;
    public static final int patchFormatVersion = 2;

    private static boolean needsArrayFix = false;
    private static final Object root = new Object();
    private static final ObjectMap<String, ContentType> nameToType = new ObjectMap<>();
    private static DataPatcher currentDataPatcher;
    private static ContentParser parser = createParser();
    private static  ModMeta dpModMeta = new ModMeta(){{
        name = internalName = "dp";
    }};
    private static LoadedMod dpMod = new LoadedMod(new Fi("dp"), new Fi(""), null, null, dpModMeta);

    private boolean applied;
    private ContentLoader contentLoader;
    private ObjectSet<Object> usedpatches = new ObjectSet<>();
    private Seq<Runnable> resetters = new Seq<>();
    private Seq<Runnable> afterCallbacks = new Seq<>();
    private Seq<Object> visitStack = new Seq<>();
    private Seq<Content> addedContent = new Seq<>();
    private @Nullable PatchAsset currentlyApplyingPatch;
    private @Nullable ContentAsset currentlyApplyingContent;
    private Seq<LVar> addedVars = new Seq<>();

    static{
        for(var type : ContentType.all){
            if(type.name().indexOf('_') == -1) nameToType.put(type.toString().toLowerCase(Locale.ROOT), type);
        }
    }

    static ContentParser createParser(){
        ContentParser cont = new ContentParser(){
            @Override
            void warnContext(@Nullable Content currentContent, @Nullable Fi currentFile, String string, Object... format){
                //forward warnings to the current patcher - this is a bit hacky, but I do not want to re-initialize the parser every time
                if(currentDataPatcher!= null){
                    currentDataPatcher.warnContext(currentContent, currentFile, string, format);
                }
            }
        };
        cont.allowClassResolution = false;
        cont.allowAssetLoading = false;
        cont.allowPatching = false;

        return cont;
    }

    public boolean isPatched(Object object){
        return usedpatches.contains(object);
    }

    /** Applies the specified patches. If patches were already applied, the previous ones are un-applied - they do not stack! */
    public void apply(Seq<PatchAsset> patches, Seq<ContentAsset> content){
        apply(patches, content, true);
    }

    /** Applies the specified patches. If patches were already applied, the previous ones are un-applied - they do not stack! */
    public void apply(Seq<PatchAsset> patches, Seq<ContentAsset> content, boolean reloadContentWorld){
        //if you're un-applying data patches, and it throws an error, just crash. this is not recoverable.
        if(applied){
            unapply();
            applied = false;
        }

        if(patches.isEmpty() && content.isEmpty()) return;

        currentDataPatcher = this;
        applied = true;
        contentLoader = Vars.content.copy();

        Attribute[] oldAttributes = Attribute.all.clone();
        var oldAttributeMap = Attribute.map.copy();
        reset(() -> {
            Attribute.all = oldAttributes;
            Attribute.map = oldAttributeMap;
        });

        //patches are read first.
        for(var set : patches){
            set.warnings.clear();
            set.error = false;

            try{
                Object someValue = parser.getJson().fromJson(null, Jval.read(set.patch).toString(Jformat.plain));
                if(!(someValue instanceof JsonValue value)) throw new SerializationException("Patch must be a JSON object.");

                if(Vars.state.rules.planet != null && value.has("requiredPlanets")){
                    JsonValue req = value.get("requiredPlanets");
                    value.remove("requiredPlanets");

                    //this should be ignored unless this instance is a dedicated server
                    if(Vars.headless){
                        String[] planets = req.isArray() ? req.asStringArray() : new String[]{req.asString()};
                        if(!Structs.contains(planets, Vars.state.rules.planet.name)){
                            continue;
                        }
                    }
                }

                set.json = value;
                currentlyApplyingPatch = set;
                visitStack.clear();

                set.name = value.getString("name", "");
                value.remove("name"); //patchsets can have a name, ignore it if present
                for(var child : value){
                    assign(root, child.name, child, null, null, null);
                }
                currentlyApplyingPatch = null;

            }catch(Exception e){
                set.error = true;
                set.name = "";
                set.warnings.add(Strings.getSimpleMessage(e));
                currentlyApplyingPatch = null;

                Log.err("Failed to apply patch: " + set.patch, e);
            }
        }

        if(!content.isEmpty()){
            content.sort();

            dpMod.erroredContent.clear();

            for(var asset : content){
                asset.errored = false;
                asset.content = null;
                asset.warnings.clear();

                currentlyApplyingContent = asset;

                if(!Structs.contains(ContentAsset.loadableContent, asset.type)){
                    warn("Content @ is of type '@', which is not supported. Skipping.", asset.path, asset.type);
                    continue;
                }

                Content current = Vars.content.getLastAdded();
                Fi file = new Fi(asset.path);

                //this is very important for resizing various arrays used in the game
                if((asset.type == ContentType.item || asset.type == ContentType.liquid)){
                    needsArrayFix = true;
                }

                try{
                    //this binds the content but does not load it entirely
                    asset.content = parser.parse(dpMod, asset.name, asset.data, file, asset.type);
                    asset.content.minfo.asset = asset;
                }catch(Throwable e){
                    asset.warnings.add(Strings.getFinalMessage(e));
                    asset.errored = true;

                    var lastAdded = Vars.content.getLastAdded();
                    if(current != lastAdded && lastAdded != null){
                        Vars.content.remove(lastAdded);
                        //markError should log it already
                        parser.markError(lastAdded, dpMod, file, e);
                    }else{
                        Log.err("Error loading content: " + asset.path, e);
                    }
                }
            }

            currentlyApplyingContent = null;

            parser.finishParsing();

            for(var errored : dpMod.erroredContent){
                if(errored.minfo.error != null && errored.minfo.asset != null){
                    errored.minfo.asset.warnings.add(errored.minfo.error);
                }
                Vars.content.remove(errored);
            }

            addedContent.clear();
            Seq<Content> all = addedContent;

            for(var arr : Vars.content.getContentMap()){
                all.addAll(arr.select(c -> c.minfo.mod == dpMod));
            }

            for(var cont : all){
                try{
                    cont.init();
                }catch(Throwable t){
                    Vars.content.remove(cont);
                    if(cont.minfo.asset != null) cont.minfo.asset.errored = true;
                    parser.markError(cont, dpMod, cont.minfo.sourceFile, t);
                }
            }

            for(var cont : all){
                try{
                    cont.postInit();
                }catch(Throwable t){
                    Vars.content.remove(cont);
                    if(cont.minfo.asset != null) cont.minfo.asset.errored = true;
                    parser.markError(cont, dpMod, cont.minfo.sourceFile, t);
                }
            }

            //register global variables
            for(var cont : all){
                if(!cont.hasErrored() && cont instanceof UnlockableContent u && Vars.logicVars.get("@" + u.name) == null){
                    addedVars.add(Vars.logicVars.put("@" + u.name, u, false));
                }
            }

            if(!Vars.headless){
                for(var cont : all){
                    try{
                        cont.loadIcon();
                        cont.load();
                        if(cont.minfo.asset != null && cont instanceof UnlockableContent u){
                            if(!u.uiIcon.found() && u.getContentType() != ContentType.planet && u.getContentType() != ContentType.weather){
                                cont.minfo.asset.warnings.add("[" + u.name.substring(u.minfo.mod.name.length() + 1) + "] Could not find an icon. Ensure that you have an image named '" + u.name + "' loaded. Remember that imported images always have the 'dp-' prefix automatically applied.");
                            }
                        }
                    }catch(Throwable t){
                        //not removed here, as this code is only called clientside, and removing it would cause a desync
                        if(cont.minfo.asset != null) cont.minfo.asset.errored = true;
                        parser.markError(cont, dpMod, cont.minfo.sourceFile, t);
                    }
                }
            }

            if(reloadContentWorld) fixContentArrays();
        }

        afterCallbacks.each(Runnable::run);
    }

    public void unapply(){
        unapply(true);
    }

    public void unapply(boolean reloadContentWorld){
        if(!applied) return;

        callContentRemove();
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
        for(var lvar : addedVars){
            Vars.logicVars.remove(lvar);
        }

        //this should never throw an exception
        afterCallbacks.each(Runnable::run);
        afterCallbacks.clear();
        usedpatches.clear();
        addedContent.clear();

        if(reloadContentWorld) fixContentArrays();
    }

    public Seq<Content> getAddedContent(){
        return addedContent;
    }

    void callContentRemove(){
        for(var arr : Vars.content.getContentMap()){
            for(var value : arr){
                if(value.isModded() && value.minfo.mod == dpMod){
                    value.removed = true;
                    value.removeContent();
                }
            }
        }
    }

    public static void fixContentArrays(){
        if(!needsArrayFix) return;
        int items = Vars.content.items().size, liquids = Vars.content.liquids().size;

        //block item/liquid filter
        for(var block : Vars.content.blocks()){
            //don't waste time resizing arrays for blocks that can't use them
            if(!block.synthetic()) continue;

            block.checkContentArrayCapacity(items, liquids);
        }

        //resize capacities in the world (editor). this SHOULD be the only time when fixing arrays is necessary
        if(!Vars.headless && Vars.ui != null && Vars.ui.editor != null && Vars.ui.editor.isShown()){
            int wh = Vars.world.width() * Vars.world.height();
            for(int i = 0; i < wh; i++){
                var b = Vars.world.tiles.geti(i).build;
                if(b != null && b.items != null) b.items.checkArrayCapacity(items);
                if(b != null && b.liquids != null) b.liquids.checkArrayCapacity(items);
            }
        }

        //TODO: this doesn't do anything about extensive ItemSeq usage across the codebase, which is limited to the campaign
        //TODO: this also doesn't change sectors
        needsArrayFix = false;
    }

    void visit(Object object){
        visitStack.add(object);
        if(object instanceof Content c && usedpatches.add(c)){
            after(c::afterPatch);
        }
    }

    void created(Object object){
        if(object instanceof Weapon weapon){
            weapon.init();
        }else if(object instanceof Content cont){
            cont.init();
            cont.postInit();
        }

        if(!Vars.headless){
            Object parent = null;
            //find last item on the stack that can be mapped to this part or weapon
            for(int i = visitStack.size - 1; i >= 0; i --){
                Object o = visitStack.items[i];
                if(o != object && (o instanceof Content || o instanceof Weapon)){
                    parent = o;
                    break;
                }
            }
            if(object instanceof DrawPart part && parent instanceof MappableContent cont){
                part.load(cont.name);
            }else if(object instanceof DrawPart part && parent instanceof Weapon w){
                part.load(w.name);
            }else if(object instanceof DrawBlock draw && parent instanceof Block block){
                draw.load(block);
            }else if(object instanceof Weapon weapon){
                weapon.load();
            }else if(object instanceof Content cont){
                cont.load();
            }
        }
    }

    void assign(Object object, String field, Object value, @Nullable FieldData metadata, @Nullable Object parentObject, @Nullable String parentField) throws Exception{
        if(field == null || field.isEmpty()) return;

        int oldLength = visitStack.size;
        try{

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
                        assign(root, field + "." + child.name, child, null, null, null);
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
                        modifiedField(parentObject, parentField, s.copy());

                        assignValue(object, field, metadata, () -> s.get(i), val -> s.set(i, val), value, true);
                    }else{
                        modifiedField(parentObject, parentField, copyArray(object));

                        var fobj = object;
                        assignValue(object, field, metadata, () -> Array.get(fobj, i), val -> Array.set(fobj, i, val), value, true);
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

                if(value instanceof JsonValue jsv && object instanceof UnitType && field.equals("controller")){
                    var fmeta = fields.get("controller");
                    assignValue(object, "controller", new FieldData(fmeta), () -> Reflect.get(fobj, fmeta.field), val -> Reflect.set(fobj, fmeta.field, val), (Func<Unit, UnitController>)(u -> parser.resolveController(jsv.asString()).get()), true);
                }else if(value instanceof JsonValue jsv && object instanceof UnitType && field.equals("aiController")){
                    var fmeta = fields.get("aiController");
                    assignValue(object, "aiController", new FieldData(fmeta), () -> Reflect.get(fobj, fmeta.field), val -> Reflect.set(fobj, fmeta.field, val), parser.resolveController(jsv.asString()), true);
                }else if(fdata != null){
                    if(checkField(fdata.field)) return;

                    assignValue(object, field, new FieldData(fdata), () -> Reflect.get(fobj, fdata.field), fv -> {
                        if(fv == null && !fdata.field.isAnnotationPresent(Nullable.class) && !(Vars.headless && ContentParser.implicitNullable.contains(fdata.field.getType()))){
                            warn("Field '@' cannot be null.", fdata.field);
                            return;
                        }
                        Reflect.set(fobj, fdata.field, fv);
                    }, value, true);
                }else if(value instanceof JsonValue jsv && object instanceof Block bl && jsv.isObject() && field.equals("consumes")){
                    Seq<Consume> prevBuilder = Reflect.<Seq<Consume>>get(Block.class, bl, "consumeBuilder").copy();
                    boolean hadItems = bl.hasItems, hadLiquids = bl.hasLiquids, hadPower = bl.hasPower, acceptedItems = bl.acceptsItems;
                    Runnable resetCons = () -> {
                        Reflect.set(Block.class, bl, "consumeBuilder", prevBuilder);
                        bl.reinitializeConsumers();
                        bl.hasItems = hadItems;
                        bl.hasLiquids = hadLiquids;
                        bl.hasPower = hadPower;
                        bl.acceptsItems = acceptedItems;
                    };
                    reset(resetCons);

                    try{
                        bl.hasPower = false; //if a block doesn't have a power consumer, hasPower should be false. if it does, it will get set to true in reinitializeConsumers
                        parser.readBlockConsumers(bl, jsv);
                        bl.reinitializeConsumers();
                    }catch(Throwable e){
                        resetCons.run();
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
        }finally{
            visitStack.truncate(oldLength);
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
                    parser.listeners.add((type, jsonData, result) -> created(result));
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

            Object prev = object instanceof Seq s ? s.get(i) : Array.get(object, i);
            reset(() -> {
                if(object instanceof Seq seq){
                    seq.set(i, prev);
                }else{
                    Array.set(object, i, prev);
                }
            });

            return new Object[]{prev, metadata != null ? new FieldData(object instanceof Seq<?> ? metadata.elementType : metadata.type.getComponentType(), null, null) : null};
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
                        record.field.set(record.target, value);
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                });
            }
        }else if(target instanceof Seq<?> || target.getClass().isArray()){
            int i = Integer.parseInt(field);
            resetters.add(() -> {

                if(target instanceof Seq seq){
                    seq.set(i, value);
                }else{
                    Array.set(target, i, value);
                }
            });
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
        warnContext(null, null, error, fmt);
    }

    void warnContext(@Nullable Content currentContent, @Nullable Fi currentFile, String error, Object... fmt){
        String formatted = Strings.format(error, fmt);

        if(currentlyApplyingPatch != null){
            currentlyApplyingPatch.warnings.add(formatted);
        }else if(currentlyApplyingContent != null && (currentlyApplyingContent.content == null || currentlyApplyingContent.content.minfo.asset == null)){
            currentlyApplyingContent.warnings.add(formatted);
        }else if(currentContent != null && currentContent.minfo.asset != null){
            currentContent.minfo.asset.warnings.add(formatted);
        }

        Log.warn("[ContentPatcher] " + formatted);
    }

    void after(Runnable run){
        afterCallbacks.add(run);
    }

    static Object copyArray(Object object){
        if(object instanceof int[] i) return i.clone();
        else if(object instanceof long[] i) return i.clone();
        else if(object instanceof short[] i) return i.clone();
        else if(object instanceof byte[] i) return i.clone();
        else if(object instanceof boolean[] i) return i.clone();
        else if(object instanceof char[] i) return i.clone();
        else if(object instanceof float[] i) return i.clone();
        else if(object instanceof double[] i) return i.clone();
        else return ((Object[])object).clone();
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
