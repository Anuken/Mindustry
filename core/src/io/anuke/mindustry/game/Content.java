package io.anuke.mindustry.game;

import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.mod.Mods.*;
import io.anuke.mindustry.type.*;


/** Base class for a content type that is loaded in {@link io.anuke.mindustry.core.ContentLoader}. */
public abstract class Content implements Comparable<Content>{
    public final short id;
    /** The mod that loaded this piece of content. */
    public @Nullable LoadedMod mod;

    public Content(){
        this.id = (short)Vars.content.getBy(getContentType()).size;
        Vars.content.handleContent(this);
    }

    /**
     * Returns the type name of this piece of content.
     * This should return the same value for all instances of this content type.
     */
    public abstract ContentType getContentType();

    /** Called after all content and modules are created. Do not use to load regions or texture data! */
    public void init(){
    }

    /**
     * Called after all content is created, only on non-headless versions.
     * Use for loading regions or other image data.
     */
    public void load(){
    }

    @Override
    public int compareTo(Content c){
        return Integer.compare(id, c.id);
    }

    @Override
    public String toString(){
        return getContentType().name() + "#" + id;
    }
}
