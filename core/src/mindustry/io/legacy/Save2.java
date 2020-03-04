package mindustry.io.legacy;

import java.io.*;

public class Save2 extends LegacySaveVersion{

    public Save2(){
        super(2);
    }

    @Override
    public void readEntities(DataInput stream) throws IOException{
        readLegacyEntities(stream);
    }
}
