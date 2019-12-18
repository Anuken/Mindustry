package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.assets.loaders.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;

/** Handles files in a modded context. */
public class FileTree implements FileHandleResolver{
    private ObjectMap<String, Fi> files = new ObjectMap<>();

    public void addFile(String path, Fi f){
        files.put(path, f);
    }

    /** Gets an asset file.*/
    public Fi get(String path){
        if(files.containsKey(path)){
            return files.get(path);
        }else if(files.containsKey("/" + path)){
            return files.get("/" + path);
        }else{
            return Core.files.internal(path);
        }
    }

    /** Clears all mod files.*/
    public void clear(){
        files.clear();
    }

    @Override
    public Fi resolve(String fileName){
        return get(fileName);
    }
}
