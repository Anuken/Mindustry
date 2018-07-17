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
        long time = stream.readLong();
        long playtime = stream.readLong();
        int build = stream.readInt();
        int sector = stream.readInt();
        byte mode = stream.readByte();
        String map = stream.readUTF();
        int wave = stream.readInt();
        byte difficulty = stream.readByte();
        return new SaveMeta(version, time, playtime, build, sector, mode, map, wave, Difficulty.values()[difficulty]);
    }

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;
}
