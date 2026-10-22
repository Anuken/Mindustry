package mindustry.io.versions;

import mindustry.gen.*;
import mindustry.io.*;

import java.io.*;

/** This version only reads entities, no entity ID mappings. */
public class Save4 extends LegacySaveVersion2{

    public Save4(){
        super(4);
    }

    @Override
    public void readEntities(DataInput stream, SaveLoadContext state) throws IOException{
        readTeamBlocks(stream, state);
        readWorldEntities(stream, EntityMapping.idMap, state);
    }

}
