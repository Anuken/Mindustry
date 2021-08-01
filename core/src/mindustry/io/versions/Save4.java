package mindustry.io.versions;

import mindustry.io.*;

import java.io.*;

/** This version only writes entities, no entity ID mappings. */
public class Save4 extends SaveVersion{

    public Save4(){
        super(4);
    }

    @Override
    public void writeEntities(DataOutput stream) throws IOException{
        writeTeamBlocks(stream);
        writeWorldEntities(stream);
    }

    @Override
    public void readEntities(DataInput stream) throws IOException{
        readTeamBlocks(stream);
        readWorldEntities(stream);
    }

}
