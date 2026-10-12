package arc.assets;

import arc.files.*;
import arc.func.*;
import arc.util.*;

/**
 * Describes an asset to be loaded by its filename, type and {@link AssetLoaderParameters}. Instances of this are used in
 * {@link AssetLoadingTask} to load the actual asset.
 * @author mzechner
 */
public class AssetDescriptor<T>{
    public final String fileName;
    public final Class<T> type;
    public final AssetLoaderParameters params;
    /** The resolved file. May be null if the fileName has not been resolved yet. */
    public Fi file;
    /** Callback for when this asset is loaded.*/
    public Cons<T> loaded = t -> {};
    /** Callback for when this asset has an error.*/
    public @Nullable Cons<Throwable> errored = null;

    public AssetDescriptor(Class<T> assetType){
        this(assetType.getSimpleName(), assetType, null);
    }

    public AssetDescriptor(String fileName, Class<T> assetType){
        this(fileName, assetType, null);
    }

    /** Creates an AssetDescriptor with an already resolved name. */
    public AssetDescriptor(Fi file, Class<T> assetType){
        this(file, assetType, null);
    }

    public AssetDescriptor(String fileName, Class<T> assetType, AssetLoaderParameters<T> params){
        this.fileName = fileName.replaceAll("\\\\", "/");
        this.type = assetType;
        this.params = params;
    }

    /** Creates an AssetDescriptor with an already resolved name. */
    public AssetDescriptor(Fi file, Class<T> assetType, AssetLoaderParameters<T> params){
        this.fileName = file.path().replaceAll("\\\\", "/");
        this.file = file;
        this.type = assetType;
        this.params = params;
    }

    @Override
    public String toString(){
        return fileName +
        ", " +
        type.getName();
    }
}
