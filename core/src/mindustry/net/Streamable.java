package mindustry.net;

import mindustry.net.Packets.*;

import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.*;

public class Streamable extends Packet{
    public transient ByteArrayInputStream stream;

    @Override
    public int getPriority(){
        return priorityHigh;
    }

    @Override
    public boolean allow(boolean server){
        return !server;
    }

    /** If true, this stream is handled as soon as it is received, instead of all at once. */
    public boolean incremental(){
        return false;
    }

    public static class StreamBuilder{
        public final int id;
        public final byte type;
        public final int total;
        public final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        public final boolean incremental;

        public final IncrementalStream incrementalStream;

        public int received;

        public StreamBuilder(StreamBegin begin, boolean incremental){
            id = begin.id;
            type = begin.type;
            total = begin.total;
            this.incremental = incremental;

            if(incremental){
                incrementalStream = new IncrementalStream();
            }else{
                incrementalStream = null;
            }
        }

        public float progress(){
            return (float)received / total;
        }

        public void add(byte[] bytes){
            received += bytes.length;
            try{
                if(incrementalStream != null){
                    incrementalStream.add(bytes);
                }else{
                    stream.write(bytes);
                }
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }

        public Streamable build(){
            Streamable s = Net.newPacket(type);
            s.stream = new ByteArrayInputStream(stream.toByteArray());
            return s;
        }

        public boolean isDone(){
            return received >= total;
        }

        public void close(){
            if(incrementalStream != null){
                incrementalStream.close();
            }
        }
    }

    /** Stream that has one thread write to an internal buffer, and another thread read from it. */
    static class IncrementalStream extends InputStream{
        private static final byte[] closedSignal = new byte[0];

        private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        private volatile boolean isClosed, isFinished;

        private byte[] currentBuffer = null, singleByte = new byte[1];
        private int currentOffset = 0;

        void add(byte[] bytes){
            if(isClosed || isFinished || bytes == null || bytes.length == 0){
                return;
            }
            queue.add(bytes);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException{
            if(off < 0 || len < 0 || len > b.length - off){
                throw new IndexOutOfBoundsException();
            }else if(len == 0){
                return 0;
            }

            //after a close(), no reads are allowed
            if(isClosed) throw new ClosedChannelException();

            if(currentBuffer == null || currentOffset >= currentBuffer.length){
                //after a finish(), reads are allowed, but only if the queue isn't empty (otherwise it's a EOF)
                //throwing EOF is against convention, but I don't care, this is invalid state, and you shouldn't be here
                if(isFinished && queue.isEmpty()) throw new EOFException();

                try{
                    currentBuffer = queue.take();
                    currentOffset = 0;

                    //when this stream gets closed, the buffer will only contain the 'signal' bytes; in that case, return EOF
                    if(currentBuffer == closedSignal){
                        queue.add(closedSignal);
                        //if the queue was finished, and you were trying to read something, you should get a EOF instead (invalid state)
                        if(isFinished) throw new EOFException();
                        throw new ClosedChannelException();
                    }
                }catch(InterruptedException e){
                    throw new IOException(e);
                }
            }

            int available = currentBuffer.length - currentOffset;
            int bytesToCopy = Math.min(len, available);

            System.arraycopy(currentBuffer, currentOffset, b, off, bytesToCopy);
            currentOffset += bytesToCopy;

            return bytesToCopy;
        }

        @Override
        public int read() throws IOException{
            int result = read(singleByte, 0, 1);
            return result == -1 ? -1 : (singleByte[0] & 0xFF);
        }

        /** unlike close(), this allows the stream to finish reading whatever it's reading, but if it tries to read more, it will return -1 (EOF) */
        public void finish(){
            isFinished = true;
            queue.add(closedSignal);
        }

        /** immediately stops all reading and makes subsequent reads throw a special ClosedChannelException */
        @Override
        public void close(){
            isClosed = true;
            queue.clear();
            queue.add(closedSignal);
        }
    }
}
