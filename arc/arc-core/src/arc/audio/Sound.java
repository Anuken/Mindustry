package arc.audio;

import arc.*;
import arc.files.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;

import static arc.audio.Soloud.*;

/**
 * <p>
 * A Sound is a short audio clip that can be played numerous times in parallel. It's completely loaded into memory, so only load
 * small audio files.
 * </p>
 *
 * <p>
 * Sound instances are created via a call to {@link Audio#newSound(Fi)}.
 * </p>
 *
 * <p>
 * Calling the {@link #play()} or {@link #play(float)} method will return a long which is an id to that instance of the sound. You
 * can use this id to modify the playback of that sound instance.
 * </p>
 *
 */
public class Sound extends AudioSource{
    public AudioBus bus = Core.audio == null ? null : Core.audio.soundBus;

    public @Nullable Fi file;

    float falloffOffset = 0f;
    long minInterval = 16;
    long lastTimePlayed;
    int lastVoice;
    float lastVolume;
    boolean stream;

    boolean lazyLoad = false;
    volatile boolean currentlyLoading;

    /** Creates music from an external file without copying it. */
    public static Sound createStream(Fi file){
        Sound sound = new Sound();
        try{
            sound.file = file;
            sound.stream = true;
            sound.handle = streamLoadFile(file.path());
        }catch(Throwable e){
            Log.err("Failed loading sound from " + file, e);
        }
        return sound;
    }

    /** Creates a sound that only loads itself once played. */
    public static Sound createLazy(Fi file){
        Sound sound = new Sound();
        sound.loadLazy(file);
        return sound;
    }

    /** Creates an empty sound. This sound cannot be played until it is loaded. */
    public Sound(){

    }

    /** Loads a sound from a file. */
    public Sound(Fi file){
        load(file);
    }

    public void load(byte[] data, boolean stream){
        this.stream = stream;
        handle = stream ? streamLoadBytes(data, data.length) : wavLoadBytes(data, data.length);

        if(Core.audio != null && Core.audio.defaultSoundMaxConcurrent > 0){
            setMaxConcurrent(Core.audio.defaultSoundMaxConcurrent);
        }
    }

    public void load(Fi file){
        this.file = file;
        load(file.readBytes(), false);
    }

    public void loadLazy(Fi file){
        this.file = file;
        this.lazyLoad = true;
    }

    @Override
    public boolean isLazy(){
        return lazyLoad || currentlyLoading;
    }

    public void checkLazyLoad(Runnable complete){
        if(handle == 0 && lazyLoad && !currentlyLoading && file != null){
            currentlyLoading = true;
            Core.executor.submit(() -> {
                try{
                    //make sure it doesn't attempt lazy loading again
                    lazyLoad = false;
                    load(file);
                    setParamsAfterLoad();
                    currentlyLoading = false;

                    complete.run();
                }catch(Throwable err){
                    currentlyLoading = false;
                    Log.err("Error loading sound: " + file, err);
                }
            });
        }
    }

    /**
     * Plays the sound. If the sound is already playing, it will be played again, concurrently.
     * @param volume the volume in the range [0,1]
     * @param pitch the pitch multiplier, 1 == default, >1 == faster, <1 == slower, the value has to be between 0.5 and 2.0
     * @param pan panning in the range -1 (full left) to 1 (full right). 0 is center position.
     * @param checkFrame if true, this sound will not be able to be played twice in the same frame.
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int play(float volume, float pitch, float pan, boolean loop, boolean checkFrame, AudioBus bus){
        if(!Core.audio.initialized || currentlyLoading) return -1;

        if(handle == 0 && lazyLoad){
            float fvolume = volume, fpitch = pitch, fpan = pan;
            checkLazyLoad(() -> {
                if(!loop){
                    play(fvolume, fpitch, fpan, loop, checkFrame, bus);
                }
            });
        }

        if(handle == 0 || bus == null) return -1;

        if((checkFrame && Time.timeSinceMillis(lastTimePlayed) <= minInterval)){
            //when a sound was already played this frame, intensify the volume of the last played voice instead of playing a new one
            if(volume > lastVolume){
                Core.audio.set(lastVoice, pan, lastVolume = Math.max(lastVolume, Math.min(lastVolume + volume, volume * 1.25f)));
            }

            return -1;
        }

        if(Float.isInfinite(volume) || Float.isNaN(volume)) volume = 0f;
        if(Float.isInfinite(pan) || Float.isNaN(pan)) pan = 0f;
        if(Float.isInfinite(pitch) || Float.isNaN(pitch)) pitch = 1f;

        lastVolume = volume;
        lastTimePlayed = Time.millis();

        return lastVoice = sourcePlayBus(handle, bus.handle, volume, Mathf.clamp(pitch * Core.audio.globalPitch, 0.0001f, 10f), Mathf.clamp(pan, -1f, 1f), loop);
    }

    public int play(float volume, float pitch, float pan, boolean loop, boolean checkFrame){
        return play(volume, pitch, pan, loop, checkFrame, this.bus);
    }

    /** Sets the bus that will be used for the next play of this SFX. */
    public void setBus(AudioBus bus){
        this.bus = bus;
    }

    public float calcPan(float x, float y){
        if(Core.app.isHeadless()) return 0f;

        return Mathf.clamp((x - Core.camera.position.x) / (Core.camera.width / 2f), -0.9f, 0.9f);
    }

    public float calcVolume(float x, float y){
        return calcFalloff(x, y) * Core.audio.sfxVolume;
    }

    public float calcFalloff(float x, float y){
        if(Core.app.isHeadless()) return 1f;

        float dst2 = Math.max(Mathf.dst2(x, y, Core.camera.position.x, Core.camera.position.y) - falloffOffset*falloffOffset, 0f);
        return Mathf.clamp(1f / (dst2 / Core.audio.falloff));
    }

    /**
     * Plays this sound at a certain position, with correct panning and volume applied.
     * Automatically uses the "sfxvolume" setting.
     */
    public int at(float x, float y, float pitch){
        return at(x, y, pitch, 1f);
    }

    /**
     * Plays this sound at a certain position, with correct panning and volume applied.
     * Automatically uses the "sfxvolume" setting.
     */
    public int at(float x, float y, float pitch, float volume, boolean checkFrame){
        float vol = calcVolume(x, y) * volume;
        if(vol < 0.005f) return -1; //discard
        return play(vol, pitch, calcPan(x, y), false, checkFrame);
    }

    /**
     * Plays this sound at a certain position, with correct panning and volume applied.
     * Automatically uses the "sfxvolume" setting.
     */
    public int at(float x, float y, float pitch, float volume, AudioBus bus){
        float vol = calcVolume(x, y) * volume;
        if(vol < 0.005f) return -1; //discard
        return play(vol, pitch, calcPan(x, y), false, true, bus);
    }

    /**
     * Plays this sound at a certain position, with correct panning and volume applied.
     * Automatically uses the "sfxvolume" setting.
     */
    public int at(Position pos, float pitch, float volume){
        return at(pos.getX(), pos.getY(), pitch, volume);
    }

    /**
     * Plays this sound at a certain position, with correct panning and volume applied.
     * Automatically uses the "sfxvolume" setting.
     */
    public int at(float x, float y, float pitch, float volume){
        return at(x, y, pitch, volume, true);
    }

    /**
     * Plays this sound at a certain position, with correct panning and volume applied.
     * Automatically uses the "sfxvolume" setting.
     */
    public int at(float x, float y){
        return at(x, y, 1f);
    }

    /** Plays #at() with this position. */
    public int at(Position pos){
        return at(pos.getX(), pos.getY());
    }

    /** Plays #at() with this position. */
    public int at(Position pos, float pitch){
        return at(pos.getX(), pos.getY(), pitch);
    }

    /**
     * Plays the sound. If the sound is already playing, it will be played again, concurrently.
     * Automatically uses the "sfxvolume" setting.
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int play(){
        return play(Core.audio.sfxVolume);
    }


    /**
     * Plays the sound. If the sound is already playing, it will be played again, concurrently.
     * Automatically uses the "sfxvolume" setting.
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int play(AudioBus bus){
        return play(Core.audio.sfxVolume, 1f, 0f, false, true, bus);
    }

    /**
     * Plays the sound. If the sound is already playing, it will be played again, concurrently.
     * Ignores SFX volume setting.
     * @param volume the volume in the range [0,1]
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int play(float volume){
        return play(volume, 1f, 0f);
    }

    public int play(float volume, float pitch, float pan, boolean loop){
        return play(volume, pitch, pan, loop, true);
    }

    /**
     * Plays the sound. If the sound is already playing, it will be played again, concurrently.
     * @param volume the volume in the range [0,1]
     * @param pitch the pitch multiplier, 1 == default, >1 == faster, <1 == slower, the value has to be between 0.5 and 2.0
     * @param pan panning in the range -1 (full left) to 1 (full right). 0 is center position.
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int play(float volume, float pitch, float pan){
        return play(volume, pitch, pan, false);
    }

    /**
     * Plays the sound, looping. If the sound is already playing, it will be played again, concurrently.
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int loop(){
        return loop(1f);
    }

    /**
     * Plays the sound, looping. If the sound is already playing, it will be played again, concurrently.
     * @param volume the volume in the range [0, 1]
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int loop(float volume){
        return loop(volume, 1f, 0f);
    }

    /**
     * Plays the sound, looping. If the sound is already playing, it will be played again, concurrently.
     * @param volume the volume in the range [0,1]
     * @param pitch the pitch multiplier, 1 == default, >1 == faster, <1 == slower, the value has to be between 0.5 and 2.0
     * @param pan panning in the range -1 (full left) to 1 (full right). 0 is center position.
     * @return the id of the sound instance if successful, or -1 on failure.
     */
    public int loop(float volume, float pitch, float pan){
        return play(volume, pitch, pan, true);
    }

    /** @return length in seconds */
    @Override
    public float getLength(){
        if(handle == 0 || !Core.audio.initialized) return 0f;
        return stream ? (float)Soloud.streamLength(handle) : (float)Soloud.wavLength(handle);
    }

    /** Sets the minimum interval for playbacks of this sound, in milliseconds. Additional playback within this interval will not play a new sound instance. */
    public void setMinInterval(long interval){
        minInterval = interval;
    }

    public void setFalloffOffset(float falloffOffset){
        this.falloffOffset = falloffOffset;
    }

    @Override
    public String toString(){
        return "Sound: " + (file == null ? "(uninitialized)" : file);
    }
}
