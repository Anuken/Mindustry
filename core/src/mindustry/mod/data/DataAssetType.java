package mindustry.mod.data;

import arc.*;
import arc.func.*;
import arc.struct.*;

public enum DataAssetType{
    patch("patches", Seq.with("json", "hjson", "json5"), PatchAsset::new, true),
    content("content", Seq.with("json", "hjson", "json5"), ContentAsset::new, true),
    bundle("bundles", Seq.with("properties"), BundleAsset::new, false),
    image("sprites", Seq.with("png"), ImageAsset::new, false),
    sound("sounds", Seq.with("mp3", "ogg"), SoundAsset::new, false),
    music("music", Seq.with("mp3", "ogg"), MusicAsset::new, false)
    ;

    public final String folder;
    public final Seq<String> extensions;
    public final Prov<DataAsset> constructor;
    public final boolean embedded;

    public static final DataAssetType[] all = values();

    DataAssetType(String folder, Seq<String> extensions, Prov<DataAsset> constructor, boolean embedded){
        this.folder = folder;
        this.extensions = extensions;
        this.constructor = constructor;
        this.embedded = embedded;
    }

    public DataAsset create(){
        return constructor.get();
    }

    public String localized(){
        return Core.bundle.get("asset." + name());
    }

    public String localizedPlural(){
        return Core.bundle.get("asset." + name() + ".plural");
    }
}
