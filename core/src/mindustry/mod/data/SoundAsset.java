package mindustry.mod.data;

public class SoundAsset extends AudioAsset{

    @Override
    public int maxSize(){
        return 1024 * 500;
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.sound;
    }
}
