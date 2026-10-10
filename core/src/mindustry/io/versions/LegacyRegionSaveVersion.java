package mindustry.io.versions;

import arc.util.io.*;
import mindustry.io.*;

import java.io.*;

/** This version does not read custom chunk data (<= 6). */
public class LegacyRegionSaveVersion extends ShortChunkSaveVersion{

    public LegacyRegionSaveVersion(int version){
        super(version);
    }

    @Override
    public void read(DataInputStream stream, CounterInputStream counter, SaveLoadContext saveState) throws IOException{
        readRegion("meta", stream, counter, in -> readMeta(in, saveState));
        readRegion("content", stream, counter, in -> readContentHeader(in, saveState));
        readRegion("map", stream, counter, in -> readMap(in, saveState));
        readRegion("entities", stream, counter, in -> readEntities(in, saveState));
    }
}
