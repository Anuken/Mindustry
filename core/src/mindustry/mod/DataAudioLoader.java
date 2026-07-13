package mindustry.mod;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.data.*;

public class DataAudioLoader{
    private static final int soundIdOffset = 100_000;
    public static final String prefix = "dp-";

    private Seq<Sound> loadedSounds = new Seq<>();
    private Seq<Music> loadedMusic = new Seq<>();
    private ObjectSet<String> registered = new ObjectSet<>();

    public void load(Seq<SoundAsset> sounds, Seq<MusicAsset> musics){

        int nextSoundId = soundIdOffset + 1;

        for(var asset : sounds){
            if(registered.contains(prefix + asset.name)){
                Log.warn("Duplicate audio file: " + asset.name);
                continue;
            }

            Fi file = asset.getCacheFile();
            Sound sound = Vars.headless || file == null ? new Sound() : Sound.createStream(file);
            loadedSounds.add(sound);

            Sounds.registerSound(sound, nextSoundId ++);

            if(Vars.headless || !Core.audio.initialized() || sound.file == null) continue;

            Vars.logicVars.put("@sfx-" + prefix + asset.name, nextSoundId - 1, false);

            Core.assets.addAsset(prefix + asset.name, Sound.class, sound);
            registered.add(prefix + asset.name);
        }

        for(var asset : musics){
            if(registered.contains(prefix + asset.name)){
                Log.warn("Duplicate audio file: " + asset.name);
                continue;
            }
            Fi file = asset.getCacheFile();
            Music music = Vars.headless || file == null ? new Music() : Music.create(file);
            loadedMusic.add(music);

            if(Vars.headless || !Core.audio.initialized() || music.file == null) continue;

            Core.assets.addAsset(prefix + asset.name, Music.class, music);
            registered.add(prefix + asset.name);
        }
    }

    public void unload(){
        for(var sound : loadedSounds){
            sound.dispose();
            Sounds.unregisterSound(sound);
        }

        for(var music : loadedMusic){
            music.dispose();
        }

        if(!Vars.headless){
           for(String reg : registered){
               Vars.logicVars.remove("@sfx-" + reg);
               try{
                   Core.assets.unload(reg);
               }catch(Exception ignored){ //unloading shouldn't throw an error, but ignore it just in case
               }
           }
        }

        loadedSounds.clear();
        loadedMusic.clear();
        registered.clear();
    }
}
