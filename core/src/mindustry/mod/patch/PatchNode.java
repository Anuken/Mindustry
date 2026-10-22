package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.world.blocks.*;

/** A patchable value in the patch tree. Subclasses define how children are resolved, assigned and added. */
public abstract class PatchNode{
    public final PatchContext context;
    public final @Nullable PatchNode parent;
    /** The key of this node in its parent. */
    public final String name;
    public final PatchType type;
    public Object value;

    public PatchNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        this.context = context;
        this.parent = parent;
        this.name = name;
        this.value = value;
        this.type = type;
    }

    /** @return the child node with this key, or null if it could not be resolved. */
    public abstract @Nullable PatchNode child(String name);

    /** Assigns JSON to the child with this key. By default, only objects can be merged into existing children. */
    public void set(String name, Jval json){
        if(!json.isObject()){
            warn("'@' cannot be assigned.", join(name));
            return;
        }

        PatchNode child = child(name);
        if(child != null) child.assignAll(json);
    }

    /** Handles the '+' syntax. */
    public void add(Jval json){
        warn("'@' does not support adding values.", path());
    }

    /** Directly assigns a raw value to the child with this key. */
    public void put(String name, @Nullable Object value){
        warn("'@' cannot be assigned.", join(name));
    }

    /** Assigns JSON to a dot-separated path relative to this node. */
    public final void assign(String path, Jval json){
        if(path == null || path.isEmpty()) return;

        int dot = path.indexOf('.');
        if(dot != -1){
            PatchNode child = child(path.substring(0, dot));
            if(child != null) child.assign(path.substring(dot + 1), json);
            return;
        }

        try{
            if(path.equals("+")){
                add(json);
            }else{
                set(path, json);
            }
        }catch(Throwable e){
            warn("Failed to assign @ = @: @", join(path), json, Strings.getSimpleMessages(e));
        }
    }

    public void assignAll(Jval json){
        for(var entry : json.asObject()){
            assign(entry.key, entry.value);
        }
    }

    /** Creates the node for a child value. */
    protected PatchNode childNode(String name, Object value, PatchType type){
        context.visit(value);

        if(value instanceof Seq<?>) return new SeqNode(context, this, name, value, type);
        if(value.getClass().isArray()) return new ArrayNode(context, this, name, value, type);
        if(value instanceof ObjectSet<?>) return new SetNode(context, this, name, value, type);
        if(value instanceof ObjectMap<?, ?>) return new ObjectMapNode(context, this, name, value, type);
        if(value instanceof ObjectFloatMap<?>) return new FloatMapNode(context, this, name, value, type);
        if(value instanceof Attributes) return new AttributesNode(context, this, name, value, type);
        return new ObjectNode(context, this, name, value, type);
    }

    /** Parses either a single element, or every element of a JSON array. */
    protected Seq<Object> readAll(PatchType elementType, Jval json){
        Seq<Object> result = new Seq<>();
        if(json.isArray()){
            for(var element : json.asArray()){
                result.add(context.read(this, elementType, element));
            }
        }else{
            result.add(context.read(this, elementType, json));
        }
        return result;
    }

    public String path(){
        return parent == null ? name : parent.join(name);
    }

    protected String join(String child){
        String path = path();
        return path.isEmpty() ? child : path + "." + child;
    }

    protected void warn(String text, Object... args){
        context.warn(text, args);
    }

    @Override
    public String toString(){
        return path();
    }
}
