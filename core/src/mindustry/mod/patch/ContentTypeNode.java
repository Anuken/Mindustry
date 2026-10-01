package mindustry.mod.patch;

import arc.util.*;
import mindustry.*;
import mindustry.type.*;

/** All content of one type, addressed by name. Content can be edited, but never replaced. */
public class ContentTypeNode extends PatchNode{
    public final ContentType contentType;

    public ContentTypeNode(PatchContext context, PatchNode parent, String name, ContentType contentType){
        super(context, parent, name, contentType, PatchType.of(null));
        this.contentType = contentType;
    }

    @Override
    public @Nullable PatchNode child(String name){
        MappableContent content = Vars.content.getByName(contentType, name);
        if(content == null){
            warn("Unknown @: '@'", contentType, name);
            return null;
        }
        return childNode(name, content, PatchType.of(content.getClass()));
    }
}
