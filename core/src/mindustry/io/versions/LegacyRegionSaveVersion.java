package mindustry.io.versions;

import arc.util.io.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

/** This version does not read custom chunk data (<= 6). */
public class LegacyRegionSaveVersion extends ShortChunkSaveVersion{

    public LegacyRegionSaveVersion(int version){
        super(version);
    }

    @Override
    public void read(DataInputStream stream, CounterInputStream counter, WorldContext context) throws IOException{
        readRegion("meta", stream, counter, in -> readMeta(in, context));
        readRegion("content", stream, counter, this::readContentHeader);

        try{
            readRegion("map", stream, counter, in -> readMap(in, context));
            readRegion("entities", stream, counter, this::readEntities);
        }finally{
            content.setTemporaryMapper(null);

        }
    }
}
