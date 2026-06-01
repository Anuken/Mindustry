package mindustry.mod;

import arc.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.data.*;

public class DataSoundLoader{
    private static final int soundIdOffset = 100_000;
    private static final String prefix = "dp-";

    private Seq<Sound> loadedSounds = new Seq<>();
    private Seq<Music> loadedMusic = new Seq<>();
    private Seq<String> registered = new Seq<>();

    public void load(Seq<SoundAsset> sounds, Seq<MusicAsset> musics){

        int nextSoundId = soundIdOffset + 1;

        for(var asset : sounds){
            Fi file = asset.getCacheFile();
            Sound sound = Vars.headless || file == null ? new Sound() : Sound.createStream(file);
            loadedSounds.add(sound);

            Sounds.registerSound(sound, nextSoundId ++);

            if(Vars.headless || !Core.audio.initialized() || sound.file == null) continue;

            Core.assets.addAsset(prefix + asset.name, Sound.class, sound);
            registered.add(prefix + asset.name);
        }

        for(var asset : musics){
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
               Core.assets.unload(reg);
           }
        }

        loadedSounds.clear();
        loadedMusic.clear();
        registered.clear();
    }
}
