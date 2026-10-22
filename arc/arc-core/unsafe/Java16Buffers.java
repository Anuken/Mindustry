package arc.util;

import java.nio.*;

public class Java16Buffers{

    public static void copy(ByteBuffer src, int srcPos, ByteBuffer dst, int dstPos, int len){
        //this is a Java 16 API and as such needs to be compiled with -target 16, unlike the rest of the codebase
        //using this class is preferred on Java 16+ as Unsafe is being deprecated and removed
        dst.put(dstPos, src, srcPos, len);
    }
}

