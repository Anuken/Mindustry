package mindustry.mod.patch;

import arc.util.*;
import arc.util.serialization.*;

/** A map with keys parsed from JSON. Assigning "-" removes a key. */
public abstract class MapNode extends KeyedNode<Object>{

    public MapNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
    }

    protected abstract @Nullable Class<?> keyType();

    protected abstract void remove(Object key);

    @Override
    protected @Nullable Object key(String name){
        if(keyType() == null){
            warn("Map cannot be edited without type information: '@'", path());
            return null;
        }

        Object key = context.parser.getJson().fromJson(keyType(), name);
        if(key == null){
            warn("Null key: '@'", name);
        }
        return key;
    }

    @Override
    protected void set(String name, Object key, Jval json){
        if(json.isString() && json.asString().equals("-")){
            remove(key);
        }else{
            super.set(name, key, json);
        }
    }
}
