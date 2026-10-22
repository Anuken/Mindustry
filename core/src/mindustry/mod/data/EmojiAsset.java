package mindustry.mod.data;

import arc.*;
import arc.files.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.mod.*;

import java.io.*;

public class EmojiAsset extends DataAsset{
    //this data is currently unused, but could be used to specify animated frames or a name that differs from the sprite name
    public String data = "";

    public EmojiAsset(){
    }

    public EmojiAsset(String name){
        setPath(name);
    }

    public TextureRegion findRegion(){
        return Core.atlas.find(DataImagePacker.regionPrefix + name, name);
    }

    @Override
    public byte[] getData(){
        return data.getBytes(Strings.utf8);
    }

    @Override
    public void readFromZip(String path, Fi file){
        setPath(path);
        data = file.readString();
    }

    @Override
    public void readOverride(String path, Fi file) throws IOException{
        setPath(path);
        data = file.readString();
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.emoji;
    }

    @Override
    public void read(DataInput stream) throws IOException{
        int len = stream.readInt();
        byte[] bytes = new byte[len];
        stream.readFully(bytes);
        data = new String(bytes, Strings.utf8);
    }

    @Override
    public void write(DataOutput stream) throws IOException{
        byte[] bytes = data.getBytes(Strings.utf8);
        stream.writeInt(bytes.length);
        stream.write(bytes);
    }
}
