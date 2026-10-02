package arc.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

public class FastDeflaterOutputStream extends DeflaterOutputStream{
    private final byte[] tmp = {0};

    public FastDeflaterOutputStream(OutputStream outputStream){
        super(outputStream);
    }

    @Override
    public void write(int var1) throws IOException{
        tmp[0] = (byte)(var1 & 255);
        this.write(tmp, 0, 1);
    }
}
