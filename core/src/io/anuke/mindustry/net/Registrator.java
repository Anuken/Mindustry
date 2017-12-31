package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Packets.WorldData;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;

public class Registrator {

    public static Class<?>[] getClasses(){
        return new Class<?>[]{
                StreamBegin.class,
                StreamChunk.class,
                WorldData.class,
                Class.class,
                byte[].class
        };
    }
}
