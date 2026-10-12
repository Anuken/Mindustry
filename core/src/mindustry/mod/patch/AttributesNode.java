package mindustry.mod.patch;

import arc.util.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

public class AttributesNode extends KeyedNode<Attribute>{
    final Attributes attributes;

    public AttributesNode(PatchContext context, @Nullable PatchNode parent, String name, Object value, PatchType type){
        super(context, parent, name, value, type);
        attributes = (Attributes)value;
    }

    @Override
    protected @Nullable Attribute key(String name){
        Attribute attribute = Attribute.getOrNull(name);
        if(attribute == null){
            warn("Unknown attribute: '@'", name);
        }
        return attribute;
    }

    @Override
    protected PatchType childType(Attribute key){
        return PatchType.of(float.class);
    }

    @Override
    protected @Nullable Object get(Attribute key){
        return attributes.get(key);
    }

    @Override
    protected void put(Attribute key, @Nullable Object value){
        context.record(attributes, key, () -> {
            float prev = attributes.get(key);
            return () -> attributes.set(key, prev);
        });
        attributes.set(key, (Float)value);
    }
}
