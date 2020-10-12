package mindustry.net;

import arc.util.pooling.Pool.*;

import java.nio.*;

public interface Packet extends Poolable{
    default void read(ByteBuffer buffer){}
    default void write(ByteBuffer buffer){}
    default void reset(){}

    default boolean isImportant(){
        return false;
    }

    default boolean isUnimportant(){
        return false;
    }
}
