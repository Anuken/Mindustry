package mindustry.mod.data;

import arc.func.*;

public enum DataAssetType{
    patch("patches", PatchAsset::new),
    content("content", ContentAsset::new),
    image("sprites", ImageAsset::new),
    sound("sounds", SoundAsset::new),
    music("music", MusicAsset::new),
    bundle("bundles", BundleAsset::new);

    public final String folder;
    public final Prov<DataAsset> constructor;

    public static final DataAssetType[] all = values();

    DataAssetType(String folder, Prov<DataAsset> constructor){
        this.folder = folder;
        this.constructor = constructor;
    }

    public DataAsset create(){
        return constructor.get();
    }
}
