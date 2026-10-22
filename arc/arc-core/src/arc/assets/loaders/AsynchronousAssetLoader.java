package arc.assets.loaders;

import arc.assets.AssetLoaderParameters;
import arc.assets.AssetManager;
import arc.files.Fi;

/**
 * Base class for asynchronous {@link AssetLoader} instances. Such loaders try to load parts of an OpenGL resource, like the
 * Pixmap, on a separate thread to then load the actual resource on the thread the OpenGL context is active on.
 * @author mzechner
 */
public abstract class AsynchronousAssetLoader<T, P extends AssetLoaderParameters<T>> extends AssetLoader<T, P>{

    public AsynchronousAssetLoader(FileHandleResolver resolver){
        super(resolver);
    }

    /**
     * Loads the non-OpenGL part of the asset and injects any dependencies of the asset into the AssetManager.
     * @param fileName the name of the asset to load
     * @param file the resolved file to load
     * @param parameter the parameters to use for loading the asset
     */
    public abstract void loadAsync(AssetManager manager, String fileName, Fi file, P parameter);

    /**
     * Loads the OpenGL part of the asset.
     * @param file the resolved file to load
     */
    public abstract T loadSync(AssetManager manager, String fileName, Fi file, P parameter);
}
