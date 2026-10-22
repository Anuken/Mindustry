package arc.util.io;

import java.io.*;
import java.nio.*;

/** DataInput wrapper of ByteBuffer. */
public class ByteBufferInput implements DataInput{
    public ByteBuffer buffer;

    /** Wraps the specified ByteBuffer. */
    public ByteBufferInput(ByteBuffer buffer){
        this.buffer = buffer;
    }

    /** {@link #setBuffer} must be called before this object can be used. */
    public ByteBufferInput(){
    }

    public void setBuffer(ByteBuffer buffer){
        this.buffer = buffer;
    }

    @Override
    public void readFully(byte[] bytes){
        buffer.get(bytes);
    }

    @Override
    public void readFully(byte[] bytes, int f, int to){
        buffer.get(bytes, f, to);
    }

    @Override
    public int skipBytes(int i){
        buffer.position(buffer.position() + i);
        return i;
    }

    @Override
    public boolean readBoolean(){
        return buffer.get() == 1;
    }

    @Override
    public byte readByte(){
        return buffer.get();
    }

    @Override
    public int readUnsignedByte(){
        return buffer.get() & 0xff;
    }

    @Override
    public short readShort(){
        return buffer.getShort();
    }

    @Override
    public int readUnsignedShort(){
        return buffer.getShort() & 0xffff;
    }

    @Override
    public char readChar(){
        return buffer.getChar();
    }

    @Override
    public int readInt(){
        return buffer.getInt();
    }

    @Override
    public long readLong(){
        return buffer.getLong();
    }

    @Override
    public float readFloat(){
        return buffer.getFloat();
    }

    @Override
    public double readDouble(){
        return buffer.getDouble();
    }

    @Override
    public String readLine(){
        throw new RuntimeException("Stub!");
    }

    @Override
    public String readUTF() throws IOException{
        return DataInputStream.readUTF(this);
    }
}
