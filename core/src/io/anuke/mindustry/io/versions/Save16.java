package io.anuke.mindustry.io.versions;

import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Serialization;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Zone;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Save16 extends SaveFileVersion{

    public Save16(){
        super(16);
    }

    @Override
    public void read(DataInputStream stream) throws IOException{
        stream.readLong(); //time
        stream.readLong(); //total playtime
        stream.readInt(); //build

        //general state
        state.rules = Serialization.readRules(stream);
        //load zone spawn patterns if applicable
        if(content.getByID(ContentType.zone, state.rules.zone) != null){
            state.rules.spawns = content.<Zone>getByID(ContentType.zone, state.rules.zone).rules.get().spawns;
        }
        String mapname = stream.readUTF();
        Map map = world.maps.getByName(mapname);
        if(map == null) map = new Map(Strings.capitalize(mapname), 1, 1);
        world.setMap(map);

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
        Serialization.writeRules(stream, state.rules);
        stream.writeUTF(world.getMap().name); //map name

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
