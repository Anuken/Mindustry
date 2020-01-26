package mindustry.ctype;

import arc.files.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.mod.Mods.*;


/** Base class for a content type that is loaded in {@link mindustry.core.ContentLoader}. */
public abstract class Content implements Comparable<Content>{
    public final short id;
    /** Info on which mod this content was loaded from. */
    public @NonNull ModContentInfo minfo = new ModContentInfo();


    public Content(){
        this.id = (short) Vars.content.getBy(getContentType()).size;
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

    /** @return whether an error ocurred during mod loading. */
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
