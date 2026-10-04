package arc.util.io;

import java.io.*;

public class CounterInputStream extends FilterInputStream{
    public int count;

    public CounterInputStream(InputStream inputStream){
        super(inputStream);
    }

    public void resetCount(){
        count = 0;
    }

    @Override
    public long skip(long l) throws IOException{
        long skipped = super.skip(l);
        count += skipped;
        return skipped;
    }

    @Override
    public int read() throws IOException{
        count ++;
        return in.read();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int total = in.read(bytes, offset, length);
        count += total;
        return total;
    }
}
