package mindustry.mod.patch;

import arc.struct.*;
import arc.util.*;
import mindustry.type.*;

import java.util.*;

/** The root of a patch; its children are content types. */
public class RootNode extends PatchNode{
    private static final ObjectMap<String, ContentType> nameToType = new ObjectMap<>();

    static{
        for(var type : ContentType.all){
            if(type.name().indexOf('_') == -1) nameToType.put(type.toString().toLowerCase(Locale.ROOT), type);
        }
    }

    public RootNode(PatchContext context){
        super(context, null, "", null, PatchType.of(null));
    }

    @Override
    public @Nullable PatchNode child(String name){
        ContentType type = nameToType.get(name);
        if(type == null){
            warn("Invalid content type: '@'", name);
            return null;
        }
        return new ContentTypeNode(context, this, name, type);
    }
}
