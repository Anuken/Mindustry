package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;

@SuppressWarnings("unchecked")
public class SeqNode extends ListNode{
    final Seq<Object> seq;

    public SeqNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
        seq = (Seq<Object>)value;
    }

    void snapshot(){
        context.record(seq, null, () -> {
            Seq<Object> copy = seq.copy();
            return () -> seq.set(copy);
        });
    }

    @Override
    protected int size(){
        return seq.size;
    }

    @Override
    protected PatchType elementType(){
        return PatchType.of(type.elementType);
    }

    @Override
    protected void append(Seq<Object> values){
        snapshot();
        seq.addAll(values);
    }

    @Override
    protected @Nullable Object get(Integer key){
        return seq.get(key);
    }

    @Override
    protected void put(Integer key, @Nullable Object value){
        snapshot();
        seq.set(key, value);
    }
}
