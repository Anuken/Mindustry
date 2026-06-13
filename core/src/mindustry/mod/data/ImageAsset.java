package mindustry.mod.data;

public class ImageAsset extends DataAsset{

    public ImageAsset(){}

    public ImageAsset(String path, byte[] hash){
        setPath(path);
        setHash(hash);
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.image;
    }
}
