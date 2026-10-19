package arc.audio;

import arc.*;
import arc.files.*;
import arc.util.*;

import static arc.audio.Soloud.*;

/** High-level wrapper for the Soloud library. */
public class Audio implements Disposable{
    public float globalPitch = 1f;
    /** Falloff when playing audio.*/
    public float falloff = 16000f;
    /** Default value for maximum instances of a sound. Must be set before a sound is loaded. */
    public int defaultSoundMaxConcurrent = 6;

    boolean initialized;

    public float sfxVolume = 0f;
    /** Global bus for all sounds. */
    public AudioBus soundBus = new AudioBus();
    /** Global bus for all music. */
    public AudioBus musicBus = new AudioBus();

    /** Initializes Soloud audio by default. May throw an exception if initialization fails. */
    public Audio(){
        initialize();
    }

    /** Conditionally initializes audio. If enabled is false, does nothing. */
    public Audio(boolean enabled){
        if(enabled){
            initialize();
        }
    }

    public boolean initialized(){
        return initialized;
    }

    /** Intializes Soloud audio. If this fails, prints an error and disables audio. */
    protected void initialize(){
        try{
            init();
            Log.info("[Audio] Initialized SoLoud @ using @ at @hz / @ samples / @ channels",
            version(), backendString(), backendSamplerate(), backendBufferSize(), backendChannels());

            initialized = true;
            soundBus = new AudioBus().init();
            musicBus = new AudioBus().init();

            Core.app.addListener(new ApplicationListener(){

                @Override
                public void update(){
                    sfxVolume = Core.settings.getInt("sfxvol") / 100f;
                }

                @Override
                public void pause(){
                    if(Core.app.isMobile()){
                        pauseAll(true);
                    }
                }

                @Override
                public void resume(){
                    if(Core.app.isMobile()){
                        pauseAll(false);
                    }
                }
            });
        }catch(Throwable error){
            Log.err("Failed to initialize audio, disabling sound", error);
        }
    }

    /** Loads a sound, logging an error and returning a dummy track upon failure. */
    public Sound newSound(Fi file){
        if(!initialized) return new Sound();
        try{
            return new Sound(file);
        }catch(Throwable t){
            Log.err("Error loading sound: " + file, t);
            return new Sound();
        }
    }

    /** Loads music, logging an error and returning a dummy track upon failure. */
    public Music newMusic(Fi file){
        if(!initialized) return new Music();
        try{
            return new Music(file);
        }catch(Throwable t){
            Log.err("Error loading music: " + file, t);
            return new Music();
        }
    }

    public boolean isPlaying(int soundId){
        if(!initialized) return false;
        return soundId > 0 && idValid(soundId);
    }

    public void protect(int voice, boolean protect){
        if(!initialized) return;
        idProtected(voice, protect);
    }

    public int play(AudioSource source, float volume, float pitch, float pan, boolean loop){
        if(!initialized || source.handle == 0) return -1;
        return sourcePlay(source.handle, volume, pitch, pan, loop);
    }

    public void stop(AudioSource source){
        if(!initialized || source.handle == 0) return;
        sourceStop(source.handle);
    }

    public void stop(int soundId){
        if(!initialized) return;
        idStop(soundId);
    }

    public void setPaused(int soundId, boolean paused){
        if(!initialized) return;
        idPause(soundId, paused);
    }

    public void setLooping(int soundId, boolean looping){
        if(!initialized) return;
        idLooping(soundId, looping);
    }

    public void setPitch(int soundId, float pitch){
        if(!initialized) return;
        if(!Float.isInfinite(pitch) && !Float.isNaN(pitch)) idPitch(soundId, Math.max(pitch, 0.001f));
    }

    public void setVolume(int soundId, float volume){
        if(!initialized) return;
        if(!Float.isInfinite(volume) && !Float.isNaN(volume)) idVolume(soundId, volume);
    }

    public void set(int soundId, float pan, float volume){
        if(!initialized) return;
        if(!Float.isInfinite(volume) && !Float.isNaN(volume)) idVolume(soundId, volume);
        if(!Float.isInfinite(pan) && !Float.isNaN(pan)) idPan(soundId, pan);
    }

    public void fadeFilterParam(int voice, int filter, int attribute, float value, float timeSec){
        if(!initialized) return;
        filterFade(voice, filter, attribute, value, timeSec);
    }

    public void setFilterParam(int voice, int filter, int attribute, float value){
        if(!initialized) return;
        filterSet(voice, filter, attribute, value);
    }

    public void setFilter(int index, @Nullable AudioFilter filter){
        if(!initialized) return;
        setGlobalFilter(index, filter == null ? 0 : filter.handle);
    }

    public int countPlaying(AudioSource source){
        if(!initialized || source.handle <= 0) return 0;
        return sourceCount(source.handle);
    }

    public int countTotalPlaying(){
        if(!initialized) return 0;
        return activeVoiceCount();
    }

    @Override
    public void dispose(){
        if(!initialized) return;
        stopAll();
        deinit();
        initialized = false;
    }

}
