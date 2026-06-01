package mindustry.io;

import arc.func.*;
import arc.struct.*;
import arc.util.io.*;

/** Interface of the serializable data used in TypeIO.*/
public interface TypeObject{
    Seq<Func<Reads, Object>> handlers = new Seq<>();

    static Func<Reads, Object> handler(int i){
        return handlers.get(i);
    }

    static void register(int id, Func<Reads, Object> handler){
        handlers.set(id, handler);
    }

    int id();

    void write(Writes write);
}
