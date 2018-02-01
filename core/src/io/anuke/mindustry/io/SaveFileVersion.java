package io.anuke.mindustry.io;

import io.anuke.mindustry.game.Difficulty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SaveFileVersion {
    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = stream.readLong(); //read last saved time
        byte mode = stream.readByte(); //read the gamemode
        byte map = stream.readByte(); //read the map
        int wave = stream.readInt(); //read the wave
        return new SaveMeta(version, time, mode, map, wave, Difficulty.normal);
    }

    public abstract void read(DataInputStream stream) throws IOException;
    public abstract void write(DataOutputStream stream) throws IOException;

    public static void writeString(DataOutputStream stream, String string) throws IOException{
        stream.writeByte(string.length());
        stream.writeBytes(string);
    }

    public static String readString(DataInputStream stream) throws IOException{
        int length = stream.readByte();
        byte[] result = new byte[length];
        stream.read(result);
        return new String(result);
    }
}
