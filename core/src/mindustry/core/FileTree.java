package mindustry.core;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.assets.loaders.MusicLoader.*;
import arc.assets.loaders.SoundLoader.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import mindustry.*;

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

    public Sound loadSound(String soundName){
        if(Vars.headless) return new Sound();

        String name = "sounds/" + soundName;
        String path = Vars.tree.get(name + ".ogg").exists() ? name + ".ogg" : name + ".mp3";

        var sound = new Sound();
        AssetDescriptor<?> desc = Core.assets.load(path, Sound.class, new SoundParameter(sound));
        desc.errored = Throwable::printStackTrace;

        return sound;
    }

    public Music loadMusic(String soundName){
        if(Vars.headless) return new Music();

        String name = "music/" + soundName;
        String path = Vars.tree.get(name + ".ogg").exists() ? name + ".ogg" : name + ".mp3";

        var music = new Music();
        AssetDescriptor<?> desc = Core.assets.load(path, Music.class, new MusicParameter(music));
        desc.errored = Throwable::printStackTrace;

        return music;
    }
}
