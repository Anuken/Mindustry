package mindustry.mod.patch;

import arc.util.*;
import arc.util.serialization.*;
import mindustry.world.blocks.*;

/** A node with children stored in slots addressed by a key of type K. */
public abstract class KeyedNode<K> extends PatchNode{

    public KeyedNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
    }

    /** @return the key parsed from a name, or null (after warning) if it is invalid. */
    protected abstract @Nullable K key(String name);

    protected abstract PatchType childType(K key);

    protected abstract @Nullable Object get(K key);

    /** Writes a value to a slot, recording how to revert it. */
    protected abstract void put(K key, @Nullable Object value);

    @Override
    public @Nullable PatchNode child(String name){
        K key = key(name);
        if(key == null) return null;

        Object value = get(key);
        if(value == null){
            warn("Failed to resolve '@': value is null.", join(name));
            return null;
        }
        return childNode(name, value, childType(key));
    }

    @Override
    public void set(String name, Jval json){
        K key = key(name);
        if(key != null) set(name, key, json);
    }

    /** Merges JSON objects into existing values; anything else replaces the value. */
    protected void set(String name, K key, Jval json){
        PatchType type = childType(key);
        Object prev = get(key);

        //objects with an explicit type are always re-created; attributes are always replaced so that custom attributes can be declared
        if(prev != null && json.isObject() && !json.has("type") && type.type != Attributes.class){
            childNode(name, prev, type).assignAll(json);
        }else{
            put(key, context.read(this, type, json));
        }
    }

    @Override
    public void put(String name, @Nullable Object value){
        K key = key(name);
        if(key != null) put(key, value);
    }
}
