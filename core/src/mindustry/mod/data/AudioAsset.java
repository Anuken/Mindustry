package mindustry.mod.data;

import arc.files.*;

import java.io.*;

public abstract class AudioAsset extends DataAsset{
    public byte[] data;

    public abstract int maxSize();

    @Override
    public void readFromFile(String path, Fi file) throws IOException{
        setPath(path);
        if(file.length() > maxSize()) throw new IOException("Audio asset too large (" + file.length() + " bytes). Maximum length in bytes: " + maxSize());
        data = file.readBytes();
    }

    @Override
    void read(DataInput stream) throws IOException{
        int len = stream.readInt();
        if(len > maxSize()) throw new IOException("Audio asset too large (" + len + " bytes). Maximum length in bytes: " + maxSize());
        data = new byte[len];
        stream.readFully(data);
    }

    @Override
    void write(DataOutput stream) throws IOException{
        stream.writeInt(data.length);
        stream.write(data);
    }
}
