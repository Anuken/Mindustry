package arc.assets;

public class AssetLoaderParameters<T>{
    public LoadedCallback loadedCallback;

    public AssetLoaderParameters(){

    }

    public AssetLoaderParameters(LoadedCallback loadedCallback){
        this.loadedCallback = loadedCallback;
    }

    /**
     * Callback interface that will be invoked when the {@link AssetManager} loaded an asset.
     * @author mzechner
     */
    public interface LoadedCallback{
        void finishedLoading(AssetManager assetManager, String fileName, Class type);
    }
}
