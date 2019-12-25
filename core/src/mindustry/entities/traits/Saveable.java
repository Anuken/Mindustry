package mindustry.entities.traits;

import java.io.*;

/** Marks something as saveable; not necessarily used for entities. */
public interface Saveable{
    void writeSave(DataOutput stream) throws IOException;
    void readSave(DataInput stream, byte version) throws IOException;
}
