package mindustry.net;

import arc.func.*;
import arc.struct.*;
import mindustry.net.Packets.*;

public class Registrator{
    private static final Seq<ClassEntry> classes = new Seq<>(127);
    private static final ObjectIntMap<Class<?>> ids = new ObjectIntMap<>();

    static{
        put(new ClassEntry(StreamBegin.class, StreamBegin::new));
        put(new ClassEntry(StreamChunk.class, StreamChunk::new));
        put(new ClassEntry(WorldStream.class, WorldStream::new));
        put(new ClassEntry(ConnectPacket.class, ConnectPacket::new));
        put(new ClassEntry(InvokePacket.class, InvokePacket::new));
    }

    public static ClassEntry getByID(byte id){
        return classes.get(id);
    }

    public static byte getID(Class<?> type){
        return (byte)ids.get(type, -1);
    }

    public static ClassEntry[] getClasses(){
        return classes.toArray();
    }

    public static void put(ClassEntry entry){
        classes.add(entry);
        ids.put(entry.type, classes.size - 1);

        if(classes.size > 127) throw new RuntimeException("Can't have more than 127 registered classes!");
    }

    public static class ClassEntry{
        public final Class<?> type;
        public final Prov<?> constructor;

        public <T extends Packet> ClassEntry(Class<T> type, Prov<T> constructor){
            this.type = type;
            this.constructor = constructor;
        }
    }
}
