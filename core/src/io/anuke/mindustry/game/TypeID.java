package io.anuke.mindustry.game;

import io.anuke.arc.function.Supplier;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.type.ContentType;

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
