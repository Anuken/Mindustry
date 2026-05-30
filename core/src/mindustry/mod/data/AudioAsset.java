package mindustry.mod.data;

import arc.files.*;

import java.io.*;

public abstract class AudioAsset extends DataAsset{
    public byte[] data;

    @Override
    public void readFromFile(String path, Fi file) throws IOException{
        setPath(path);
        data = file.readBytes();
    }

    @Override
    void read(DataInput stream) throws IOException{
        data = new byte[stream.readInt()];
        stream.readFully(data);
    }

    @Override
    void write(DataOutput stream) throws IOException{
        stream.writeInt(data.length);
        stream.write(data);
    }
}
