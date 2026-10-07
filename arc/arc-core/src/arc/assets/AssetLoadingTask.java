package arc.assets;

import arc.assets.loaders.AssetLoader;
import arc.assets.loaders.AsynchronousAssetLoader;
import arc.assets.loaders.SynchronousAssetLoader;
import arc.struct.Seq;
import arc.files.Fi;
import arc.util.*;

import java.util.concurrent.*;

/**
 * Responsible for loading an asset through an {@link AssetLoader} based on an {@link AssetDescriptor}.
 * @author mzechner
 */
@SuppressWarnings("unchecked")
class AssetLoadingTask implements Callable<Void>{
    final AssetDescriptor assetDesc;
    final AssetLoader loader;
    final ExecutorService executor;
    final long startTime;
    AssetManager manager;
    volatile boolean asyncDone = false;
    volatile boolean dependenciesLoaded = false;
    volatile Seq<AssetDescriptor> dependencies;
    volatile Future<Void> depsFuture = null;
    volatile Future<Void> loadFuture = null;
    volatile Object asset = null;

    int ticks = 0;
    volatile boolean cancel = false;

    public AssetLoadingTask(AssetManager manager, AssetDescriptor assetDesc, AssetLoader loader, ExecutorService threadPool){
        this.manager = manager;
        this.assetDesc = assetDesc;
        this.loader = loader;
        this.executor = threadPool;
        startTime = Time.nanos();
    }

    /** Loads parts of the asset asynchronously if the loader is an {@link AsynchronousAssetLoader}. */
    @Override
    public Void call(){
        AsynchronousAssetLoader asyncLoader = (AsynchronousAssetLoader)loader;
        if(!dependenciesLoaded){
            dependencies = asyncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
            if(dependencies != null){
                removeDuplicates(dependencies);
                manager.injectDependencies(assetDesc.fileName, dependencies);
            }else{
                // if we have no dependencies, we load the async part of the task immediately.
                asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
                asyncDone = true;
            }
        }else{
            asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
        }
        return null;
    }

    /**
     * Updates the loading of the asset. In case the asset is loaded with an {@link AsynchronousAssetLoader}, the loaders
     * {@link AsynchronousAssetLoader#loadAsync(AssetManager, String, Fi, AssetLoaderParameters)} method is first called on
     * a worker thread. Once this method returns, the rest of the asset is loaded on the rendering thread via
     * {@link AsynchronousAssetLoader#loadSync(AssetManager, String, Fi, AssetLoaderParameters)}.
     * @return true in case the asset was fully loaded, false otherwise
     */
    public boolean update(){
        ticks++;
        if(loader instanceof SynchronousAssetLoader){
            handleSyncLoader();
        }else{
            handleAsyncLoader();
        }
        return asset != null;
    }

    private void handleSyncLoader(){
        SynchronousAssetLoader syncLoader = (SynchronousAssetLoader)loader;
        if(!dependenciesLoaded){
            dependenciesLoaded = true;
            dependencies = syncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
            if(dependencies == null){
                asset = syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
                return;
            }
            removeDuplicates(dependencies);
            manager.injectDependencies(assetDesc.fileName, dependencies);
        }else{
            asset = syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
        }
    }

    private void handleAsyncLoader(){
        AsynchronousAssetLoader asyncLoader = (AsynchronousAssetLoader)loader;
        if(!dependenciesLoaded){
            if(depsFuture == null){
                depsFuture = executor.submit(this);
            }else{
                if(depsFuture.isDone()){
                    try{
                        depsFuture.get();
                    }catch(Exception e){
                        throw new ArcRuntimeException("Couldn't load dependencies of asset: " + assetDesc.fileName, e);
                    }
                    dependenciesLoaded = true;
                    if(asyncDone){
                        asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
                    }
                }
            }
        }else{
            if(loadFuture == null && !asyncDone){
                loadFuture = executor.submit(this);
            }else{
                if(asyncDone){
                    asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
                }else if(loadFuture.isDone()){
                    try{
                        loadFuture.get();
                    }catch(Exception e){
                        throw new ArcRuntimeException("Couldn't load asset: " + assetDesc.fileName, e);
                    }
                    asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
                }
            }
        }
    }

    private Fi resolve(AssetLoader loader, AssetDescriptor assetDesc){
        if(assetDesc.file == null) assetDesc.file = loader.resolve(assetDesc.fileName);
        return assetDesc.file;
    }

    public Object getAsset(){
        return asset;
    }

    private void removeDuplicates(Seq<AssetDescriptor> array){
        boolean ordered = array.ordered;
        array.ordered = true;
        for(int i = 0; i < array.size; ++i){
            final String fn = array.get(i).fileName;
            final Class type = array.get(i).type;
            for(int j = array.size - 1; j > i; --j){
                if(type == array.get(j).type && fn.equals(array.get(j).fileName))
                    array.remove(j);
            }
        }
        array.ordered = ordered;
    }
}
