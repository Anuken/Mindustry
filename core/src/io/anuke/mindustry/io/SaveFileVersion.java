package io.anuke.mindustry.io;

import io.anuke.mindustry.game.Difficulty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SaveFileVersion{
    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = stream.readLong(); //read last saved time
        int build = stream.readInt();
        byte mode = stream.readByte(); //read the gamemode
        String map = stream.readUTF(); //read the map
        int wave = stream.readInt(); //read the wave
        byte difficulty = stream.readByte(); //read the difficulty
        return new SaveMeta(version, time, build, mode, map, wave, Difficulty.values()[difficulty]);
    }

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;
}
