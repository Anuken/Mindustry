

package arc.net;

import arc.func.*;
import arc.net.FrameworkMessage.*;
import arc.util.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Represents a TCP and optionally a UDP connection to a {@link Server}.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Client extends Connection implements EndPoint{
    private final NetSerializer serialization;
    private Selector selector;
    private int emptySelects;
    private volatile boolean tcpRegistered, udpRegistered;
    private Object tcpRegistrationLock = new Object();
    private Object udpRegistrationLock = new Object();
    private volatile boolean shutdown;
    private final Object updateLock = new Object();
    private Thread updateThread;
    private int connectTimeout;
    private InetAddress connectHost;
    private int connectTcpPort;
    private int connectUdpPort;
    private boolean isClosed;
    private final ExecutorService discoverExecutor = Threads.unboundedExecutor("Server Discovery");
    private Prov<DatagramPacket> discoveryPacket = () -> new DatagramPacket(new byte[256], 256);

    /**
     * @param writeBufferSize One buffer of this size is allocated. Objects are serialized
     * to the write buffer where the bytes are queued until they can
     * be written to the TCP socket.
     * <p>
     * Normally the socket is writable and the bytes are written
     * immediately. If the socket cannot be written to and enough
     * serialized objects are queued to overflow the buffer, then the
     * connection will be closed.
     * <p>
     * The write buffer should be sized at least as large as the
     * largest object that will be sent, plus some head room to allow
     * for some serialized objects to be queued in case the buffer is
     * temporarily not writable. The amount of head room needed is
     * dependent upon the size of objects being sent and how often
     * they are sent.
     * @param objectBufferSize One (using only TCP) or three (using both TCP and UDP) buffers
     * of this size are allocated. These buffers are used to hold the
     * bytes for a single object graph until it can be sent over the
     * network or deserialized.
     * <p>
     * The object buffers should be sized at least as large as the
     * largest object that will be sent or received.
     */
    public Client(int writeBufferSize, int objectBufferSize, NetSerializer serialization){
        super();
        endPoint = this;

        this.serialization = serialization;

        initialize(serialization, writeBufferSize, objectBufferSize);

        try{
            selector = Selector.open();
        }catch(IOException ex){
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    public void setDiscoveryPacket(Prov<DatagramPacket> discoveryPacket){
        this.discoveryPacket = discoveryPacket;
    }

    /**
     * Opens a TCP only client.
     * @see #connect(int, InetAddress, int, int)
     */
    public void connect(int timeout, String host, int tcpPort) throws IOException{
        connect(timeout, InetAddress.getByName(host), tcpPort, -1);
    }

    /**
     * Opens a TCP and UDP client.
     * @see #connect(int, InetAddress, int, int)
     */
    public void connect(int timeout, String host, int tcpPort, int udpPort) throws IOException{
        connect(timeout, InetAddress.getByName(host), tcpPort, udpPort);
    }

    /**
     * Opens a TCP only client.
     * @see #connect(int, InetAddress, int, int)
     */
    public void connect(int timeout, InetAddress host, int tcpPort) throws IOException{
        connect(timeout, host, tcpPort, -1);
    }

    /**
     * Opens a TCP and UDP client. Blocks until the connection is complete or
     * the timeout is reached.
     * <p>
     * Because the framework must perform some minimal communication before the
     * connection is considered successful, {@link #update(int)} must be called
     * on a separate thread during the connection process.
     * @throws IllegalStateException if called from the connection's update thread.
     * @throws IOException if the client could not be opened or connecting times out.
     */
    public void connect(int timeout, InetAddress host, int tcpPort, int udpPort) throws IOException{
        if(host == null)
            throw new IllegalArgumentException("host cannot be null.");
        if(Thread.currentThread() == getUpdateThread())
            throw new IllegalStateException(
            "Cannot connect on the connection's update thread.");
        this.connectTimeout = timeout;
        this.connectHost = host;
        this.connectTcpPort = tcpPort;
        this.connectUdpPort = udpPort;
        close();
        id = -1;
        try{
            if(udpPort != -1)
                udp = new UdpConnection(serialization,
                tcp.readBuffer.capacity());

            long endTime;
            synchronized(updateLock){
                tcpRegistered = false;
                selector.wakeup();
                endTime = System.currentTimeMillis() + timeout;
                tcp.connect(selector, new InetSocketAddress(host, tcpPort),
                5000);
            }

            // Wait for RegisterTCP.
            synchronized(tcpRegistrationLock){
                while(!tcpRegistered && System.currentTimeMillis() < endTime){
                    try{
                        tcpRegistrationLock.wait(100);
                    }catch(InterruptedException ignored){
                    }
                }
                if(!tcpRegistered){
                    throw new SocketTimeoutException(
                    "Connected, but timed out during TCP registration.\n"
                    + "Note: Client#update must be called in a separate thread during connect.");
                }
            }

            if(udpPort != -1){
                InetSocketAddress udpAddress = new InetSocketAddress(host,
                udpPort);
                synchronized(updateLock){
                    udpRegistered = false;
                    selector.wakeup();
                    udp.connect(selector, udpAddress);
                }

                // Wait for RegisterUDP reply.
                synchronized(udpRegistrationLock){
                    while(!udpRegistered
                    && System.currentTimeMillis() < endTime){
                        RegisterUDP registerUDP = new RegisterUDP();
                        registerUDP.connectionID = id;
                        udp.send(registerUDP, udpAddress);
                        try{
                            udpRegistrationLock.wait(100);
                        }catch(InterruptedException ignored){
                        }
                    }
                    if(!udpRegistered)
                        throw new SocketTimeoutException(
                        "Connected, but timed out during UDP registration: "
                        + host + ":" + udpPort);
                }
            }
        }catch(IOException ex){
            close();
            throw ex;
        }
    }

    /**
     * Calls {@link #connect(int, InetAddress, int, int) connect} with the
     * values last passed to connect.
     * @throws IllegalStateException if connect has never been called.
     */
    public void reconnect() throws IOException{
        reconnect(connectTimeout);
    }

    /**
     * Calls {@link #connect(int, InetAddress, int, int) connect} with the
     * specified timeout and the other values last passed to connect.
     * @throws IllegalStateException if connect has never been called.
     */
    public void reconnect(int timeout) throws IOException{
        if(connectHost == null)
            throw new IllegalStateException(
            "This client has never been connected.");
        connect(timeout, connectHost, connectTcpPort, connectUdpPort);
    }

    /**
     * Reads or writes any pending data for this client. Multiple threads should
     * not call this method at the same time.
     * @param timeout Wait for up to the specified milliseconds for data to be ready
     * to process. May be zero to return immediately if there is no
     * data to process.
     */
    public void update(int timeout) throws IOException{
        updateThread = Thread.currentThread();
        synchronized(updateLock){ // Blocks to avoid a select while the
            // selector is used to bind the server
            // connection.
        }
        long startTime = System.currentTimeMillis();
        int select = 0;
        if(timeout > 0){
            select = selector.select(timeout);
        }else{
            select = selector.selectNow();
        }
        if(select == 0){
            emptySelects++;
            if(emptySelects == 100){
                emptySelects = 0;
                // NIO freaks and returns immediately with 0 sometimes, so try
                // to keep from hogging the CPU.
                long elapsedTime = System.currentTimeMillis() - startTime;
                try{
                    if(elapsedTime < 25)
                        Thread.sleep(25 - elapsedTime);
                }catch(InterruptedException ignored){
                }
            }
        }else{
            emptySelects = 0;
            isClosed = false;
            Set<SelectionKey> keys = selector.selectedKeys();
            synchronized(keys){
                for(Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext(); ){
                    keepAlive();
                    SelectionKey selectionKey = iter.next();
                    iter.remove();
                    try{
                        int ops = selectionKey.readyOps();
                        if((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ){
                            if(selectionKey.attachment() == tcp){
                                while(true){
                                    Object object = tcp.readObject();
                                    if(object == null)
                                        break;
                                    if(!tcpRegistered){
                                        if(object instanceof RegisterTCP){
                                            id = ((RegisterTCP)object).connectionID;
                                            synchronized(tcpRegistrationLock){
                                                tcpRegistered = true;
                                                tcpRegistrationLock.notifyAll();
                                                if(udp == null)
                                                    setConnected(true);
                                            }
                                            if(udp == null)
                                                notifyConnected();
                                        }
                                        continue;
                                    }
                                    if(udp != null && !udpRegistered){
                                        if(object instanceof RegisterUDP){
                                            synchronized(udpRegistrationLock){
                                                udpRegistered = true;
                                                udpRegistrationLock.notifyAll();
                                                setConnected(true);
                                            }
                                            notifyConnected();
                                        }
                                        continue;
                                    }
                                    if(!isConnected)
                                        continue;
                                    notifyReceived(object);
                                }
                            }else{
                                if(udp.readFromAddress() == null)
                                    continue;
                                Object object = udp.readObject();
                                if(object == null)
                                    continue;
                                notifyReceived(object);
                            }
                        }
                        if((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE)
                            tcp.writeOperation();
                    }catch(CancelledKeyException ignored){
                        // Connection is closed.
                    }
                }
            }
        }
        if(isConnected){
            long time = System.currentTimeMillis();
            if(tcp.isTimedOut(time)){
                close();
            }else
                keepAlive();
            if(isIdle())
                notifyIdle();
        }
    }

    void keepAlive(){
        if(!isConnected) return;
        long time = System.currentTimeMillis();
        if(tcp.needsKeepAlive(time)) sendTCP(FrameworkMessage.keepAlive);
        if(udp != null && udpRegistered && udp.needsKeepAlive(time)) sendUDP(FrameworkMessage.keepAlive);
    }

    public void handleNetException(ArcNetException ex){
        lastProtocolError = ex;
        close();
        throw ex;
    }

    @Override
    public void run(){
        shutdown = false;
        while(!shutdown){
            try{
                update(250);
            }catch(IOException ex){
                close();
            }catch(ArcNetException ex){
                handleNetException(ex);
            }
        }
    }

    @Override
    public void start(){
        // Try to let any previous update thread stop.
        if(updateThread != null){
            shutdown = true;
            try{
                updateThread.join(5000);
            }catch(InterruptedException ignored){
            }
        }
        updateThread = new Thread(this, "Client");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    @Override
    public void stop(){
        if(shutdown)
            return;
        close();
        shutdown = true;
        selector.wakeup();
    }

    @Override
    public void close(){
        super.close(DcReason.closed);
        synchronized(updateLock){ // Blocks to avoid a select while the
            // selector is used to bind the server
            // connection.
        }
        // Select one last time to complete closing the socket.
        if(!isClosed){
            isClosed = true;
            selector.wakeup();
        }
    }

    /** Releases the resources used by this client, which may no longer be used.*/
    public void dispose() throws IOException{
        close();
        selector.close();
    }

    /**
     * An empty object will be sent if the UDP connection is inactive more than
     * the specified milliseconds. Network hardware may keep a translation table
     * of inside to outside IP addresses and a UDP keep alive keeps this table
     * entry from expiring. Set to zero to disable. Defaults to 19000.
     */
    public void setKeepAliveUDP(int keepAliveMillis){
        if(udp == null) throw new IllegalStateException("Not connected via UDP.");
        udp.keepAliveMillis = keepAliveMillis;
    }

    @Override
    public Thread getUpdateThread(){
        return updateThread;
    }

    public NetSerializer getSerialization(){
        return serialization;
    }

    private void broadcast(int udpPort, DatagramSocket socket) throws IOException{
        ByteBuffer dataBuffer = ByteBuffer.allocate(64);
        serialization.write(dataBuffer, new DiscoverHost());
        dataBuffer.flip();
        byte[] data = new byte[dataBuffer.limit()];
        dataBuffer.get(data);
        for(NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())){
            if(!iface.isUp()){
                continue;
            }

            for(InterfaceAddress baseAddress : iface.getInterfaceAddresses()){
                InetAddress address = baseAddress.getBroadcast();
                if(address == null) continue;
                socket.send(new DatagramPacket(data, data.length, address, udpPort));
            }
        }
    }

    /**
     * Broadcasts a UDP message on the LAN to discover any running servers.
     * @param udpPort The UDP port of the server.
     * @param timeoutMillis The number of milliseconds to wait for a response.
     */
    public void discoverHosts(int udpPort, String multicastGroup, int multicastPort, int timeoutMillis, Cons<DatagramPacket> handler, Runnable done){
        final boolean[] isDone = {false};

        //broadcast
        discoverExecutor.submit(() -> {
            try(DatagramSocket socket = new DatagramSocket()){
                socket.setBroadcast(true);
                broadcast(udpPort, socket);
                socket.setSoTimeout(timeoutMillis);
                while(true){
                    DatagramPacket packet = discoveryPacket.get();
                    socket.receive(packet);
                    handler.get(packet);
                }
            }finally{
                synchronized(isDone){
                    if(!isDone[0]){
                        isDone[0] = true;
                        done.run();
                    }
                }
            }
        });

        //multicast
        discoverExecutor.submit(() -> {
            try(DatagramSocket socket = new DatagramSocket()){

                ByteBuffer dataBuffer = ByteBuffer.allocate(64);
                serialization.write(dataBuffer, new DiscoverHost());
                dataBuffer.flip();
                byte[] data = new byte[dataBuffer.limit()];
                dataBuffer.get(data);

                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(multicastGroup), multicastPort));
                socket.setSoTimeout(timeoutMillis);
                while(true){
                    DatagramPacket packet = discoveryPacket.get();
                    socket.receive(packet);
                    handler.get(packet);
                }
            }finally{
                synchronized(isDone){
                    if(!isDone[0]){
                        isDone[0] = true;
                        done.run();
                    }
                }
            }
        });
    }
}
