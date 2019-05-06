package io.anuke.mindustry.io.versions;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.Serialization;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Zone;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class Save1 extends SaveFileVersion{

    public Save1(){
        super(1);
    }

    @Override
    public void read(DataInputStream stream) throws IOException{
        stream.readLong(); //time
        stream.readLong(); //total playtime
        stream.readInt(); //build

        //general state
        state.rules = Serialization.readRulesStreamJson(stream);
        String mapname = stream.readUTF();
        Map map = world.maps.all().find(m -> m.name().equals(mapname));
        if(map == null) map = new Map(customMapDirectory.child(mapname), 1, 1, new ObjectMap<>(), true);
        world.setMap(map);
        state.rules.spawns = map.getWaves();
        if(content.getByID(ContentType.zone, state.rules.zone) != null){
            Rules rules = content.<Zone>getByID(ContentType.zone, state.rules.zone).rules.get();
            if(rules.spawns != DefaultWaves.get()){
                state.rules.spawns = rules.spawns;
            }
        }

        int wave = stream.readInt();
        float wavetime = stream.readFloat();

        state.wave = wave;
        state.wavetime = wavetime;
        state.stats = Serialization.readStats(stream);
        world.spawner.read(stream);

        content.setTemporaryMapper(readContentHeader(stream));

        readEntities(stream);
        readMap(stream);
    }

    @Override
    public void write(DataOutputStream stream) throws IOException{
        //--META--
        stream.writeInt(version); //version id
        stream.writeLong(Time.millis()); //last saved
        stream.writeLong(headless ? 0 : control.saves.getTotalPlaytime()); //playtime
        stream.writeInt(Version.build); //build

        //--GENERAL STATE--
        Serialization.writeRulesStreamJson(stream, state.rules);
        stream.writeUTF(world.getMap().name()); //map name

        stream.writeInt(state.wave); //wave
        stream.writeFloat(state.wavetime); //wave countdown

        Serialization.writeStats(stream, state.stats);

        world.spawner.write(stream);

        writeContentHeader(stream);

        //--ENTITIES--

        writeEntities(stream);

        writeMap(stream);
    }
}
