package mindustry.io.versions;

import mindustry.io.*;

import java.io.*;

public class Save1 extends LegacySaveVersion{

    public Save1(){
        super(1);
    }

    @Override
    public void readEntities(DataInput stream, SaveReadState state) throws IOException{
        readLegacyEntities(stream);
    }
}
