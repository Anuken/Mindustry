package mindustry.mod.data;

import arc.util.*;
import mindustry.ctype.*;

import java.io.*;

public class ContentAsset extends DataAsset{
    public ContentType type = ContentType.unit;
    public String data = "";

    @Override
    public DataAssetType getType(){
        return DataAssetType.content;
    }

    @Override
    void read(DataInput stream) throws IOException{
        type = ContentType.all[stream.readShort()];
        int len = stream.readInt();
        byte[] bytes = new byte[len];
        stream.readFully(bytes);
        data = new String(bytes, Strings.utf8);
    }

    @Override
    void write(DataOutput stream) throws IOException{
        stream.writeShort(type.ordinal());
        byte[] bytes = data.getBytes(Strings.utf8);
        stream.writeInt(bytes.length);
        stream.write(bytes);
    }
}
