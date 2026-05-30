package mindustry.mod;

import arc.struct.*;
import mindustry.*;
import mindustry.mod.data.*;

/**
 * TODO: strict content limits and size limits.
 * Music: 10mb
 * Sounds: 500kb each
 * Images: 2mb
 */
public class DataManager{
    private DataPatcher patcher = new DataPatcher();
    private DataImagePacker packer = new DataImagePacker();
    private DataSoundLoader soundLoader = new DataSoundLoader();
    private DataBundleLoader bundleLoader = new DataBundleLoader();

    private ObjectMap<DataAssetType, Seq<DataAsset>> assets = new ObjectMap<>();

    public void reloadPatches(Seq<PatchAsset> patches){
        if(patches != getPatches()) getPatches().set(patches);

        patcher.unapply();
        patcher.apply(patches, getContent());
    }

    public void reloadImages(Seq<ImageAsset> images){
        if(images != getImages()) getImages().set(images);

        packer.unload();
        packer.pack(images);
    }

    public void load(Seq<DataAsset> newAssets){
        unload(); //if already loaded

        if(newAssets.isEmpty()) return;

        for(var asset : newAssets){
            assets.get(asset.getType(), Seq::new).add(asset);
        }

        bundleLoader.load(getBundles());

        soundLoader.load(getSounds(), getMusic());

        if(!Vars.headless) packer.pack(getImages());

        patcher.apply(getPatches(), getContent());
    }

    public void unload(){
        patcher.unapply();
        if(!Vars.headless) packer.unload();
        soundLoader.unload();
        bundleLoader.unload();

        assets.clear();
    }

    public Seq<DataAsset> getAllAssets(){
        Seq<DataAsset> result = new Seq<>();
        assets.each((key, seq) -> result.addAll(seq));
        return result;
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
