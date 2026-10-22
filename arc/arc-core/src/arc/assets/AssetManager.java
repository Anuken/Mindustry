package arc.assets;

import arc.*;
import arc.assets.loaders.*;
import arc.audio.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.font.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;

import java.util.concurrent.*;

/**
 * Loads and stores assets like textures, bitmapfonts, tile maps, sounds, music and so on.
 * @author mzechner
 */
@SuppressWarnings("unchecked")
public class AssetManager implements Disposable{
    final ObjectMap<Class, ObjectMap<String, RefCountedContainer>> assets = new ObjectMap<>();
    final ObjectMap<String, Class> assetTypes = new ObjectMap<>();
    final ObjectMap<String, Seq<String>> assetDependencies = new ObjectMap<>();
    final ObjectSet<String> injected = new ObjectSet<>();

    final ObjectMap<Class, ObjectMap<String, AssetLoader>> loaders = new ObjectMap<>();
    final Seq<AssetDescriptor> loadQueue = new Seq<>();
    final ExecutorService executor;

    final Seq<AssetLoadingTask> tasks = new Seq<>();
    final FileHandleResolver resolver;
    AssetErrorListener listener = null;
    int loaded = 0;
    int toLoad = 0;
    int peakTasks = 0;

    /** Creates a new AssetManager with all default loaders. */
    public AssetManager(){
        this(Core.files::internal);
    }

    /** Creates a new AssetManager with all default loaders. */
    public AssetManager(FileHandleResolver resolver){
        this(resolver, true);
    }

    /**
     * Creates a new AssetManager with optionally all default loaders. If you don't add the default loaders then you do have to
     * manually add the loaders you need, including any loaders they might depend on.
     * @param defaultLoaders whether to add the default loaders
     */
    public AssetManager(FileHandleResolver resolver, boolean defaultLoaders){
        this.resolver = resolver;
        if(defaultLoaders){
            setLoader(Font.class, new FontLoader(resolver));
            setLoader(Music.class, new MusicLoader(resolver));
            setLoader(Pixmap.class, new PixmapLoader(resolver));
            setLoader(Sound.class, new SoundLoader(resolver));
            setLoader(TextureAtlas.class, new TextureAtlasLoader(resolver));
            setLoader(Texture.class, new TextureLoader(resolver));
            setLoader(I18NBundle.class, new I18NBundleLoader(resolver));
            setLoader(Shader.class, new ShaderProgramLoader(resolver));
            setLoader(Cubemap.class, new CubemapLoader(resolver));
        }
        executor = Threads.executor("Assets", 1);
    }

    /**
     * Returns the {@link FileHandleResolver} for which this AssetManager was loaded with.
     * @return the file handle resolver which this AssetManager uses
     */
    public FileHandleResolver getFileHandleResolver(){
        return resolver;
    }

    /**
     * @param fileName the asset file name
     * @return the asset
     */
    public synchronized <T> T get(String fileName){
        Class<T> type = assetTypes.get(fileName);
        if(type == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        T asset = (T)assetContainer.object;
        if(asset == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        return asset;
    }

    /**
     * @param fileName the asset file name
     * @param type the asset type
     * @return the asset
     */
    public synchronized <T> T get(String fileName, Class<T> type){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        T asset = (T)assetContainer.object;
        if(asset == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        return asset;
    }

    /**
     * @param fileName the asset file name
     * @param type the asset type
     * @return the asset, or null if not found
     */
    public synchronized <T> T getOrNull(String fileName, Class<T> type){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) return null;
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) return null;
        return (T)assetContainer.object;
    }

    /**
     * @param type the asset type
     * @return all the assets matching the specified type
     */
    public synchronized <T> Seq<T> getAll(Class<T> type, Seq<T> out){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType != null){
            for(ObjectMap.Entry<String, RefCountedContainer> asset : assetsByType.entries()){
                out.add((T)asset.value.object);
            }
        }
        return out;
    }

    /**
     * @param type the asset type
     * @return all the assets matching the specified type as entries by file name
     */
    public synchronized <T> Seq<Entry<String, T>> getAllEntries(Class<T> type, Seq<Entry<String, T>> out){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType != null){
            for(ObjectMap.Entry<String, RefCountedContainer> asset : assetsByType.entries()){
                Entry<String, T> entry = new Entry<>();
                entry.key = asset.key;
                entry.value = (T)asset.value.object;
                out.add(entry);
            }
        }
        return out;
    }

    /**
     * @param assetDescriptor the asset descriptor
     * @return the asset
     */
    public synchronized <T> T get(AssetDescriptor<T> assetDescriptor){
        return get(assetDescriptor.fileName, assetDescriptor.type);
    }

    /** Returns true if an asset with the specified name is loading, queued to be loaded, or has been loaded. */
    public synchronized boolean contains(String fileName){
        if(tasks.size > 0 && tasks.first().assetDesc.fileName.equals(fileName)) return true;

        for(int i = 0; i < loadQueue.size; i++)
            if(loadQueue.get(i).fileName.equals(fileName)) return true;

        return isLoaded(fileName);
    }

    /** Returns true if an asset with the specified name and type is loading, queued to be loaded, or has been loaded. */
    public synchronized boolean contains(String fileName, Class type){
        if(tasks.size > 0){
            AssetDescriptor assetDesc = tasks.first().assetDesc;
            if(assetDesc.type == type && assetDesc.fileName.equals(fileName)) return true;
        }

        for(int i = 0; i < loadQueue.size; i++){
            AssetDescriptor assetDesc = loadQueue.get(i);
            if(assetDesc.type == type && assetDesc.fileName.equals(fileName)) return true;
        }

        return isLoaded(fileName, type);
    }

    /**
     * Removes the asset and all its dependencies, if they are not used by other assets.
     * @param fileName the file name
     */
    public synchronized void unload(String fileName){
        // check if it's currently processed (and the first element in the stack, thus not a dependency) and cancel if necessary
        if(tasks.size > 0){
            AssetLoadingTask currAsset = tasks.first();
            if(currAsset.assetDesc.fileName.equals(fileName)){
                currAsset.cancel = true;
                return;
            }
        }

        // check if it's in the queue
        int foundIndex = -1;
        for(int i = 0; i < loadQueue.size; i++){
            if(loadQueue.get(i).fileName.equals(fileName)){
                foundIndex = i;
                break;
            }
        }
        if(foundIndex != -1){
            toLoad--;
            loadQueue.remove(foundIndex);
            return;
        }

        // get the asset and its type
        Class type = assetTypes.get(fileName);
        if(type == null) return;

        RefCountedContainer assetRef = assets.get(type).get(fileName);

        if(assetRef == null) return;

        // if it is reference counted, decrement ref count and check if we can really get rid of it.
        assetRef.count--;
        if(assetRef.count <= 0){

            // if it is disposable dispose it
            if(assetRef.object instanceof Disposable)
                ((Disposable)assetRef.object).dispose();

            // remove the asset from the manager.
            assetTypes.remove(fileName);
            assets.get(type).remove(fileName);
        }

        // remove any dependencies (or just decrement their ref count).
        Seq<String> dependencies = assetDependencies.get(fileName);
        if(dependencies != null){
            for(String dependency : dependencies){
                if(isLoaded(dependency)) unload(dependency);
            }
        }
        // remove dependencies if ref count < 0
        if(assetRef.count <= 0){
            assetDependencies.remove(fileName);
        }
    }

    /**
     * @param asset the asset
     * @return whether the asset is contained in this manager
     */
    public synchronized <T> boolean containsAsset(T asset){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(asset.getClass());
        if(assetsByType == null) return false;
        for(String fileName : assetsByType.keys()){
            T otherAsset = (T)assetsByType.get(fileName).object;
            if(otherAsset == asset || asset.equals(otherAsset)) return true;
        }
        return false;
    }

    /**
     * @param asset the asset
     * @return the filename of the asset or null
     */
    public synchronized <T> String getAssetFileName(T asset){
        for(Class assetType : assets.keys()){
            ObjectMap<String, RefCountedContainer> assetsByType = assets.get(assetType);
            for(String fileName : assetsByType.keys()){
                T otherAsset = (T)assetsByType.get(fileName).object;
                if(otherAsset == asset || asset.equals(otherAsset)) return fileName;
            }
        }
        return null;
    }

    /**
     * @param assetDesc the AssetDescriptor of the asset
     * @return whether the asset is loaded
     */
    public synchronized boolean isLoaded(AssetDescriptor assetDesc){
        return isLoaded(assetDesc.fileName);
    }

    /**
     * @param fileName the file name of the asset
     * @return whether the asset is loaded
     */
    public synchronized boolean isLoaded(String fileName){
        if(fileName == null) return false;
        return assetTypes.containsKey(fileName);
    }

    /**
     * @param fileName the file name of the asset
     * @return whether the asset is loaded
     */
    public synchronized boolean isLoaded(String fileName, Class type){
        ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
        if(assetsByType == null) return false;
        RefCountedContainer assetContainer = assetsByType.get(fileName);
        if(assetContainer == null) return false;
        return assetContainer.object != null;
    }

    /**
     * Returns the default loader for the given type
     * @param type The type of the loader to get
     * @return The loader capable of loading the type, or null if none exists
     */
    public <T> AssetLoader getLoader(final Class<T> type){
        return getLoader(type, null);
    }

    /**
     * Returns the loader for the given type and the specified filename. If no loader exists for the specific filename, the
     * default loader for that type is returned.
     * @param type The type of the loader to get
     * @param fileName The filename of the asset to get a loader for, or null to get the default loader
     * @return The loader capable of loading the type and filename, or null if none exists
     */
    public <T> AssetLoader getLoader(final Class<T> type, final String fileName){
        final ObjectMap<String, AssetLoader> loaders = this.loaders.get(type);
        if(loaders == null || loaders.size < 1) return null;
        if(fileName == null) return loaders.get("");
        AssetLoader result = null;
        int l = -1;
        for(ObjectMap.Entry<String, AssetLoader> entry : loaders.entries()){
            if(entry.key.length() > l && fileName.endsWith(entry.key)){
                result = entry.value;
                l = entry.key.length();
            }
        }
        return result;
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     * @param fileName the file name (interpretation depends on {@link AssetLoader})
     * @param type the type of the asset.
     */
    public synchronized <T> AssetDescriptor<T> load(String fileName, Class<T> type){
        return load(fileName, type, null);
    }

    /**
     * Loads a custom one-time 'asset' that knows how to load itself.
     */
    public synchronized AssetDescriptor loadRun(String name, Class<?> type, Runnable loadasync){
        return loadRun(name, type, loadasync, () -> {});
    }

    /**
     * Loads a custom one-time 'asset' that knows how to load itself.
     */
    public synchronized AssetDescriptor loadRun(String name, Class<?> type, Runnable loadasync, Runnable loadsync){
        if(getLoader(type) == null){
            setLoader(type, new CustomLoader(){
                @Override
                public void loadAsync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    loadasync.run();
                }

                @Override
                public Object loadSync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    loadsync.run();
                    return super.loadSync(manager, fileName, file, parameter);
                }
            });
        }else{
            throw new IllegalArgumentException("Class already registered or loaded: " + type);
        }
        return load(name, type, null);
    }

    /**
     * Loads a custom one-time 'asset' that knows how to load itself.
     * @param load the asset
     */
    public synchronized AssetDescriptor load(Loadable load){
        if(getLoader(load.getClass()) == null){
            setLoader(load.getClass(), new AsynchronousAssetLoader(Core.files::internal){
                @Override
                public void loadAsync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    load.loadAsync();
                }

                @Override
                public Object loadSync(AssetManager manager, String fileName, Fi file, AssetLoaderParameters parameter){
                    load.loadSync();
                    return load;
                }

                @Override
                public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, AssetLoaderParameters parameter){
                    return load.getDependencies();
                }
            });
        }
        return load(load.getName(), load.getClass(), null);
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     * @param fileName the file name (interpretation depends on {@link AssetLoader})
     * @param type the type of the asset.
     * @param parameter parameters for the AssetLoader.
     */
    public synchronized <T> AssetDescriptor<T> load(String fileName, Class<T> type, AssetLoaderParameters<T> parameter){
        AssetLoader loader = getLoader(type, fileName);
        if(loader == null) throw new ArcRuntimeException("No loader for type: " + type.getSimpleName());

        // reset stats
        if(loadQueue.size == 0){
            loaded = 0;
            toLoad = 0;
            peakTasks = 0;
        }

        // check if an asset with the same name but a different type has already been added.

        // check preload queue
        for(int i = 0; i < loadQueue.size; i++){
            AssetDescriptor desc = loadQueue.get(i);
            if(desc.fileName.equals(fileName) && !desc.type.equals(type)) throw new ArcRuntimeException(
            "Asset with name '" + fileName + "' already in preload queue, but has different type (expected: "
            + type.getSimpleName() + ", found: " + desc.type.getSimpleName() + ")");
        }

        // check task list
        for(int i = 0; i < tasks.size; i++){
            AssetDescriptor desc = tasks.get(i).assetDesc;
            if(desc.fileName.equals(fileName) && !desc.type.equals(type)) throw new ArcRuntimeException(
            "Asset with name '" + fileName + "' already in task list, but has different type (expected: "
            + type.getSimpleName() + ", found: " + desc.type.getSimpleName() + ")");
        }

        // check loaded assets
        Class otherType = assetTypes.get(fileName);
        if(otherType != null && !otherType.equals(type))
            throw new ArcRuntimeException("Asset with name '" + fileName + "' already loaded, but has different type (expected: "
            + type.getSimpleName() + ", found: " + otherType.getSimpleName() + ")");

        toLoad++;
        AssetDescriptor assetDesc = new AssetDescriptor<>(fileName, type, parameter);
        loadQueue.add(assetDesc);
        return assetDesc;
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     * @param desc the {@link AssetDescriptor}
     */
    public synchronized <T> AssetDescriptor<T> load(AssetDescriptor<T> desc){
        return load(desc.fileName, desc.type, desc.params);
    }

    /**
     * Updates the AssetManager, keeping it loading any assets in the preload queue.
     * @return true if all loading is finished.
     */
    public synchronized boolean update(){
        try{
            if(tasks.size == 0){
                // loop until we have a new task ready to be processed
                while(loadQueue.size != 0 && tasks.size == 0){
                    nextTask();
                }
                // have we not found a task? We are done!
                if(tasks.size == 0) return true;
            }
            return updateTask() && loadQueue.size == 0 && tasks.size == 0;
        }catch(Throwable t){
            handleTaskError(t);
            return loadQueue.size == 0;
        }
    }

    /** @return the asset loading task that is currently being processed.
     * May return null if nothing is being loaded. */
    public synchronized AssetDescriptor getCurrentLoading(){
        if(tasks.size > 0){
            return tasks.first().assetDesc;
        }
        return null;
    }

    /**
     * Updates the AssetManager continuously for the specified number of milliseconds, yielding the CPU to the loading thread
     * between updates. This may block for less time if all loading tasks are complete. This may block for more time if the portion
     * of a single task that happens in the GL thread takes a long time.
     * @return true if all loading is finished.
     */
    public boolean update(int millis){
        long endTime = Time.millis() + millis;
        while(true){
            boolean done = update();
            if(done || Time.millis() > endTime) return done;
            Thread.yield();
        }
    }

    /** Returns true when all assets are loaded. Can be called from any thread. */
    public synchronized boolean isFinished(){
        return loadQueue.size == 0 && tasks.size == 0;
    }

    /** Blocks until all assets are loaded. */
    public void finishLoading(){
        while(!update())
            Thread.yield();
    }

    /**
     * Blocks until the specified asset is loaded.
     * @param assetDesc the AssetDescriptor of the asset
     */
    public void finishLoadingAsset(AssetDescriptor assetDesc){
        finishLoadingAsset(assetDesc.fileName);
    }

    /**
     * Blocks until the specified asset is loaded.
     * @param fileName the file name (interpretation depends on {@link AssetLoader})
     */
    public void finishLoadingAsset(String fileName){
        while(!isLoaded(fileName)){
            update();
            Thread.yield();
        }
    }

    synchronized void injectDependencies(String parentAssetFilename, Seq<AssetDescriptor> dependendAssetDescs){
        ObjectSet<String> injected = this.injected;
        for(AssetDescriptor desc : dependendAssetDescs){
            if(injected.contains(desc.fileName)) continue; // Ignore subsequent dependencies if there are duplicates.
            injected.add(desc.fileName);
            injectDependency(parentAssetFilename, desc);
        }
        injected.clear();
    }

    private synchronized void injectDependency(String parentAssetFilename, AssetDescriptor dependendAssetDesc){
        // add the asset as a dependency of the parent asset
        Seq<String> dependencies = assetDependencies.get(parentAssetFilename);
        if(dependencies == null){
            dependencies = new Seq();
            assetDependencies.put(parentAssetFilename, dependencies);
        }
        dependencies.add(dependendAssetDesc.fileName);

        // if the asset is already loaded, increase its reference count.
        if(isLoaded(dependendAssetDesc.fileName)){
            Class type = assetTypes.get(dependendAssetDesc.fileName);
            RefCountedContainer assetRef = assets.get(type).get(dependendAssetDesc.fileName);
            assetRef.count++;
            incrementRefCountedDependencies(dependendAssetDesc.fileName);
        }
        // else add a new task for the asset.
        else{
            addTask(dependendAssetDesc);
        }
    }

    /**
     * Removes a task from the loadQueue and adds it to the task stack. If the asset is already loaded (which can happen if it was
     * a dependency of a previously loaded asset) its reference count will be increased.
     */
    private void nextTask(){
        AssetDescriptor assetDesc = loadQueue.remove(0);
        //Log.info("Loading asset task: {0}", assetDesc.fileName);

        // if the asset not meant to be reloaded and is already loaded, increase its reference count
        if(isLoaded(assetDesc.fileName)){
            Class type = assetTypes.get(assetDesc.fileName);
            RefCountedContainer assetRef = assets.get(type).get(assetDesc.fileName);
            assetRef.count++;
            incrementRefCountedDependencies(assetDesc.fileName);
            if(assetDesc.params != null && assetDesc.params.loadedCallback != null){
                assetDesc.params.loadedCallback.finishedLoading(this, assetDesc.fileName, assetDesc.type);
            }
            loaded++;
        }else{
            // else add a new task for the asset.
            addTask(assetDesc);
        }
    }

    /**
     * Adds a {@link AssetLoadingTask} to the task stack for the given asset.
     */
    private void addTask(AssetDescriptor assetDesc){
        AssetLoader loader = getLoader(assetDesc.type, assetDesc.fileName);
        if(loader == null)
            throw new ArcRuntimeException("No loader for type: " + assetDesc.type.getSimpleName());
        tasks.add(new AssetLoadingTask(this, assetDesc, loader, executor));
        peakTasks++;
    }

    /** Adds an asset to this AssetManager */
    public <T> void addAsset(final String fileName, Class<T> type, T asset){
        // add the asset to the filename lookup
        assetTypes.put(fileName, type);

        // add the asset to the type lookup
        ObjectMap<String, RefCountedContainer> typeToAssets = assets.get(type);
        if(typeToAssets == null){
            typeToAssets = new ObjectMap<>();
            assets.put(type, typeToAssets);
        }
        typeToAssets.put(fileName, new RefCountedContainer(asset));
    }

    /**
     * Updates the current task on the top of the task stack.
     * @return true if the asset is loaded or the task was cancelled.
     */
    private boolean updateTask(){
        AssetLoadingTask task = tasks.peek();

        boolean complete = true;
        try{
            complete = task.cancel || task.update();
        }catch(RuntimeException ex){
            task.cancel = true;
            taskFailed(task.assetDesc, ex);
        }

        // if the task has been cancelled or has finished loading
        if(complete){
            // increase the number of loaded assets and pop the task from the stack
            if(tasks.size == 1){
                loaded++;
                peakTasks = 0;
            }
            tasks.pop();

            if(task.cancel) return true;

            addAsset(task.assetDesc.fileName, task.assetDesc.type, task.getAsset());

            // otherwise, if a listener was found in the parameter invoke it
            if(task.assetDesc.params != null && task.assetDesc.params.loadedCallback != null){
                task.assetDesc.params.loadedCallback.finishedLoading(this, task.assetDesc.fileName, task.assetDesc.type);
            }

            task.assetDesc.loaded.get(task.getAsset());

            return true;
        }
        return false;
    }

    /**
     * Called when a task throws an exception during loading. The default implementation rethrows the exception. A subclass may
     * supress the default implementation when loading assets where loading failure is recoverable.
     */
    protected void taskFailed(AssetDescriptor assetDesc, RuntimeException ex){
        throw ex;
    }

    private void incrementRefCountedDependencies(String parent){
        Seq<String> dependencies = assetDependencies.get(parent);
        if(dependencies == null) return;

        for(String dependency : dependencies){
            Class type = assetTypes.get(dependency);
            RefCountedContainer assetRef = assets.get(type).get(dependency);
            assetRef.count++;
            incrementRefCountedDependencies(dependency);
        }
    }

    /**
     * Handles a runtime/loading error in {@link #update()} by optionally invoking the {@link AssetErrorListener}.
     */
    private void handleTaskError(Throwable t){
        if(tasks.isEmpty()) throw new ArcRuntimeException(t);

        // pop the faulty task from the stack
        AssetLoadingTask task = tasks.pop();
        AssetDescriptor assetDesc = task.assetDesc;

        // remove all dependencies
        if(task.dependenciesLoaded && task.dependencies != null){
            for(AssetDescriptor desc : task.dependencies){
                unload(desc.fileName);
            }
        }

        // clear the rest of the stack
        tasks.clear();

        // inform the listener that something bad happened
        if(listener != null){
            listener.error(assetDesc, t);
        }

        if(assetDesc.errored != null){
            assetDesc.errored.get(t);
        }else{
            throw new ArcRuntimeException(t);
        }
    }

    /**
     * Sets a new {@link AssetLoader} for the given type.
     * @param type the type of the asset
     * @param loader the loader
     */
    public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader(Class<T> type, AssetLoader<T, P> loader){
        setLoader(type, null, loader);
    }

    /**
     * Sets a new {@link AssetLoader} for the given type.
     * @param type the type of the asset
     * @param suffix the suffix the filename must have for this loader to be used or null to specify the default loader.
     * @param loader the loader
     */
    public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader(Class<T> type, String suffix,
                                                                               AssetLoader<T, P> loader){
        if(type == null) throw new IllegalArgumentException("type cannot be null.");
        if(loader == null) throw new IllegalArgumentException("loader cannot be null.");
        ObjectMap<String, AssetLoader> loaders = this.loaders.get(type);
        if(loaders == null) this.loaders.put(type, loaders = new ObjectMap<>());
        loaders.put(suffix == null ? "" : suffix, loader);
    }

    /** @return the number of loaded assets */
    public synchronized int getLoadedAssets(){
        return assetTypes.size;
    }

    /** @return the number of currently queued assets */
    public synchronized int getQueuedAssets(){
        return loadQueue.size + tasks.size;
    }

    /** @return the progress in percent of completion. */
    public synchronized float getProgress(){
        if(toLoad == 0) return 1;
        float fractionalLoaded = (float)loaded;
        if(peakTasks > 0){
            fractionalLoaded += ((peakTasks - tasks.size) / (float)peakTasks);
        }
        return Math.min(1, fractionalLoaded / (float)toLoad);
    }

    /**
     * Sets an {@link AssetErrorListener} to be invoked in case loading an asset failed.
     * @param listener the listener or null
     */
    public synchronized void setErrorListener(AssetErrorListener listener){
        this.listener = listener;
    }

    /** Disposes all assets in the manager and stops all asynchronous loading. */
    @Override
    public synchronized void dispose(){
        clear();
        Threads.await(executor);
    }

    /** Clears and disposes all assets and the preloading queue. */
    public synchronized void clear(){
        loadQueue.clear();
        while(!update());

        ObjectIntMap<String> dependencyCount = new ObjectIntMap<>();
        while(assetTypes.size > 0){
            // for each asset, figure out how often it was referenced
            dependencyCount.clear();
            Seq<String> assets = assetTypes.keys().toSeq();
            for(String asset : assets){
                dependencyCount.put(asset, 0);
            }

            for(String asset : assets){
                Seq<String> dependencies = assetDependencies.get(asset);
                if(dependencies == null) continue;
                for(String dependency : dependencies){
                    int count = dependencyCount.get(dependency, 0);
                    count++;
                    dependencyCount.put(dependency, count);
                }
            }

            // only dispose of assets that are root assets (not referenced)
            for(String asset : assets){
                if(dependencyCount.get(asset, 0) == 0){
                    unload(asset);
                }
            }
        }

        this.assets.clear();
        this.assetTypes.clear();
        this.assetDependencies.clear();
        this.loaded = 0;
        this.toLoad = 0;
        this.peakTasks = 0;
        this.loadQueue.clear();
        this.tasks.clear();
    }

    /**
     * Returns the reference count of an asset.
     */
    public synchronized int getReferenceCount(String fileName){
        Class type = assetTypes.get(fileName);
        if(type == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        return assets.get(type).get(fileName).count;
    }

    /**
     * Sets the reference count of an asset.
     */
    public synchronized void setReferenceCount(String fileName, int refCount){
        Class type = assetTypes.get(fileName);
        if(type == null) throw new ArcRuntimeException("Asset not loaded: " + fileName);
        assets.get(type).get(fileName).count = refCount;
    }

    /** @return a string containing ref count and dependency information for all assets. */
    public synchronized String getDiagnostics(){
        StringBuilder sb = new StringBuilder();
        for(String fileName : assetTypes.keys()){
            sb.append(fileName);
            sb.append(", ");

            Class type = assetTypes.get(fileName);
            RefCountedContainer assetRef = assets.get(type).get(fileName);
            Seq<String> dependencies = assetDependencies.get(fileName);

            sb.append(type.getSimpleName());

            sb.append(", refs: ");
            sb.append(assetRef.count);

            if(dependencies != null){
                sb.append(", deps: [");
                for(String dep : dependencies){
                    sb.append(dep);
                    sb.append(",");
                }
                sb.append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** @return the file names of all loaded assets. */
    public synchronized Seq<String> getAssetNames(){
        return assetTypes.keys().toSeq();
    }

    /** @return the dependencies of an asset or null if the asset has no dependencies. */
    public synchronized Seq<String> getDependencies(String fileName){
        return assetDependencies.get(fileName);
    }

    /** @return the type of a loaded asset. */
    public synchronized Class getAssetType(String fileName){
        return assetTypes.get(fileName);
    }

    static class RefCountedContainer{
        Object object;
        int count = 1;

        public RefCountedContainer(Object object){
            if(object == null) throw new IllegalArgumentException("Object must not be null");
            this.object = object;
        }
    }
}
