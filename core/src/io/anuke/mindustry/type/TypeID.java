package io.anuke.mindustry.type;

import io.anuke.arc.function.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.entities.traits.*;

public class TypeID extends MappableContent{
    public final Supplier<? extends TypeTrait> constructor;

    public TypeID(String name, Supplier<? extends TypeTrait> constructor){
        super(name);
        this.constructor = constructor;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.typeid;
    }
}
