package mindustry.mod.data;

import arc.func.*;
import arc.struct.*;

public enum DataAssetType{
    patch("patches", Seq.with("json", "hjson", "json5"), PatchAsset::new),
    content("content", Seq.with("json", "hjson", "json5"), ContentAsset::new),
    image("sprites", Seq.with("png"), ImageAsset::new),
    sound("sounds", Seq.with("mp3", "ogg"), SoundAsset::new),
    music("music", Seq.with("mp3", "ogg"), MusicAsset::new),
    bundle("bundles", Seq.with("properties"), BundleAsset::new);

    public final String folder;
    public final Seq<String> extensions;
    public final Prov<DataAsset> constructor;

    public static final DataAssetType[] all = values();

    DataAssetType(String folder, Seq<String> extensions, Prov<DataAsset> constructor){
        this.folder = folder;
        this.extensions = extensions;
        this.constructor = constructor;
    }

    public DataAsset create(){
        return constructor.get();
    }
}
