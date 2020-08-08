package mindustry.core;

import arc.*;
import arc.assets.loaders.*;
import arc.struct.*;
import arc.files.*;

/** Handles files in a modded context. */
public class FileTree implements FileHandleResolver{
    private ObjectMap<String, Fi> files = new ObjectMap<>();

    public void addFile(String path, Fi f){
        files.put(path, f);
    }

    /** Gets an asset file.*/
    public Fi get(String path){
        return get(path, false);
    }

    /** Gets an asset file.*/
    public Fi get(String path, boolean safe){
        if(files.containsKey(path)){
            return files.get(path);
        }else if(files.containsKey("/" + path)){
            return files.get("/" + path);
        }else if(Core.files == null && !safe){ //headless
            return Fi.get(path);
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
