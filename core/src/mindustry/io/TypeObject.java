package mindustry.io;

import arc.func.*;
import arc.struct.*;
import arc.util.io.*;

/** Interface of the serializable data used in TypeIO.*/
public interface TypeObject{
    IntMap<Func<Reads, Object>> handlers = new IntMap<>();

    static Func<Reads, Object> handler(int i){
        return handlers.get(i);
    }

    static void register(int id, Func<Reads, Object> handler){
        handlers.put(id, handler);
    }

    int objectID();

    void typeWrite(Writes write);
}
