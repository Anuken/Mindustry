package mindustry.io.versions;

import arc.util.io.*;
import mindustry.io.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

/** This version does not read custom chunk data (<= 6). */
public class LegacyRegionSaveVersion extends SaveVersion{

    public LegacyRegionSaveVersion(int version){
        super(version);
    }

    @Override
    public void read(DataInputStream stream, CounterInputStream counter, WorldContext context) throws IOException{
        region("meta", stream, counter, in -> readMeta(in, context));
        region("content", stream, counter, this::readContentHeader);

        try{
            region("map", stream, counter, in -> readMap(in, context));
            region("entities", stream, counter, this::readEntities);
        }finally{
            content.setTemporaryMapper(null);

        }
    }
}
