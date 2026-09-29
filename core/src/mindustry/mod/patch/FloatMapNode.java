package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;

@SuppressWarnings("unchecked")
public class FloatMapNode extends MapNode{
    final ObjectFloatMap<Object> map;

    public FloatMapNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
        map = (ObjectFloatMap<Object>)value;
    }

    void snapshot(){
        context.record(map, null, () -> {
            ObjectFloatMap<Object> copy = map.copy();
            return () -> map.set(copy);
        });
    }

    //ObjectFloatMap has a single generic parameter, so its key is stored as the element type
    @Override
    protected @Nullable Class<?> keyType(){
        return type.elementType;
    }

    @Override
    protected PatchType childType(Object key){
        return PatchType.of(float.class);
    }

    @Override
    protected @Nullable Object get(Object key){
        return map.get(key, 0f);
    }

    @Override
    protected void put(Object key, @Nullable Object value){
        snapshot();
        map.put(key, (Float)value);
    }

    @Override
    protected void remove(Object key){
        snapshot();
        map.remove(key, 0f);
    }
}
