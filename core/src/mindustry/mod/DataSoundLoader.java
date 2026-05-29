package mindustry.mod;

import arc.*;
import arc.audio.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.data.*;

public class DataSoundLoader{
    private static final int soundIdOffset = 100_000;
    private static final String prefix = "dp-";

    private Seq<Sound> loadedSounds = new Seq<>();
    private Seq<Music> loadedMusic = new Seq<>();

    public void load(Seq<SoundAsset> sounds, Seq<MusicAsset> musics){

        int nextSoundId = soundIdOffset + 1;

        for(var asset : sounds){
            Sound sound = Vars.headless ? new Sound() : Sound.createLazy(prefix + asset.name, asset.data);
            loadedSounds.add(sound);

            Sounds.registerSound(sound, nextSoundId ++);

            if(Vars.headless || !Core.audio.initialized()) continue;

            Core.assets.addAsset(sound.file.toString(), Sound.class, sound);
        }

        for(var asset : musics){
            Music music = Vars.headless ? new Music() : Music.createLazy(prefix + asset.name, asset.data);
            loadedMusic.add(music);

            if(Vars.headless || !Core.audio.initialized()) continue;

            Core.assets.addAsset(music.file.toString(), Music.class, music);
        }
    }

    public void unload(){
        for(var sound : loadedSounds){
            if(!Vars.headless) Core.assets.unload(sound.file.toString());
            sound.dispose();
            Sounds.unregisterSound(sound);
        }

        for(var music : loadedMusic){
            if(!Vars.headless) Core.assets.unload(music.file.toString());
            music.dispose();
        }

        loadedSounds.clear();
        loadedMusic.clear();
    }
}
