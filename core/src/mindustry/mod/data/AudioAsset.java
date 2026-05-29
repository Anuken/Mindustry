package mindustry.mod.data;

import java.io.*;

public abstract class AudioAsset extends DataAsset{
    public byte[] data;

    @Override
    void read(DataInput stream) throws IOException{
        data = new byte[stream.readInt()];
        stream.readFully(data);
    }

    @Override
    void write(DataOutput stream) throws IOException{
        stream.writeInt(data.length);
    }
}
