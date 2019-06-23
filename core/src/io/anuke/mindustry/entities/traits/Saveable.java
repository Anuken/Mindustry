package io.anuke.mindustry.entities.traits;

import java.io.*;

public interface Saveable{
    void writeSave(DataOutput stream) throws IOException;
    void readSave(DataInput stream, byte version) throws IOException;
}
