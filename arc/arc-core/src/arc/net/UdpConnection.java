

package arc.net;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * @author Nathan Sweet <misc@n4te.com>
 */
public class UdpConnection{
    InetSocketAddress connectedAddress;
    DatagramChannel datagramChannel;
    int keepAliveMillis = 19000;
    final ByteBuffer readBuffer, writeBuffer;
    private final NetSerializer serialization;
    private SelectionKey selectionKey;
    private final Object writeLock = new Object();
    private long lastCommunicationTime;

    public UdpConnection(NetSerializer serialization, int bufferSize){
        this.serialization = serialization;
        readBuffer = ByteBuffer.allocate(bufferSize);
        writeBuffer = ByteBuffer.allocateDirect(bufferSize);
    }

    public void bind(Selector selector, InetSocketAddress localPort) throws IOException{
        close();
        readBuffer.clear();
        writeBuffer.clear();
        try{
            datagramChannel = selector.provider().openDatagramChannel();
            datagramChannel.socket().bind(localPort);
            datagramChannel.configureBlocking(false);
            selectionKey = datagramChannel.register(selector, SelectionKey.OP_READ);

            lastCommunicationTime = System.currentTimeMillis();
        }catch(IOException ex){
            close();
            throw ex;
        }
    }

    public void connect(Selector selector, InetSocketAddress remoteAddress) throws IOException{
        close();
        readBuffer.clear();
        writeBuffer.clear();
        try{
            datagramChannel = selector.provider().openDatagramChannel();
            datagramChannel.socket().bind(null);
            datagramChannel.socket().connect(remoteAddress);
            datagramChannel.configureBlocking(false);

            selectionKey = datagramChannel.register(selector, SelectionKey.OP_READ);

            lastCommunicationTime = System.currentTimeMillis();

            connectedAddress = remoteAddress;
        }catch(IOException ex){
            close();
            throw new IOException("Unable to connect to: " + remoteAddress, ex);
        }
    }

    public InetSocketAddress readFromAddress() throws IOException{
        DatagramChannel datagramChannel = this.datagramChannel;
        if(datagramChannel == null)
            throw new SocketException("Connection is closed.");
        lastCommunicationTime = System.currentTimeMillis();

        if(!datagramChannel.isConnected())
            return (InetSocketAddress)datagramChannel.receive(readBuffer); //always null on Android >= 5.0
        datagramChannel.read(readBuffer);
        return connectedAddress;
    }

    public Object readObject(){
        readBuffer.flip();
        try{
            try{
                Object object = serialization.read(readBuffer);
                if(readBuffer.hasRemaining())
                    throw new ArcNetException("Incorrect number of bytes ("
                    + readBuffer.remaining()
                    + " remaining) used to deserialize object: "
                    + object);
                return object;
            }catch(Exception ex){
                throw new ArcNetException("Error during UDP deserialization.", ex);
            }
        }finally{
            readBuffer.clear();
        }
    }

    /** Serializes the specified object. This method is thread safe. */
    public int send(Object object, SocketAddress address) throws IOException{
        DatagramChannel datagramChannel = this.datagramChannel;
        if(datagramChannel == null)
            throw new SocketException("Connection is closed.");
        synchronized(writeLock){
            try{
                try{
                    serialization.write(writeBuffer, object);
                }catch(Exception ex){
                    throw new ArcNetException("Error serializing object of type: " + object.getClass().getName(), ex);
                }
                writeBuffer.flip();
                int length = writeBuffer.limit();
                datagramChannel.send(writeBuffer, address);

                lastCommunicationTime = System.currentTimeMillis();

                return !writeBuffer.hasRemaining() ? length : -1;
            }finally{
                writeBuffer.clear();
            }
        }
    }

    /** Directly sends an entire buffer to this address. No serialization is performed. */
    public int sendBuffer(ByteBuffer buffer, SocketAddress address) throws IOException{
        DatagramChannel datagramChannel = this.datagramChannel;
        if(datagramChannel == null) throw new SocketException("Connection is closed.");

        //note: writeBuffer is not used and thus writeLock is not needed
        buffer.rewind();
        int length = buffer.remaining();
        datagramChannel.send(buffer, address);
        lastCommunicationTime = System.currentTimeMillis();
        return !buffer.hasRemaining() ? length : -1;
    }

    public void close(){
        connectedAddress = null;
        try{
            if(datagramChannel != null){
                datagramChannel.close();
                datagramChannel = null;
                if(selectionKey != null)
                    selectionKey.selector().wakeup();
            }
        }catch(IOException ignored){
        }
    }

    public boolean needsKeepAlive(long time){
        return connectedAddress != null && keepAliveMillis > 0
        && time - lastCommunicationTime > keepAliveMillis;
    }
}
