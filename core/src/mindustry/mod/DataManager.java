package mindustry.mod;

import arc.struct.*;
import mindustry.*;
import mindustry.mod.data.*;

/**
 * TODO:
 * - Patchset names are weird and broken with the new path system
 * - Test planets/sectors/techtree stuff and make sure remove() works properly
 * - Try really hard to make the game crash or misbehave
 * - Unit tests
 */
public class DataManager{
    private DataPatcher patcher = new DataPatcher();
    private DataImagePacker packer = new DataImagePacker();
    private DataSoundLoader soundLoader = new DataSoundLoader();
    private DataBundleLoader bundleLoader = new DataBundleLoader();

    private ObjectMap<DataAssetType, Seq<DataAsset>> assets = new ObjectMap<>();
    private Seq<DataAsset> orderedAssets = new Seq<>();
    private Seq<DataAsset> orderedExternalAssets = new Seq<>();

    public void reloadPatches(Seq<PatchAsset> patches){
        if(patches != getPatches()) getPatches().set(patches);

        patcher.unapply();
        patcher.apply(patches, getContent());

        rebuildOrderedAssets();
    }

    public void reloadImages(Seq<ImageAsset> images){
        if(images != getImages()) getImages().set(images);

        packer.unload();
        packer.pack(images);

        rebuildOrderedAssets();
    }

    public void load(Seq<DataAsset> newAssets){
        unload(); //if already loaded

        if(newAssets.isEmpty()) return;

        for(var asset : newAssets){
            assets.get(asset.getType(), Seq::new).add(asset);
        }

        soundLoader.load(getSounds(), getMusic());

        if(!Vars.headless){
            bundleLoader.load(getBundles());
            packer.pack(getImages());
        }

        patcher.apply(getPatches(), getContent());

        rebuildOrderedAssets();
    }

    public void unload(){
        patcher.unapply();
        if(!Vars.headless){
            bundleLoader.unload();
            packer.unload();
        }
        soundLoader.unload();

        assets.clear();
        orderedAssets.clear();
        orderedExternalAssets.clear();
    }

    private void rebuildOrderedAssets(){
        orderedAssets.clear();
        orderedExternalAssets.clear();
        for(DataAssetType type : DataAssetType.all){
            var seq = assets.get(type);
            if(seq != null){
                orderedAssets.addAll(seq);
                if(!type.embedded){
                    orderedExternalAssets.addAll(seq);
                }
            }
        }
    }

    /** @return broken assets with no cache file */
    public Seq<DataAsset> getMissingAssets(){
        return orderedExternalAssets.select(d -> !d.isCached());
    }

    public Seq<DataAsset> getAllAssets(){
        return orderedAssets;
    }

    /** @return whether any assets like audio/images (external to saves) are loaded, requiring separate network transmission. */
    public boolean hasExternalAssets(){
        return orderedExternalAssets.size > 0;
    }

    /** @return all assets that can be external to a save (for network sync) */
    public Seq<DataAsset> getAllExternalAssets(){
        return orderedExternalAssets;
    }

    public boolean isPatched(Object content){
        return patcher.isPatched(content);
    }

    public <T extends DataAsset> Seq<T> getAssets(DataAssetType type){
        return assets.get(type, Seq::new).as();
    }

    public Seq<ImageAsset> getImages(){
        return getAssets(DataAssetType.image);
    }

    public Seq<PatchAsset> getPatches(){
        return getAssets(DataAssetType.patch);
    }

    public Seq<MusicAsset> getMusic(){
        return getAssets(DataAssetType.music);
    }

    public Seq<SoundAsset> getSounds(){
        return getAssets(DataAssetType.sound);
    }

    public Seq<BundleAsset> getBundles(){
        return getAssets(DataAssetType.bundle);
    }

    public Seq<ContentAsset> getContent(){
        return getAssets(DataAssetType.content);
    }
}
