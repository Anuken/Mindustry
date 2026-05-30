package mindustry.mod.data;

import arc.files.*;
import arc.util.*;

import java.io.*;

public class BundleAsset extends DataAsset{
    public String string = "";

    @Override
    public void readFromFile(String path, Fi file) throws IOException{
        setPath(path);
        string = file.readString();
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.bundle;
    }

    @Override
    void read(DataInput stream) throws IOException{
        int len = stream.readInt();
        byte[] bytes = new byte[len];
        stream.readFully(bytes);
        string = new String(bytes, Strings.utf8);
    }

    @Override
    void write(DataOutput stream) throws IOException{
        byte[] bytes = string.getBytes(Strings.utf8);
        stream.writeInt(bytes.length);
        stream.write(bytes);
    }
}
