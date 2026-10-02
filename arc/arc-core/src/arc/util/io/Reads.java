package arc.util.io;

import arc.util.*;

import java.io.*;

/** A wrapper for DataInput with more concise method names and no IOExceptions. */
public class Reads implements Closeable{
    private @Nullable byte[] bytearr;
    private @Nullable char[] chararr;

    public DataInput input;

    public Reads(DataInput input){
        this.input = input;
    }

    /** @return -1 if EOF or unsupported, or the next byte. */
    public int checkEOF(){
        try{
            if(input instanceof InputStream){
                return ((InputStream)input).read();
            }
            return -1;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read long */
    public long l(){
        try{
            return input.readLong();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read int */
    public int i(){
        try{
            return input.readInt();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read short */
    public short s(){
        try{
            return input.readShort();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read unsigned short */
    public int us(){
        try{
            return input.readUnsignedShort();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read byte */
    public byte b(){
        try{
            return input.readByte();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** allocate & read byte array */
    public byte[] b(int length){
        try{
            byte[] array = new byte[length];
            input.readFully(array);
            return array;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read byte array */
    public byte[] b(byte[] array){
        try{
            input.readFully(array);
            return array;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read byte array w/ offset */
    public byte[] b(byte[] array, int offset, int length){
        try{
            input.readFully(array, offset, length);
            return array;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read unsigned byte */
    public int ub(){
        try{
            return input.readUnsignedByte();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read boolean */
    public boolean bool(){
        try{
            return input.readBoolean();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read float */
    public float f(){
        try{
            return input.readFloat();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read double */
    public double d(){
        try{
            return input.readDouble();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read string (UTF) */
    public String str(){
        try{
            return input.readUTF();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** skip bytes */
    public void skip(int amount){
        try{
            input.skipBytes(amount);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** read string (UTF), with an optional max length */
    public String str(int maxLen){
        try{
            DataInput in = input;

            int utflen = in.readUnsignedShort();
            if(maxLen > 0 && utflen > maxLen) throw new RuntimeException("String too long: " + maxLen);

            if(bytearr == null || bytearr.length < utflen){
                bytearr = new byte[utflen * 2];
                chararr = new char[utflen * 2];
            }

            int count = 0;
            int chararr_count = 0;
            in.readFully(bytearr, 0, utflen);

            int c;
            while(count < utflen){
                c = bytearr[count] & 255;
                if(c > 127){
                    break;
                }

                ++count;
                chararr[chararr_count++] = (char)c;
            }

            while(count < utflen){
                c = bytearr[count] & 255;
                byte char2;
                switch(c >> 4){
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        ++count;
                        chararr[chararr_count++] = (char)c;
                        break;
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    default:
                        throw new UTFDataFormatException("malformed input around byte " + count);
                    case 12:
                    case 13:
                        count += 2;
                        if(count > utflen){
                            throw new UTFDataFormatException("malformed input: partial character at end");
                        }

                        char2 = bytearr[count - 1];
                        if((char2 & 192) != 128){
                            throw new UTFDataFormatException("malformed input around byte " + count);
                        }

                        chararr[chararr_count++] = (char)((c & 31) << 6 | char2 & 63);
                        break;
                    case 14:
                        count += 3;
                        if(count > utflen){
                            throw new UTFDataFormatException("malformed input: partial character at end");
                        }

                        char2 = bytearr[count - 2];
                        int char3 = bytearr[count - 1];
                        if((char2 & 192) != 128 || (char3 & 192) != 128){
                            throw new UTFDataFormatException("malformed input around byte " + (count - 1));
                        }

                        chararr[chararr_count++] = (char)((c & 15) << 12 | (char2 & 63) << 6 | (char3 & 63) << 0);
                }
            }

            return new String(chararr, 0, chararr_count);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close(){
        if(input instanceof Closeable){
            try{
                ((Closeable)input).close();
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }
    }
}
