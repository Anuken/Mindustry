

package arc.net;

import java.io.IOException;
import java.io.InputStream;

abstract public class InputStreamSender extends TcpIdleSender{
    private final InputStream input;
    private final byte[] chunk;

    public InputStreamSender(InputStream input, int chunkSize){
        this.input = input;
        chunk = new byte[chunkSize];
    }

    @Override
    protected final Object next(){
        try{
            int total = 0;
            while(total < chunk.length){
                int count = input.read(chunk, total, chunk.length - total);
                if(count < 0){
                    if(total == 0)
                        return null;
                    byte[] partial = new byte[total];
                    System.arraycopy(chunk, 0, partial, 0, total);
                    return next(partial);
                }
                total += count;
            }
        }catch(IOException ex){
            throw new ArcNetException(ex);
        }
        return next(chunk);
    }

    abstract protected Object next(byte[] chunk);
}
