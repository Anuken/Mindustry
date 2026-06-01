package mindustry.mod.data;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;

import java.io.*;

public class ContentAsset extends DataAsset{
    //Note: sectors can't be loaded at the moment
    public static final ContentType[] loadableContent = {ContentType.item, ContentType.block, ContentType.liquid, ContentType.status, ContentType.unit, ContentType.weather, ContentType.planet, ContentType.unitCommand, ContentType.unitStance};

    /** Content type to be parsed as. */
    public ContentType type = ContentType.unit;
    /** Raw string data to be parsed into JSON. */
    public String data = "";
    /** Warnings encountered during deserialization. */
    public Seq<String> warnings = new Seq<>();

    public void readFromFile(String path, Fi file, ContentType type) throws IOException{
        this.type = type;
        setPath(path);
        data = file.readString();
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
}
