package mindustry.ctype;

import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.mod.Mods.*;

/** Base class for a content type that is loaded in {@link mindustry.core.ContentLoader}. */
public abstract class Content implements Comparable<Content>{
    public short id;
    /** Info on which mod this content was loaded from. */
    public ModContentInfo minfo = new ModContentInfo();

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
    public void init(){}

    /**
     * Called after all content is created, only on non-headless versions.
     * Use for loading regions or other image data.
     */
    public void load(){}

    /** Called right after load(). */
    public void loadIcon(){}

    /**
     * Use if you save instance before ResetEvent, and pass it to game.
     * <code>
     *     var inst = Blocks.arc
     *     Events.on(xxxEvent.class,()->{
     *         tile.setBlock(inst.reGet())
     *     })
     * </code>
     * @return Instance get from Vars.content
     */
    public <T extends Content> T reGet(){
        return Vars.content.getByID(getContentType(), id);
    }

    /** @return whether an error occurred during mod loading. */
    public boolean hasErrored(){
        return minfo.error != null;
    }

    @Override
    public int compareTo(Content c){
        return Integer.compare(id, c.id);
    }

    @Override
    public String toString(){
        return getContentType().name() + "#" + id;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof Content content)) return false;
        return id == content.id && getContentType() == content.getContentType();
    }

    @Override
    public int hashCode(){
        return id | getContentType().ordinal() << 16;
    }

    public static class ModContentInfo{
        /** The mod that loaded this piece of content. */
        public @Nullable LoadedMod mod;
        /** File that this content was loaded from. */
        public @Nullable Fi sourceFile;
        /** The error that occurred during loading, if applicable. Null if no error occurred. */
        public @Nullable String error;
        /** Base throwable that caused the error. */
        public @Nullable Throwable baseError;
    }
}
