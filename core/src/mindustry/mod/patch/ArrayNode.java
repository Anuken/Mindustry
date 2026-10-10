package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;

import java.lang.reflect.*;

public class ArrayNode extends ListNode{

    public ArrayNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
    }

    @Override
    protected int size(){
        return Array.getLength(value);
    }

    @Override
    protected PatchType elementType(){
        return PatchType.of(value.getClass().getComponentType());
    }

    @Override
    protected void append(Seq<Object> values){
        int len = size();
        Object copy = Array.newInstance(value.getClass().getComponentType(), len + values.size);
        System.arraycopy(value, 0, copy, 0, len);
        for(int i = 0; i < values.size; i++){
            Array.set(copy, len + i, values.get(i));
        }

        //arrays cannot be resized, so the parent's reference is replaced instead
        parent.put(name, copy);
        value = copy;
    }

    @Override
    protected @Nullable Object get(Integer key){
        return Array.get(value, key);
    }

    @Override
    protected void put(Integer key, @Nullable Object value){
        Object array = this.value;
        context.record(array, null, () -> {
            Object copy = copyArray(array);
            return () -> System.arraycopy(copy, 0, array, 0, Array.getLength(copy));
        });
        Array.set(array, key, value);
    }

    static Object copyArray(Object array){
        int len = Array.getLength(array);
        Object copy = Array.newInstance(array.getClass().getComponentType(), len);
        System.arraycopy(array, 0, copy, 0, len);
        return copy;
    }
}
