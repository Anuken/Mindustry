package arc.util.serialization;

import java.io.*;

/**
 * A lightweight writing buffer to reduce the amount of write operations to be performed on the
 * underlying writer. This implementation is not thread-safe. It deliberately deviates from the
 * contract of Writer. In particular, it does not flush or close the wrapped writer nor does it
 * ensure that the wrapped writer is open.
 */
class WritingBuffer extends Writer{
    private final Writer writer;
    private final char[] buffer;
    private int fill = 0;

    WritingBuffer(Writer writer, int bufferSize){
        this.writer = writer;
        buffer = new char[bufferSize];
    }

    @Override
    public void write(int c) throws IOException{
        if(fill > buffer.length - 1){
            flush();
        }
        buffer[fill++] = (char)c;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException{
        if(fill > buffer.length - len){
            flush();
            if(len > buffer.length){
                writer.write(cbuf, off, len);
                return;
            }
        }
        System.arraycopy(cbuf, off, buffer, fill, len);
        fill += len;
    }

    @Override
    public void write(String str, int off, int len) throws IOException{
        if(fill > buffer.length - len){
            flush();
            if(len > buffer.length){
                writer.write(str, off, len);
                return;
            }
        }
        str.getChars(off, off + len, buffer, fill);
        fill += len;
    }

    /**  Flushes the internal buffer but does not flush the wrapped writer.*/
    @Override
    public void flush() throws IOException{
        writer.write(buffer, 0, fill);
        fill = 0;
    }

    /** Does not close or flush the wrapped writer.*/
    @Override
    public void close(){}
}