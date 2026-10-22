package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;

/** An unordered set, which only supports adding values. */
@SuppressWarnings("unchecked")
public class SetNode extends PatchNode{
    final ObjectSet<Object> set;

    public SetNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
        set = (ObjectSet<Object>)value;
    }

    @Override
    public @Nullable PatchNode child(String name){
        warn("Sets do not support access by key; use '+' to add values: '@'", join(name));
        return null;
    }

    @Override
    public void add(Jval json){
        Seq<Object> values = readAll(PatchType.of(type.elementType), json);

        context.record(set, null, () -> {
            ObjectSet<Object> copy = set.copy();
            return () -> {
                set.clear();
                set.addAll(copy);
            };
        });
        set.addAll(values);
    }
}
