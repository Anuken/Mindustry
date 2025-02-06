package mindustry.io.versions;

import arc.func.*;
import arc.util.io.*;
import mindustry.gen.*;

import java.io.*;

/** This version did not read/write entity IDs to the save. */
public class LegacySaveVersion2 extends LegacyRegionSaveVersion{

    public LegacySaveVersion2(int version){
        super(version);
    }

    @Override
    public void readWorldEntities(DataInput stream) throws IOException{
        //entityMapping is null in older save versions, so use the default
        Prov[] mapping = this.entityMapping == null ? EntityMapping.idMap : this.entityMapping;

        int amount = stream.readInt();
        for(int j = 0; j < amount; j++){
            readChunk(stream, true, in -> {
                int typeid = in.readUnsignedByte();
                if(mapping[typeid] == null){
                    in.skipBytes(lastRegionLength - 1);
                    return;
                }

                Entityc entity = (Entityc)mapping[typeid].get();
                entity.read(Reads.get(in));
                entity.add();
            });
        }
    }
}
