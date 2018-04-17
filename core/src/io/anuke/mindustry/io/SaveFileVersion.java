package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.anuke.mindustry.game.Difficulty;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class SaveFileVersion {
    private static final ObjectMap<Class<?>, Constructor> cachedConstructors = new ObjectMap<>();

    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = stream.readLong(); //read last saved time
        byte mode = stream.readByte(); //read the gamemode
        String map = stream.readUTF(); //read the map
        int wave = stream.readInt(); //read the wave
        byte difficulty = stream.readByte(); //read the difficulty
        return new SaveMeta(version, time, mode, map, wave, Difficulty.values()[difficulty]);
    }

    public abstract void read(DataInputStream stream) throws IOException;
    public abstract void write(DataOutputStream stream) throws IOException;

    protected <T> T construct(Class<T> type){
        try {
            if (!cachedConstructors.containsKey(type)) {
                Constructor cons = ClassReflection.getDeclaredConstructor(type);
                cons.setAccessible(true);
                cachedConstructors.put(type, cons);
            }

            return (T)cachedConstructors.get(type).newInstance();

        }catch (ReflectionException e){
            throw new RuntimeException(e);
        }
    }
}
