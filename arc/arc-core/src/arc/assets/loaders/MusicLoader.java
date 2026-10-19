package arc.assets.loaders;

import arc.*;
import arc.assets.*;
import arc.audio.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;

/**
 * {@link AssetLoader} for {@link Music} instances. The Music instance is loaded synchronously.
 * @author mzechner
 */
public class MusicLoader extends AsynchronousAssetLoader<Music, MusicLoader.MusicParameter>{
    private Music music;

    public MusicLoader(FileHandleResolver resolver){
        super(resolver);
    }

    /**
     * Returns the {@link Music} instance currently loaded by this
     * {@link MusicLoader}.
     * @return the currently loaded {@link Music}, otherwise {@code null} if
     * no {@link Music} has been loaded yet.
     */
    protected Music getLoadedMusic(){
        return music;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, MusicParameter parameter){
        if(parameter != null && parameter.music != null){
            try{
                (music = parameter.music).load(file);
            }catch(Exception e){
                Log.err(e);
            }
        }else{
            music = Core.audio.newMusic(file);
        }
    }

    @Override
    public Music loadSync(AssetManager manager, String fileName, Fi file, MusicParameter parameter){
        Music music = this.music;
        this.music = null;
        return music;
    }

    @Override
    public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, MusicParameter parameter){
        return null;
    }

    public static class MusicParameter extends AssetLoaderParameters<Music>{
        public @Nullable Music music;

        public MusicParameter(){
        }

        public MusicParameter(@Nullable Music music){
            this.music = music;
        }

        public MusicParameter(LoadedCallback loadedCallback){
            super(loadedCallback);
        }
    }

}
