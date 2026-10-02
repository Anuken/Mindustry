package arc.audio;

import arc.*;
import arc.Files.*;
import arc.files.*;
import arc.util.*;

import java.io.*;

import static arc.audio.Soloud.*;

/**
 * <p>
 * A Music instance represents a streamed audio file. The interface supports pausing, resuming
 * and so on. When you are done with using the Music instance you have to dispose it via the {@link #dispose()} method.
 * </p>
 *
 * <p>
 * Music instances are created via {@link Audio#newMusic(Fi)}.
 * </p>
 *
 * <p>
 * Music instances are automatically paused and resumed when an {@link Application} is paused or resumed. See
 * {@link ApplicationListener}.
 * </p>
 *
 * <p>
 * <b>Note</b>: any values provided will not be clamped, it is the developer's responsibility to do so
 * </p>
 * @author mzechner
 */
public class Music extends AudioSource{
    public @Nullable Fi file;

    int voice = -1;
    boolean looping;
    float volume = 1f, pitch = 1f, pan = 0f;

    /** Creates music from an external file without copying it. */
    public static Music create(Fi file){
        Music music = new Music();
        try{
            music.file = file;
            music.handle = streamLoadFile(file.path());
        }catch(Throwable e){
            Log.err("Failed loading music from " + file, e);
        }
        return music;
    }

    /** Loads music from a file. */
    public Music(Fi file) throws Exception{
        load(file);
    }

    /** Creates an empty music instance. This instance cannot be played until loaded. */
    public Music(){

    }

    public void load(byte[] bytes) throws Exception{
        handle = streamLoadBytes(bytes, bytes.length);
    }

    public void load(Fi file) throws Exception{
        this.file = file;

        //on iOS, try to load from internal storage instead, as that prevents unnecessary copies
        if(OS.isIos && file.type() == FileType.internal){
            try{
                String path = Core.files.getInternalStoragePath();
                handle = streamLoadFile((path.endsWith("/") ? path : path + "/") + file.path());
                return;
            }catch(Exception failed){}
        }

        Exception last = null;

        for(Fi result : caches(file.nameWithoutExtension() + "__" + file.length() + "." + file.extension())){
            //check if file already exists (use length as "hash")
            if(!(result.exists() && !result.isDirectory() && result.length() == file.length())){
                //save to the cached file
                file.copyTo(result);
            }

            try{
                handle = streamLoadFile(result.file().getCanonicalPath());
                return;
            }catch(Exception e){
                try{
                    handle = streamLoadFile(result.file().getAbsolutePath());
                    return;
                }catch(Exception ignored){
                }
                last = new ArcRuntimeException("Error loading music: " + result.file().getCanonicalPath(), e);
            }
        }

        if(last != null) throw last;
    }

    public void play(){
        if(handle == 0 || !Core.audio.initialized) return;

        if(idValid(voice) && idGetPause(voice)){
            pause(false);
        }else{
            voice = sourcePlayBus(handle, Core.audio.musicBus.handle, volume, pitch, pan, looping);

            idProtected(voice, true);
        }
    }

    public void pause(boolean pause){
        if(handle == 0 || voice <= 0) return;

        idPause(voice, pause);
    }

    @Override
    public void stop(){
        super.stop();
        voice = 0;
    }

    public boolean isPlaying(){
        if(handle == 0 || voice <= 0) return false;

        return idValid(voice) && !idGetPause(voice);
    }

    public boolean isLooping(){
        return looping;
    }

    public void setLooping(boolean isLooping){
        this.looping = isLooping;
        if(handle == 0 || voice <= 0) return;

        idLooping(voice, isLooping);
    }

    public float getVolume(){
        return volume;
    }

    public void setVolume(float volume){
        this.volume = volume;
        if(handle == 0 || voice <= 0) return;

        idVolume(voice, volume);
    }

    public void set(float pan, float volume){
        this.volume = volume;
        this.pan = pan;

        if(handle == 0 || voice <= 0) return;

        idVolume(voice, volume);
        idPan(voice, pan);
    }

    public float getPosition(){
        if(handle == 0) return 0;

        return idPosition(voice);
    }

    public void setPosition(float position){
        if(handle == 0 || voice <= 0) return;

        idSeek(voice, position);
    }

    /** @return length in seconds */
    @Override
    public float getLength(){
        if(handle == 0 || !Core.audio.initialized) return 0f;
        return (float)Soloud.streamLength(handle);
    }

    @Override
    public String toString(){
        return "SoloudMusic: " + file;
    }

    protected static Fi[] caches(String name) throws IOException{
        String dir = System.getProperty("java.io.tmpdir");

        //prefer cache dir on android
        if(Core.app.isAndroid()){
            return new Fi[]{
            Core.files.cache(name), Core.settings.getDataDirectory().child("cache").child(name),
            dir == null ? Core.files.absolute(File.createTempFile(name, "mind").getAbsolutePath()) : Core.files.absolute(dir).child(name)
            };
        }else{
            return new Fi[]{
            Core.settings.getDataDirectory().child("cache").child(name), Core.files.cache(name),
            dir == null ? Core.files.absolute(File.createTempFile(name, "mind").getAbsolutePath()) : Core.files.absolute(dir).child(name)
            };
        }
    }
}
