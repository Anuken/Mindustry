package mindustry.io.versions;

import java.io.*;

/** This version only reads entities, no entity ID mappings. */
public class Save4 extends LegacySaveVersion2{

    public Save4(){
        super(4);
    }

    @Override
    public void readEntities(DataInput stream) throws IOException{
        readTeamBlocks(stream);
        readWorldEntities(stream);
    }

}
