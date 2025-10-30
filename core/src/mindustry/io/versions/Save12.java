package mindustry.io.versions;

import mindustry.game.*;
import mindustry.game.Teams.*;

import java.io.*;

import static mindustry.Vars.*;

public class Save12 extends LegacySaveVersion{

    public Save12(){
        super(12);
    }

    @Override
    public void readEntities(DataInput stream) throws IOException{
        readLegacyEntities(stream);
    }
}
