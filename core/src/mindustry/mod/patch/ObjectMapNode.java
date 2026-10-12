package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;

@SuppressWarnings("unchecked")
public class ObjectMapNode extends MapNode{
    final ObjectMap<Object, Object> map;

    public ObjectMapNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
        map = (ObjectMap<Object, Object>)value;
    }

    void snapshot(){
        context.record(map, null, () -> {
            ObjectMap<Object, Object> copy = map.copy();
            return () -> map.set(copy);
        });
    }

    @Override
    protected @Nullable Class<?> keyType(){
        return type.keyType;
    }

    @Override
    protected PatchType childType(Object key){
        return PatchType.of(type.elementType);
    }

    @Override
    protected @Nullable Object get(Object key){
        return map.get(key);
    }

    @Override
    protected void put(Object key, @Nullable Object value){
        snapshot();
        map.put(key, value);
    }

    @Override
    protected void remove(Object key){
        snapshot();
        map.remove(key);
    }
}
