package mindustry.mod.data;

public class ImageAsset extends DataAsset{

    public ImageAsset(){}

    public ImageAsset(String path, byte[] hash){
        setPath(path);
        setHash(hash);
    }

    public boolean isGenerated(){
        return path.startsWith("generated/");
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.image;
    }
}
