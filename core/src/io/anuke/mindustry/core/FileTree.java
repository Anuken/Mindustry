package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;

/** Handles files in a modded context. */
public class FileTree{
    private ObjectMap<String, FileHandle> files = new ObjectMap<>();

    public void addFile(FileHandle f){
        files.put(f.path(), f);
    }

    /** Gets an asset file.*/
    public FileHandle get(String path){
        if(files.containsKey(path)){
            return files.get(path);
        }else{
            return Core.files.internal(path);
        }
    }
}
