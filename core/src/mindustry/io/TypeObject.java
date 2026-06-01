package mindustry.io;

import arc.func.*;
import arc.struct.*;
import arc.util.io.*;

public interface TypeObject{
    IntMap<Func<Reads, Object>> readers = new IntMap<>();
    IntMap<String> names = new IntMap<>();

    static Func<Reads, Object> handler(int i){
        return readers.get(i);
    }

    /**
     * Registers a custom type reader by id. This is mostly used for mods that need to transmit custom data.
     * @param id a mod-specific, unique id for identifying this type object.
     * @param name a mod-specific, unique name for identifying this type object. Prefix preferred,
     */
    static void register(int id, String name, Func<Reads, Object> reader){
        if(readers.get(id) != null) throw new IllegalArgumentException("Two type object cannot have the same id! (issue: '" + name + "' and '" + names.get(id) + "')");
        readers.put(id, reader);
        names.put(id, name);
    }

    int objectID();

    void typeWrite(Writes write);
}
