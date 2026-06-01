package mindustry.mod.data;

import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.mod.*;

import java.io.*;

import static mindustry.Vars.*;

/** Abstract class for a kind of asset used in an asset mod. */
public abstract class DataAsset implements Comparable<DataAsset>{
    /** File path, including name and extension, but excluding base folder prefix. */
    public String path = "";
    /** File name, excluding extension. This is taken from the path. */
    public String name = "";

    /** sha256 of the internal data. this is null for non-external assets. */
    public @Nullable String stringHash;
    public @Nullable byte[] byteHash;
    public @Nullable Fi overrideCacheFile;

    /** Caches this asset in the asset folder, and updates its hash to correspond to the appropriate cache file. */
    public void updateData(byte[] data){
        setHash(Vars.assetCache.add(data));
    }

    public void setHash(byte[] value){
        if(value.length != 32) throw new IllegalArgumentException("hash must be 32 bytes long: " + value.length);
        byteHash = value;
        stringHash = DataAssetCache.encodeHash(value);
    }

    public void setPath(String path){
        this.path = path.replace('\\', '/');
        this.name = Strings.getFileNameWithoutExtension(path);
    }

    public abstract DataAssetType getType();

    public boolean isAlwaysEmbedded(){
        return getType().embedded;
    }

    public boolean isCached(){
        return overrideCacheFile != null || (stringHash != null && Vars.assetCache.has(stringHash));
    }

    public Fi getCacheFileNoNull(){
        Fi file = getCacheFile();
        if(file == null) throw new RuntimeException("Cache file for asset " + path + " not found!");
        return file;
    }

    public @Nullable Fi getCacheFile(){
        return overrideCacheFile != null ? overrideCacheFile : Vars.assetCache.get(stringHash);
    }

    /** Reads this asset in from a file on disk. Only used on the server. */
    public void readOverride(String path, Fi file) throws IOException{
        setPath(path);
        setHash(file.sha256());
        this.overrideCacheFile = file;
        //manually make this asset's hash refer to its file in the folder instead of using a folder
        assetCache.addOverride(stringHash, file);
    }

    public void read(DataInput stream) throws IOException{
        int length = stream.readInt();
        if(length == 0){
            Log.err("Empty asset in save: @", path);
            return;
        }

        byte[] data = new byte[length];
        stream.readFully(data);
        updateData(data);
    }

    public void write(DataOutput stream) throws IOException{
        Fi file = getCacheFile();
        if(file == null || !file.exists()){
            Log.err("Failed to embed asset in save: missing cache file: " + path);
            stream.writeInt(0);
            return;
        }

        try{
            //TODO: would be more memory efficient to use streams to copy it without reading the whole file at once
            byte[] bytes = file.readBytes();
            stream.writeInt(bytes.length);
            stream.write(bytes);
        }catch(ArcRuntimeException e){
            Log.err("Failed to write asset to save: " + path, e);
            stream.writeInt(0);
        }
    }

    @Override
    public String toString(){
        return getClass().getSimpleName() + "{" + path + "}";
    }

    @Override
    public int compareTo(DataAsset asset){
        return path.compareTo(asset.path);
    }
}
