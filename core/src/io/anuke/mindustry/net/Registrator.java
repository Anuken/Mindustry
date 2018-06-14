package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.Packets.StreamBegin;
import io.anuke.mindustry.net.Packets.StreamChunk;

public class Registrator {
    private static Class<?>[] classes = {
        StreamBegin.class,
        StreamChunk.class,
        WorldStream.class,
        ConnectPacket.class,
        ClientSnapshotPacket.class,
        SnapshotPacket.class,
        InvokePacket.class
    };
    private static ObjectIntMap<Class<?>> ids = new ObjectIntMap<>();

    static{
        if(classes.length > 127) throw new RuntimeException("Can't have more than 127 registered classes!");
        for(int i = 0; i < classes.length; i ++){
            if(!ClassReflection.isAssignableFrom(Packet.class, classes[i]) &&
                    !ClassReflection.isAssignableFrom(Streamable.class, classes[i])) throw new RuntimeException("Not a packet: " + classes[i]);
            ids.put(classes[i], i);
        }
    }

    public static Class<?> getByID(byte id){
        return classes[id];
    }

    public static byte getID(Class<?> type){
        return (byte)ids.get(type, -1);
    }

    public static Class<?>[] getClasses(){
        return classes;
    }
}
