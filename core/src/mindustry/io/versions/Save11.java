package mindustry.io.versions;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

/**
 * Adds patches in content header. Unlike >= 12, this version has patches before the content header.
 * */
public class Save11 extends SaveVersion{

    public Save11(){
        super(11);
    }

    @Override
    public void read(DataInputStream stream, CounterInputStream counter, WorldContext context) throws IOException{
        readRegion("meta", stream, counter, in -> readMeta(in, context));
        readRegion("content", stream, counter, this::readContentHeader);

        try{
            readRegion("patches", stream, counter, this::readContentPatches);
            readRegion("map", stream, counter, in -> readMap(in, context));
            readRegion("entities", stream, counter, this::readEntities);
            readRegion("markers", stream, counter, this::readMarkers);
            readRegion("custom", stream, counter, this::readCustomChunks);
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    //old, simplified string-only data patches
    @Override
    public void readContentPatches(DataInput stream) throws IOException{
        Seq<String> patches = new Seq<>();

        int amount = stream.readUnsignedByte();
        if(amount > 0){
            for(int i = 0; i < amount; i++){
                int len = stream.readInt();
                byte[] bytes = new byte[len];
                stream.readFully(bytes);
                patches.add(new String(bytes, Strings.utf8));
            }
        }

        Events.fire(new ContentPatchLoadEvent(patches));

        if(patches.size > 0){
            try{
                state.patcher.apply(patches);
            }catch(Throwable e){
                Log.err("Failed to apply patches: " + patches, e);
            }
        }
    }
}
