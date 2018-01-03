package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.entities.enemies.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SaveFileVersion {
    public static final Array<Class<? extends Enemy>> enemyIDs = Array.with(
            Enemy.class,
            FastEnemy.class,
            RapidEnemy.class,
            FlamerEnemy.class,
            TankEnemy.class,
            BlastEnemy.class,
            MortarEnemy.class,
            TestEnemy.class,
            HealerEnemy.class,
            TitanEnemy.class,
            EmpEnemy.class
    );

    public static final ObjectMap<Class<? extends Enemy>, Byte> idEnemies = new ObjectMap<Class<? extends Enemy>, Byte>(){{
        for(int i = 0; i < enemyIDs.size; i ++){
            put(enemyIDs.get(i), (byte)i);
        }
    }};

    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        int version = stream.readInt(); //read version
        long time = stream.readLong(); //read last saved time
        byte mode = stream.readByte(); //read the gamemode
        byte map = stream.readByte(); //read the map
        int wave = stream.readInt(); //read the wave
        return new SaveMeta(version, time, mode, map, wave);
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
