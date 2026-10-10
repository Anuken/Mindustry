package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;

/** An ordered collection with children addressed by index. */
public abstract class ListNode extends KeyedNode<Integer>{

    public ListNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
    }

    protected abstract int size();

    protected abstract PatchType elementType();

    protected abstract void append(Seq<Object> values);

    @Override
    public void add(Jval json){
        append(readAll(elementType(), json));
    }

    @Override
    protected @Nullable Integer key(String name){
        int i = Strings.parseInt(name);
        if(i == Integer.MIN_VALUE){
            warn("Invalid number for array access: '@'", name);
            return null;
        }else if(i < 0 || i >= size()){
            warn("Number outside of array bounds: '@' (length is @)", name, size());
            return null;
        }
        return i;
    }

    @Override
    protected PatchType childType(Integer key){
        return elementType();
    }
}
