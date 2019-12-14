package io.anuke.mindustry.mod;

import io.anuke.arc.files.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.mod.Mods.*;

public class ModContentInfo{
    /** The mod that loaded this piece of content. */
    public @Nullable LoadedMod mod;
    /** File that this content was loaded from. */
    public FileHandle sourceFile;
    /** The error that occurred during loading, if applicable. Null if no error occurred. */
    public @Nullable String error;
}
