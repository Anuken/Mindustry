package io.anuke.mindustry.io.versions;

import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.type.*;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

public class Save2 extends SaveVersion{

    public Save2(){
        super(2);
    }

    @Override
    public void readEntities(DataInput stream) throws IOException{
        byte groups = stream.readByte();

        for(int i = 0; i < groups; i++){
            int amount = stream.readInt();
            for(int j = 0; j < amount; j++){
                //TODO throw exception on read fail
                readChunk(stream, true, in -> {
                    byte typeid = in.readByte();
                    byte version = in.readByte();
                    SaveTrait trait = (SaveTrait)content.<TypeID>getByID(ContentType.typeid, typeid).constructor.get();
                    trait.readSave(in, version);
                });
            }
        }
    }
}
