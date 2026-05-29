package mindustry.mod.data;

import arc.util.*;

import java.io.*;

/** Abstract class for a kind of asset used in an asset mod. */
public abstract class DataAsset{
    /** File path, including name and extension, but excluding base folder prefix. */
    public String path;
    /** File name, excluding extension. This is taken from the path. */
    public String name;

    public void setPath(String path){
        this.path = path.replace('\\', '/');
        this.name = Strings.getFileNameWithoutExtension(path);
    }

    public abstract DataAssetType getType();

    abstract void read(DataInput stream) throws IOException;

    abstract void write(DataOutput stream) throws IOException;

    public static DataAsset readAsset(DataInput input) throws IOException{
        short typeId = input.readShort();
        if(typeId < 0 || typeId >= DataAssetType.all.length) throw new IOException("Invalid asset type ID: " + typeId);

        String path = input.readUTF();
        var type = DataAssetType.all[typeId];
        var asset = type.create();

        asset.setPath(path);
        asset.read(input);

        return asset;
    }

    public static void writeAsset(DataAsset asset, DataOutput output) throws IOException{
        var type = asset.getType();
        output.writeShort(type.ordinal());
        output.writeUTF(asset.path);
        asset.write(output);
    }
}
