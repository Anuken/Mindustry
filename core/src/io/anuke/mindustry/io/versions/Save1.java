package io.anuke.mindustry.io.versions;

import io.anuke.arc.function.Supplier;
import io.anuke.mindustry.entities.traits.SaveTrait;
import io.anuke.mindustry.io.SaveVersion;

import java.io.DataInput;
import java.io.IOException;

public class Save1 extends SaveVersion{

    public Save1(){
        super(1);
    }

    @Override
    public void readEntities(DataInput stream) throws IOException{
        Supplier[] table = LegacyTypeTable.getTable(lastReadBuild);

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
