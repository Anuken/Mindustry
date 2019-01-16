package io.anuke.mindustry.io.versions;

import io.anuke.arc.util.Time;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Serialization;
import io.anuke.mindustry.io.SaveFileVersion;
import io.anuke.mindustry.maps.Map;

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
        String mapname = stream.readUTF();
        Map map = world.maps.getByName(mapname);
        if(map == null) map = new Map("unknown", 1, 1);
        world.setMap(map);

        int wave = stream.readInt();
        float wavetime = stream.readFloat();

        state.wave = wave;
        state.wavetime = wavetime;

        content.setTemporaryMapper(readContentHeader(stream));

        world.spawner.read(stream);

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

        writeContentHeader(stream);

        world.spawner.write(stream); //spawnes

        //--ENTITIES--

        writeEntities(stream);

        writeMap(stream);
    }
}
