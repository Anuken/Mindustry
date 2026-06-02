package mindustry.io;

import arc.func.*;
import arc.struct.*;
import arc.util.io.*;

public interface CustomData{
    Seq<Func<Reads, Object>> readers = new Seq<>();
    Seq<Class<?>> classIds = new Seq<>();

    static int id(Class<?> clazz){
        clazz = clazz.isAnonymousClass() ? clazz.getSuperclass() : clazz;
        int id = classIds.indexOf(clazz);
        if(id == -1) throw new IllegalArgumentException("The class" + clazz.getSimpleName() + " has not been registered. ");
        return id;
    }

    static Func<Reads, Object> reader(int id){
        return readers.get(id);
    }

    /**
     * Registers a custom type reader by id. This is mostly used for mods that need to transmit custom data.
     */
    static <T extends CustomData> void register(Class<T> clazz, Func<Reads, Object> reader){
        if(classIds.contains(clazz)) throw new IllegalArgumentException("The class" + clazz.getSimpleName() + " has been registered twice.");
        classIds.add(clazz);
        readers.add(reader);
    }

    void dataWrite(Writes write);
}
