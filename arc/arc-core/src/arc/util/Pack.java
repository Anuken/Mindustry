package arc.util;

import arc.math.*;

public class Pack{

    /** Packs [0-1] texture coordinates or positions into a single float. For use in VertexAttribute.packedTexCoords/packedPosition. */
    public static float packUv(float x, float y){
        return Float.intBitsToFloat(((int)(Mathf.clamp(x) * 65535)) | (((int)(Mathf.clamp(y) * 65535)) << 16));
    }

    /** byte -> unsigned byte */
    public static int u(byte b){
        return b & 0xff;
    }

    /** short -> unsigned short */
    public static int u(short b){
        return b & 0xffff;
    }

    /** int -> unsigned int */
    public static long u(int b){
        return b & 0x00000000ffffffffL;
    }

    public static byte byteValue(boolean b){
        return b ? (byte)1 : 0;
    }

    public static int shortInt(short left, short right){
        return (left << 16) | (right & 0xFFFF);
    }

    public static long longInt(int x, int y){
        return (((long)x) << 32) | (y & 0xffffffffL);
    }

    /** Packs two bytes with values 0-15 into one byte. */
    public static byte byteByte(byte left, byte right){
        return (byte)((left << 4) | right);
    }

    public static byte leftByte(byte value){
        return (byte)((value >> 4) & (byte)0x0F);
    }

    public static byte rightByte(byte value){
        return (byte)(value & 0x0F);
    }

    public static short leftShort(int field){
        return (short)(field >>> 16);
    }

    public static short rightShort(int field){
        return (short)(field & 0xFFFF);
    }

    public static int leftInt(long field){
        return (int)(field >> 32);
    }

    public static int rightInt(long field){
        return (int)(field);
    }

    public static byte leftByte(short field){
        return (byte)(field >> 8);
    }

    public static byte rightByte(short field){
        return (byte)field;
    }

    public static short shortByte(byte left, byte right){
        return (short)((left << 8) | (right & 0xFF));
    }

    /** The same array instance is returned each call. */
    public static byte[] bytes(int i, byte[] result){
        result[0] = (byte)(i >> 24);
        result[1] = (byte)(i >> 16);
        result[2] = (byte)(i >> 8);
        result[3] = (byte)(i /*>> 0*/);

        return result;
    }

    /** The same array instance is returned each call. */
    public static short[] shorts(long i, short[] resultShort){
        resultShort[0] = (short)(i >> 48);
        resultShort[1] = (short)(i >> 32);
        resultShort[2] = (short)(i >> 16);
        resultShort[3] = (short)(i /*>> 0*/);

        return resultShort;
    }

    public static long longShorts(short[] s){
        return ((long)(0xFFFF & s[0]) << 48) | ((long)(0xFFFF & s[1]) << 32) | ((long)(0xFFFF & s[2]) << 16) | (long)(0xFFFF & s[3]);
    }

    public static int intBytes(byte b1, byte b2, byte b3, byte b4){
        return ((0xFF & b1) << 24) | ((0xFF & b2) << 16) | ((0xFF & b3) << 8) | (0xFF & b4);
    }

    /** Packs 4 bytes into an int. */
    public static int intBytes(byte[] array){
        return ((0xFF & array[0]) << 24) | ((0xFF & array[1]) << 16) | ((0xFF & array[2]) << 8) | (0xFF & array[3]);
    }

    public static int bitmask(int bits, int mask, boolean b){
        return b ? bits | mask : bits & ~mask;
    }

}
