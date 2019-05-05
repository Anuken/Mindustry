package io.anuke.mindustry.game;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.type.ContentType;


/** Base class for a content type that is loaded in {@link io.anuke.mindustry.core.ContentLoader}. */
public abstract class Content{
    public final short id;

    public Content(){
        this.id = (short)Vars.content.getBy(getContentType()).size;
        Vars.content.handleContent(this);
    }

    /**
     * Returns the type name of this piece of content.
     * This should return the same value for all instances of this content type.
     */
    public abstract ContentType getContentType();

    /** Called after all content is created. Do not use to load regions or texture data! */
    public void init(){
    }

    /**
     * Called after all content is created, only on non-headless versions.
     * Use for loading regions or other image data.
     */
    public void load(){
    }

    @Override
    public String toString(){
        return getContentType().name() + "#" + id;
    }
}
