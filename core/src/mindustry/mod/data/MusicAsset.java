package mindustry.mod.data;

public class MusicAsset extends AudioAsset{

    @Override
    public int maxSize(){
        return 1024 * 1024 * 10;
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.music;
    }
}
