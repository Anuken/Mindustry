package io.anuke.mindustry.mod;

import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.mod.Mods.*;

public class Scripts{

    public void run(LoadedMod mod, FileHandle file){
       Log.info("Skipping {0} (no scripting implenmentation)", file);
    }
}
