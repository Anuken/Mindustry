package arc.util;

import sun.misc.*;

import java.lang.reflect.*;
import java.nio.*;

//uses Unsafe, which won't compile with Gradle normally due to module export issues
public class UnsafeBuffers{
    private static Unsafe unsafe;
    private static long bufferOffset;
    public static boolean failed, initialized;

    public static void checkInit(){
        if(initialized) return;
        initialized = true;
        try{
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe)theUnsafe.get(null);

            //field doesn't exist on Android/iOS
            Field addressField = Buffer.class.getDeclaredField("address");
            bufferOffset = unsafe.objectFieldOffset(addressField);
            //verify that memory can be copied (in older Android versions, this method doesn't exist)
            sun.misc.Unsafe.class.getMethod("copyMemory", long.class, long.class, long.class);
            failed = false;
        }catch(Throwable e){
            //usually happens on Android and iOS
            failed = true;
        }
    }

    public static void copy(Buffer src, int srcPos, Buffer dst, int dstPos, int length){
        long addressSrc = unsafe.getLong(src, bufferOffset);
        long addressDst = unsafe.getLong(dst, bufferOffset);

        unsafe.copyMemory(addressSrc + srcPos, addressDst + dstPos, length);
    }

}

