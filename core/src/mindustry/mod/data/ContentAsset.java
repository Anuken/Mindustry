package mindustry.mod.data;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.mod.*;

import java.io.*;

public class ContentAsset extends DataAsset{
    //Note: sectors and planets can't be loaded at the moment, as custom campaigns aren't functional to begin with, and adding them would cause confusion
    public static final ContentType[] loadableContent = {ContentType.item, ContentType.block, ContentType.liquid, ContentType.status, ContentType.unit, ContentType.weather};

    /** Content type to be parsed as. */
    public ContentType type = ContentType.unit;
    /** Raw string data to be parsed into JSON. */
    public String data = "";
    /** Warnings encountered during deserialization. */
    public Seq<String> warnings = new Seq<>();
    /** Content that was loaded, if successful. */
    public @Nullable Content content;
    /** If true, this asset failed to completely, and cannot be used. */
    public boolean errored;

    public ContentAsset(String path, ContentType type, String data){
        setPath(path);
        this.type = type;
        this.data = data;
    }

    public ContentAsset(){
    }

    public String hashData(){
        return type.name() + "_" + DataAssetCache.encodeHash(Strings.sha256(data));
    }

    public void readOverride(String path, Fi file, ContentType type) throws IOException{
        this.type = type;
        setPath(path);
        data = file.readString();
    }

    @Override
    public String getFullPath(){
        return getType().folder + "/" + type.folderName + "/" + path;
    }

    @Override
    public byte[] getData(){
        return data.getBytes(Strings.utf8);
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.content;
    }

    @Override
    public void read(DataInput stream) throws IOException{
        type = ContentType.all[stream.readShort()];
        int len = stream.readInt();
        byte[] bytes = new byte[len];
        stream.readFully(bytes);
        data = new String(bytes, Strings.utf8);
    }

    @Override
    public void write(DataOutput stream) throws IOException{
        stream.writeShort(type.ordinal());
        byte[] bytes = data.getBytes(Strings.utf8);
        stream.writeInt(bytes.length);
        stream.write(bytes);
    }

    @Override
    public int compareTo(DataAsset asset){
        if(asset instanceof ContentAsset cont){
            int cmp = type.compareTo(cont.type);
            if(cmp != 0) return cmp;
        }

        return super.compareTo(asset);
    }
}
