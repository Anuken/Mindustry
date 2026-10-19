

package arc.net;

import arc.net.FrameworkMessage.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Represents a TCP and optionally a UDP connection between a {@link Client} and
 * a {@link Server}. If either underlying connection is closed or errors, both
 * connections are closed.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Connection{
    int id = -1;
    private String name;
    EndPoint endPoint;
    TcpConnection tcp;
    UdpConnection udp;
    InetSocketAddress udpRemoteAddress;
    private NetListener[] listeners = {};
    private final Object listenerLock = new Object();
    private int lastPingID;
    private long lastPingSendTime;
    private int returnTripTime;
    volatile boolean isConnected;
    volatile ArcNetException lastProtocolError;
    private Object arbitraryData;

    protected Connection(){
    }

    void initialize(NetSerializer serialization, int writeBufferSize, int objectBufferSize){
        tcp = new TcpConnection(serialization, writeBufferSize,
        objectBufferSize);
    }

    /**
     * Returns the server assigned ID. Will return -1 if this connection has
     * never been connected or the last assigned ID if this connection has been
     * disconnected.
     */
    public int getID(){
        return id;
    }

    /**
     * Returns true if this connection is connected to the remote end. Note that
     * a connection can become disconnected at any time.
     */
    public boolean isConnected(){
        return isConnected;
    }

    /**
     * Returns the last protocol error that occured on the connection.
     * @return The last protocol error or null if none error occured.
     */
    public ArcNetException getLastProtocolError(){
        return lastProtocolError;
    }

    /**
     * Sends the object over the network using TCP.
     * @return The number of bytes sent.
     */
    public int sendTCP(Object object){
        if(object == null) throw new IllegalArgumentException("object cannot be null.");

        try{
            return tcp.send(object);
        }catch(IOException | ArcNetException ex){
            close(DcReason.error);
            ArcNet.handleError(ex);
            return 0;
        }
    }

    /**
     * Sends a pre-serialized, length-prefixed buffer over TCP.
     * Note that this cannot use a raw buffer like UDP, it must contain length!
     * @return The number of bytes sent, 0 on error.
     */
    public int sendTCPBuffer(ByteBuffer buffer){
        if(buffer == null) throw new IllegalArgumentException("buffer cannot be null.");

        try{
            return tcp.sendBuffer(buffer);
        }catch(IOException | ArcNetException ex){
            close(DcReason.error);
            ArcNet.handleError(ex);
            return 0;
        }
    }

    /**
     * Sends the object over the network using UDP.
     * @return The number of bytes sent.
     * @throws IllegalStateException if this connection was not opened with both TCP and UDP.
     */
    public int sendUDP(Object object){
        if(object == null) throw new IllegalArgumentException("object cannot be null.");
        SocketAddress address = udpRemoteAddress;
        if(address == null && udp != null)
            address = udp.connectedAddress;
        if(address == null && isConnected)
            throw new IllegalStateException("Connection is not connected via UDP.");

        try{
            if(address == null) throw new SocketException("Connection is closed.");

            return udp.send(object, address);
        }catch(IOException | ArcNetException ex){
            close(DcReason.error);
            ArcNet.handleError(ex);
            return 0;
        }
    }

    /**
     * Sends a pre-serialized buffer over UDP.
     * @return The number of bytes sent, 0 on error.
     */
    public int sendUDPBuffer(ByteBuffer buffer){
        if(buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
        SocketAddress address = udpRemoteAddress;
        if(address == null && udp != null)
            address = udp.connectedAddress;
        if(address == null && isConnected)
            throw new IllegalStateException("Connection is not connected via UDP.");

        try{
            if(address == null) throw new SocketException("Connection is closed.");

            return udp.sendBuffer(buffer, address);
        }catch(IOException | ArcNetException ex){
            close(DcReason.error);
            ArcNet.handleError(ex);
            return 0;
        }
    }

    public void close(DcReason reason){
        boolean wasConnected = isConnected;
        isConnected = false;
        tcp.close();
        if(udp != null && udp.connectedAddress != null)
            udp.close();
        if(wasConnected){
            notifyDisconnected(reason);
        }
        setConnected(false);
    }

    /**
     * Requests the connection to communicate with the remote computer to
     * determine a new value for the {@link #getReturnTripTime() return trip
     * time}. When the connection receives a {@link FrameworkMessage.Ping}
     * object with {@link Ping#isReply isReply} set to true, the new return trip
     * time is available.
     */
    public void updateReturnTripTime(){
        Ping ping = new Ping();
        ping.id = lastPingID++;
        lastPingSendTime = System.currentTimeMillis();
        sendTCP(ping);
    }

    /**
     * Returns the last calculated TCP return trip time, or -1 if
     * {@link #updateReturnTripTime()} has never been called or the
     * {@link FrameworkMessage.Ping} response has not yet been received.
     */
    public int getReturnTripTime(){
        return returnTripTime;
    }

    /**
     * An empty object will be sent if the TCP connection has not sent an object
     * within the specified milliseconds. Periodically sending a keep alive
     * ensures that an abnormal close is detected in a reasonable amount of time
     * (see {@link #setTimeout(int)} ). Also, some network hardware will close a
     * TCP connection that ceases to transmit for a period of time (typically 1+
     * minutes). Set to zero to disable. Defaults to 8000.
     */
    public void setKeepAliveTCP(int keepAliveMillis){
        tcp.keepAliveMillis = keepAliveMillis;
    }

    /**
     * If the specified amount of time passes without receiving an object over
     * TCP, the connection is considered closed. When a TCP socket is closed
     * normally, the remote end is notified immediately and this timeout is not
     * needed. However, if a socket is closed abnormally (eg, power loss),
     * ArcNet uses this timeout to detect the problem. The timeout should be
     * set higher than the {@link #setKeepAliveTCP(int) TCP keep alive} for the
     * remote end of the connection. The keep alive ensures that the remote end
     * of the connection will be constantly sending objects, and setting the
     * timeout higher than the keep alive allows for network latency. Set to
     * zero to disable. Defaults to 12000.
     */
    public void setTimeout(int timeoutMillis){
        tcp.timeoutMillis = timeoutMillis;
    }

    /** Adds a listener to the connection, after existing listeners. If the listener already exists, it is not added again. */
    public void addListener(NetListener listener){
        if(listener == null)
            throw new IllegalArgumentException("listener cannot be null.");
        synchronized(listenerLock){
            NetListener[] listeners = this.listeners;
            int n = listeners.length;
            for(int i = 0; i < n; i++)
                if(listener == listeners[i])
                    return;
            NetListener[] newListeners = new NetListener[n + 1];
            System.arraycopy(listeners, 0, newListeners, 0, n);
            newListeners[n] = listener;
            this.listeners = newListeners;
        }
    }

    public void removeListener(NetListener listener){
        if(listener == null)
            throw new IllegalArgumentException("listener cannot be null.");
        synchronized(listenerLock){
            NetListener[] listeners = this.listeners;
            int n = listeners.length;
            if(n == 0)
                return;
            NetListener[] newListeners = new NetListener[n - 1];
            for(int i = 0, ii = 0; i < n; i++){
                NetListener copyListener = listeners[i];
                if(listener == copyListener)
                    continue;
                if(ii == n - 1)
                    return;
                newListeners[ii++] = copyListener;
            }
            this.listeners = newListeners;
        }
    }

    void notifyConnected(){
        NetListener[] listeners = this.listeners;
        for(NetListener listener : listeners){
            listener.connected(this);
        }
    }

    void notifyDisconnected(DcReason reason){
        NetListener[] listeners = this.listeners;
        for(NetListener listener : listeners){
            listener.disconnected(this, reason);
        }
    }

    void notifyIdle(){
        NetListener[] listeners = this.listeners;
        for(NetListener listener : listeners){
            listener.idle(this);
            if(!isIdle())
                break;
        }
    }

    void notifyReceived(Object object){
        if(object instanceof Ping){
            Ping ping = (Ping)object;
            if(ping.isReply){
                if(ping.id == lastPingID - 1){
                    returnTripTime = (int)(System.currentTimeMillis()
                    - lastPingSendTime);
                }
            }else{
                ping.isReply = true;
                sendTCP(ping);
            }
        }

        NetListener[] listeners = this.listeners;
        for(NetListener listener : listeners){
            listener.received(this, object);
        }
    }

    /**
     * Returns the local {@link Client} or {@link Server} to which this
     * connection belongs.
     */
    public EndPoint getEndPoint(){
        return endPoint;
    }

    /**
     * Returns the IP address and port of the remote end of the TCP connection,
     * or null if this connection is not connected.
     */
    public InetSocketAddress getRemoteAddressTCP(){
        SocketChannel socketChannel = tcp.socketChannel;
        if(socketChannel != null){
            Socket socket = tcp.socketChannel.socket();
            if(socket != null){
                return (InetSocketAddress)socket.getRemoteSocketAddress();
            }
        }
        return null;
    }

    /**
     * Returns the IP address and port of the remote end of the UDP connection,
     * or null if this connection is not connected.
     */
    public InetSocketAddress getRemoteAddressUDP(){
        return udp.connectedAddress != null ? udp.connectedAddress : udpRemoteAddress;
    }

    /**
     * Sets the friendly name of this connection. This is returned by
     * {@link #toString()} and is useful for providing application specific
     * identifying information in the logging. May be null for the default name
     * of "Connection X", where X is the connection ID.
     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Returns the number of bytes that are waiting to be written to the TCP
     * socket, if any.
     */
    public int getTcpWriteBufferSize(){
        return tcp.writeBuffer.position();
    }

    /**
     * @see #setIdleThreshold(float)
     */
    public boolean isIdle(){
        return tcp.writeBuffer.position() / (float)tcp.writeBuffer.capacity() < tcp.idleThreshold;
    }

    /**
     * If the percent of the TCP write buffer that is filled is less than the
     * specified threshold, {@link NetListener#idle(Connection)} will be called for
     * each network thread update. Default is 0.1.
     */
    public void setIdleThreshold(float idleThreshold){
        tcp.idleThreshold = idleThreshold;
    }

    public String toString(){
        if(name != null)
            return name;
        return "Connection " + id;
    }

    void setConnected(boolean isConnected){
        this.isConnected = isConnected;
        if(isConnected && name == null)
            name = "Connection " + id;
    }

    public Object getArbitraryData(){
        return arbitraryData;
    }

    public void setArbitraryData(Object arbitraryData){
        this.arbitraryData = arbitraryData;
    }
}
