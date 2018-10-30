package io.anuke.ucore.util;

import java.nio.ByteBuffer;

public class IOUtils {

    public static String readString(ByteBuffer buffer){
        short length = buffer.getShort();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes);
    }

    public static void writeString(ByteBuffer buffer, String string){
        byte[] bytes = string.getBytes();
        if(bytes.length >= Short.MAX_VALUE) throw new IllegalArgumentException("Input string is too long!");
        buffer.putShort((short)bytes.length);
        buffer.put(bytes);
    }
}
