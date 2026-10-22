package arc.util.serialization;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.Jval.*;

import java.io.*;

/** Reads UBJSON from a stream into a {@link Jval} tree. */
public class UBJson{

    public static void write(Jval value, OutputStream output) throws IOException{
        DataOutputStream out = output instanceof DataOutputStream ? (DataOutputStream)output : new DataOutputStream(output);
        writeValue(value, out);
        out.flush();
    }

    private static void writeValue(Jval value, DataOutputStream out) throws IOException{
        switch(value.getType()){
            case object:
                writeObject(value, out);
                break;
            case array:
                writeArray(value, out);
                break;
            case string:
                writeString(value.asString(), out);
                break;
            case number:
                writeNumber(value.asDouble(), out);
                break;
            case bool:
                out.writeByte(value.isTrue() ? 'T' : 'F');
                break;
            default:
                out.writeByte('Z');
                break;
        }
    }

    private static void writeObject(Jval value, DataOutputStream out) throws IOException{
        out.writeByte('{');
        for(ObjectMap.Entry<String, Jval> entry : value.asObject()){
            writeSize(entry.key.getBytes(Strings.utf8).length, out);
            out.write(entry.key.getBytes(Strings.utf8));
            writeValue(entry.value, out);
        }
        out.writeByte('}');
    }

    private static void writeArray(Jval value, DataOutputStream out) throws IOException{
        out.writeByte('[');
        Jval.JsonArray arr = value.asArray();
        for(int i = 0; i < arr.size; i++) writeValue(arr.get(i), out);
        out.writeByte(']');
    }

    private static void writeString(String value, DataOutputStream out) throws IOException{
        byte[] bytes = value.getBytes(Strings.utf8);
        out.writeByte('S');
        writeSize(bytes.length, out);
        out.write(bytes);
    }

    private static void writeSize(int size, DataOutputStream out) throws IOException{
        if(size <= Byte.MAX_VALUE){
            out.writeByte('i');
            out.writeByte(size);
        }else if(size <= Short.MAX_VALUE){
            out.writeByte('I');
            out.writeShort(size);
        }else{
            out.writeByte('l');
            out.writeInt(size);
        }
    }

    private static void writeNumber(double value, DataOutputStream out) throws IOException{
        if(value == Math.rint(value) && !Double.isInfinite(value)){
            long l = (long)value;
            if(l >= Byte.MIN_VALUE && l <= Byte.MAX_VALUE){
                out.writeByte('i');
                out.writeByte((int)l);
            }else if(l >= Short.MIN_VALUE && l <= Short.MAX_VALUE){
                out.writeByte('I');
                out.writeShort((int)l);
            }else if(l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE){
                out.writeByte('l');
                out.writeInt((int)l);
            }else{
                out.writeByte('L');
                out.writeLong(l);
            }
        }else{
            out.writeByte('D');
            out.writeDouble(value);
        }
    }

    public static Jval read(Fi file){
        try(InputStream in = file.read(8192)){
            return read(in);
        }catch(IOException e){
            throw new ArcRuntimeException("Error parsing file: " + file, e);
        }
    }

    public static Jval read(InputStream input) throws IOException{
        DataInputStream in = input instanceof DataInputStream ? (DataInputStream)input : new DataInputStream(input);
        return readValue(in, in.readByte());
    }

    private static Jval readValue(DataInputStream in, byte type) throws IOException{
        switch(type){
            case '[':
                return readArray(in);
            case '{':
                return readObject(in);
            case 'Z':
                return Jval.nullValue;
            case 'T':
                return Jval.trueValue;
            case 'F':
                return Jval.falseValue;
            case 'B':
            case 'U':
                return Jval.valueOf(readUChar(in));
            case 'i':
                return Jval.valueOf(in.readByte());
            case 'I':
                return Jval.valueOf(in.readShort());
            case 'l':
                return Jval.valueOf(in.readInt());
            case 'L':
                return Jval.valueOf(in.readLong());
            case 'd':
                return Jval.valueOf(in.readFloat());
            case 'D':
                return Jval.valueOf(in.readDouble());
            case 's':
            case 'S':
                return Jval.valueOf(readString(in, type));
            case 'C':
                return Jval.valueOf(String.valueOf(in.readChar()));
            default:
                throw new ArcRuntimeException("Unrecognized data type: " + (char)type);
        }
    }

    private static Jval readArray(DataInputStream in) throws IOException{
        Jval result = Jval.newArray();
        byte type = in.readByte();
        while(type != ']'){
            result.add(readValue(in, type));
            type = in.readByte();
        }
        return result;
    }

    private static Jval readObject(DataInputStream in) throws IOException{
        JsonMap result = Jval.newObject();
        byte type = in.readByte();
        while(type != '}'){
            String key = readBytes(in, readSize(in, type));
            result.putAdd(key, readValue(in, in.readByte()));
            type = in.readByte();
        }
        return result;
    }

    private static String readString(DataInputStream in, byte type) throws IOException{
        long size = type == 's' ? readUChar(in) : readSize(in, in.readByte());
        return readBytes(in, size);
    }

    private static String readBytes(DataInputStream in, long size) throws IOException{
        byte[] data = new byte[(int)size];
        in.readFully(data);
        return new String(data, Strings.utf8);
    }

    private static long readSize(DataInputStream in, byte type) throws IOException{
        switch(type){
            case 'i':
                return readUChar(in);
            case 'I':
                return readUShort(in);
            case 'l':
                return in.readInt() & 0xFFFFFFFFL;
            case 'L':
                return in.readLong();
            default:
                throw new ArcRuntimeException("Unrecognized size type: " + (char)type);
        }
    }

    private static int readUChar(DataInputStream in) throws IOException{
        return in.readByte() & 0xFF;
    }

    private static int readUShort(DataInputStream in) throws IOException{
        return in.readShort() & 0xFFFF;
    }
}