package arc.util.io;

import java.io.ByteArrayOutputStream;

public class ReusableByteOutStream extends ByteArrayOutputStream{

    public ReusableByteOutStream(int capacity){
        super(capacity);
    }

    public ReusableByteOutStream(){
    }

    public byte[] getBytes(){
        return buf;
    }

}
