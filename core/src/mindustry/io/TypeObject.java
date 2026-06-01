package mindustry.io;

import arc.func.*;
import arc.struct.*;
import arc.util.io.*;

public interface TypeObject{
    IntMap<Func<Reads, Object>> readers = new IntMap<>();

    static Func<Reads, Object> handler(int i){
        return readers.get(i);
    }

    /**
     * Registers a custom type reader by id. This is mostly used for mods that need to transmit custom data.
     * @param id a mod-specific, unique id for identifying this type object.
     * */
    static void register(int id, Func<Reads, Object> reader){
        readers.put(id, reader);
    }

    int objectID();

    void typeWrite(Writes write);
}
