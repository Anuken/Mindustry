package io.anuke.mindustry.type;

import io.anuke.arc.func.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.entities.traits.*;

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
