package mindustry.core;

import arc.*;
import arc.assets.loaders.*;
import arc.assets.loaders.MusicLoader.*;
import arc.assets.loaders.SoundLoader.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;

/** Handles files in a modded context. */
public class FileTree implements FileHandleResolver{
    private ObjectMap<String, Fi> files = new ObjectMap<>();
    private ObjectMap<String, Sound> loadedSounds = new ObjectMap<>();
    private ObjectMap<String, Music> loadedMusic = new ObjectMap<>();

    public void addFile(String path, Fi f){
        files.put(path.replace('\\', '/'), f);
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

    /**
     * Loads a sound by name from the sounds/ folder. OGG and MP3 are supported; the extension is automatically added to the end of the file name.
     * Results are cached; consecutive calls to this method with the same name will return the same sound instance.
     * */
    public Sound loadSound(String soundName){
        if(Vars.headless) return Sounds.none;

        return loadedSounds.get(soundName, () -> {
            String name = "sounds/" + soundName;
            String path = Vars.tree.get(name + ".ogg").exists() ? name + ".ogg" : name + ".mp3";

            var sound = new Sound();
            var desc = Core.assets.load(path, Sound.class, new SoundParameter(sound));
            desc.errored = Throwable::printStackTrace;

            return sound;
        });
    }

    /**
     * Loads a music file by name from the music/ folder. OGG and MP3 are supported; the extension is automatically added to the end of the file name.
     * Results are cached; consecutive calls to this method with the same name will return the same music instance.
     * */
    public Music loadMusic(String musicName){
        if(Vars.headless) return new Music();

        return loadedMusic.get(musicName, () -> {
            String name = "music/" + musicName;
            String path = Vars.tree.get(name + ".ogg").exists() ? name + ".ogg" : name + ".mp3";

            var music = new Music();
            var desc = Core.assets.load(path, Music.class, new MusicParameter(music));
            desc.errored = Throwable::printStackTrace;

            return music;
        });
    }
}
