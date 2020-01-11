package mindustry.io.versions;

import arc.func.*;
import mindustry.entities.traits.*;

import java.io.*;

public class Save1 extends Save2{

    public Save1(){
        version = 1;
    }

    @Override
    public void readEntities(DataInput stream) throws IOException{
        Prov[] table = LegacyTypeTable.getTable(lastReadBuild);

        byte groups = stream.readByte();

        for(int i = 0; i < groups; i++){
            int amount = stream.readInt();
            for(int j = 0; j < amount; j++){
                readChunk(stream, true, in -> {
                    byte typeid = in.readByte();
                    byte version = in.readByte();
                    SaveTrait trait = (SaveTrait)table[typeid].get();
                    trait.readSave(in, version);
                });
            }
        }
    }
}
