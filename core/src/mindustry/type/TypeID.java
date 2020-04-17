package mindustry.type;

import arc.func.*;
import mindustry.ctype.*;
import mindustry.ctype.ContentType;
import mindustry.entities.traits.*;

public class TypeID extends MappableContent{
    public final Prov<? extends TypeTrait> constructor;

    public TypeID(String name, Prov<? extends TypeTrait> constructor){
        super(name);
        this.constructor = constructor;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.typeid;
    }
}
