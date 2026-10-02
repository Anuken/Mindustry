package arc.net;

import arc.math.*;
import arc.net.FrameworkMessage.*;
import arc.struct.*;
import arc.util.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manages TCP and optionally UDP connections from many {@linkplain Client
 * Clients}.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Server implements EndPoint{
    private final NetSerializer serializer;
    private final int writeBufferSize, objectBufferSize;
    private final Selector selector;
    private int emptySelects;
    private ServerSocketChannel serverChannel;
    private UdpConnection udp;
    private Connection[] connections = {};
    private ConcurrentHashMap<InetSocketAddress, Connection> udpAddressToConnection = new ConcurrentHashMap<>();
    private IntMap<Connection> pendingConnections = new IntMap<>();
    NetListener[] listeners = {};
    private final Object listenerLock = new Object();
    private volatile boolean shutdown;
    private final Object updateLock = new Object();
    private Thread updateThread;
    private int multicastPort = 21010;
    protected InetAddress multicastGroup;
    protected DiscoveryReceiver discoveryReceiver;
    protected ServerDiscoveryHandler discoveryHandler;
    private ServerConnectFilter connectFilter;

    private final ByteBuffer bulkWriteBuffer;
    private final Object bulkWriteLock = new Object();

    private NetListener dispatchListener = new NetListener(){
        @Override
        public void connected(Connection connection){
            NetListener[] listeners = Server.this.listeners;
            for(NetListener listener : listeners) listener.connected(connection);
        }

        @Override
        public void disconnected(Connection connection, DcReason reason){
            removeConnection(connection);
            NetListener[] listeners = Server.this.listeners;
            for(NetListener listener : listeners) listener.disconnected(connection, reason);
        }

        @Override
        public void received(Connection connection, Object object){
            NetListener[] listeners = Server.this.listeners;
            for(NetListener listener : listeners) listener.received(connection, object);
        }

        @Override
        public void idle(Connection connection){
            NetListener[] listeners = Server.this.listeners;
            for(NetListener listener : listeners) listener.idle(connection);
        }
    };

    /**
     * @param writeBufferSize One buffer of this size is allocated for each connected
     * client. Objects are serialized to the write buffer where the
     * bytes are queued until they can be written to the TCP socket.
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
    public Server(int writeBufferSize, int objectBufferSize, NetSerializer serializer){
        this.writeBufferSize = writeBufferSize;
        this.objectBufferSize = objectBufferSize;
        this.serializer = serializer;

        bulkWriteBuffer = ByteBuffer.allocateDirect(writeBufferSize);

        this.discoveryHandler = (address, handler) -> handler.respond(ByteBuffer.allocate(0));

        try{
            selector = Selector.open();
        }catch(IOException ex){
            throw new RuntimeException("Error opening the selector.", ex);
        }
    }

    public void setMulticast(String group, int multicastPort){
        this.multicastPort = multicastPort;
        try{
            multicastGroup = InetAddress.getByName(group);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void setDiscoveryHandler(ServerDiscoveryHandler newDiscoveryHandler){
        discoveryHandler = newDiscoveryHandler;
    }

    public void setConnectFilter(ServerConnectFilter connectFilter){
        this.connectFilter = connectFilter;
    }

    public ServerConnectFilter getConnectFilter(){
        return connectFilter;
    }

    /**
     * Opens a TCP only server.
     * @throws IOException if the server could not be opened.
     */
    public void bind(int tcpPort) throws IOException{
        bind(new InetSocketAddress(tcpPort), null);
    }

    /**
     * Opens a TCP and UDP server. All clients must also have a TCP and an UDP
     * port.
     * @throws IOException if the server could not be opened.
     */
    public void bind(int tcpPort, int udpPort) throws IOException{
        bind(new InetSocketAddress(tcpPort), new InetSocketAddress(udpPort));
    }

    /**
     * @param udpPort May be null.
     */
    public void bind(InetSocketAddress tcpPort, InetSocketAddress udpPort) throws IOException{
        close();
        synchronized(updateLock){
            selector.wakeup();
            try{
                serverChannel = selector.provider().openServerSocketChannel();
                serverChannel.socket().bind(tcpPort);
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                if(udpPort != null){
                    udp = new UdpConnection(
                    serializer,
                    objectBufferSize);
                    udp.bind(selector, udpPort);
                }

                if(multicastGroup != null && (udpPort == null || multicastPort != udpPort.getPort())){
                    discoveryReceiver = new DiscoveryReceiver(multicastPort);
                    discoveryReceiver.start();
                }
            }catch(IOException ex){
                close();
                throw ex;
            }
        }
    }

    /**
     * Accepts any new connections and reads or writes any pending data for the
     * current connections.
     * @param timeout Wait for up to the specified milliseconds for a connection to
     * be ready to process. May be zero to return immediately if
     * there are no connections to process.
     */
    @Override
    public void update(int timeout) throws IOException{
        updateThread = Thread.currentThread();
        synchronized(updateLock){ // Blocks to avoid a select while the
            // selector is used to bind the server
            // connection.
        }
        long startTime = System.currentTimeMillis();
        int select;
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
                    if(elapsedTime < 25) Thread.sleep(25 - elapsedTime);
                }catch(InterruptedException ignored){
                }
            }
        }else{
            emptySelects = 0;
            Set<SelectionKey> keys = selector.selectedKeys();
            synchronized(keys){
                UdpConnection udp = this.udp;
                for(Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();){
                    SelectionKey selectionKey = iter.next();
                    iter.remove();
                    Connection fromConnection = (Connection)selectionKey.attachment();
                    try{
                        int ops = selectionKey.readyOps();

                        if(fromConnection != null){ // Must be a TCP read or
                            // write operation.
                            if(udp != null && fromConnection.udpRemoteAddress == null){
                                fromConnection.close(DcReason.error);
                                continue;
                            }
                            if((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ){
                                try{
                                    while(true){
                                        Object object = fromConnection.tcp.readObject();
                                        if(object == null)
                                            break;
                                        fromConnection.notifyReceived(object);
                                    }
                                }catch(IOException | ArcNetException ex){
                                    ArcNet.handleError(new ArcNetException("Error reading TCP from connection: " + fromConnection, ex));
                                    fromConnection.close(ex.getMessage() != null && ex.getMessage().contains("closed") ? DcReason.closed : DcReason.error);
                                }
                            }
                            if((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE){
                                try{
                                    fromConnection.tcp.writeOperation();
                                }catch(IOException ex){
                                    fromConnection.close(ex.getMessage() != null && ex.getMessage().contains("closed") ? DcReason.closed : DcReason.error);
                                }
                            }
                            continue;
                        }

                        if((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT){
                            ServerSocketChannel serverChannel = this.serverChannel;
                            if(serverChannel == null)
                                continue;
                            try{
                                SocketChannel socketChannel = serverChannel.accept();
                                if(socketChannel != null)
                                    acceptOperation(socketChannel);
                            }catch(IOException ex){
                                ArcNet.handleError(ex);
                            }
                            continue;
                        }

                        // Must be a UDP read operation.
                        if(udp == null){
                            selectionKey.channel().close();
                            continue;
                        }
                        InetSocketAddress fromAddress;
                        try{
                            fromAddress = udp.readFromAddress();
                        }catch(IOException ex){
                            ArcNet.handleError(ex);
                            continue;
                        }
                        if(fromAddress == null)
                            continue;

                        fromConnection = udpAddressToConnection.get(fromAddress);

                        Object object;
                        try{
                            object = udp.readObject();
                        }catch(ArcNetException ex){
                            ArcNet.handleError(new ArcNetException("Error reading UDP from connection: " + fromAddress, ex));
                            continue;
                        }

                        if(object instanceof FrameworkMessage){
                            if(object instanceof RegisterUDP){
                                // Store the fromAddress on the connection and
                                // reply over TCP with a RegisterUDP to indicate
                                // success.
                                int fromConnectionID = ((RegisterUDP)object).connectionID;
                                Connection connection = pendingConnections
                                .remove(fromConnectionID);
                                if(connection != null){
                                    if(connection.udpRemoteAddress != null) continue;
                                    connection.udpRemoteAddress = fromAddress;
                                    addConnection(connection);
                                    connection.sendTCP(new RegisterUDP());
                                    connection.notifyConnected();
                                    continue;
                                }
                                continue;
                            }
                            if(object instanceof DiscoverHost){
                                try{
                                    discoveryHandler.onDiscoverReceived(fromAddress.getAddress(), buff -> udp.datagramChannel.send(buff, fromAddress));
                                }catch(IOException ignored){
                                }
                                continue;
                            }
                        }

                        if(fromConnection != null){
                            fromConnection.notifyReceived(object);
                            continue;
                        }
                    }catch(CancelledKeyException ex){
                        if(fromConnection != null)
                            fromConnection.close(DcReason.error);
                        else
                            selectionKey.channel().close();
                    }
                }
            }
        }
        long time = System.currentTimeMillis();
        Connection[] connections = this.connections;
        for(Connection connection : connections){
            if(connection.tcp.isTimedOut(time)){
                connection.close(DcReason.timeout);
            }else{
                if(connection.tcp.needsKeepAlive(time))
                    connection.sendTCP(FrameworkMessage.keepAlive);
            }
            if(connection.isIdle())
                connection.notifyIdle();
        }
    }

    @Override
    public void run(){
        shutdown = false;
        while(!shutdown){
            try{
                update(250);
            }catch(IOException ex){
                close();
            }
        }
    }

    @Override
    public void start(){
        new Thread(this, "Server").start();
    }

    @Override
    public void stop(){
        if(shutdown)
            return;
        shutdown = true;
        close();
    }

    private void acceptOperation(SocketChannel socketChannel){
        if(connectFilter != null){
            try{
                if(!connectFilter.accept(((InetSocketAddress)socketChannel.getRemoteAddress()).getAddress().getHostAddress())){
                    socketChannel.close();
                    return;
                }
            }catch(IOException ignored){}
        }

        Connection connection = newConnection();
        connection.initialize(serializer,
        writeBufferSize, objectBufferSize);
        connection.endPoint = this;
        UdpConnection udp = this.udp;
        if(udp != null)
            connection.udp = udp;
        try{
            SelectionKey selectionKey = connection.tcp.accept(selector, socketChannel);
            selectionKey.attach(connection);

            int id = generateId();
            connection.id = id;
            connection.setConnected(true);
            connection.addListener(dispatchListener);

            if(udp == null)
                addConnection(connection);
            else
                pendingConnections.put(id, connection);

            RegisterTCP registerConnection = new RegisterTCP();
            registerConnection.connectionID = id;
            connection.sendTCP(registerConnection);

            if(udp == null)
                connection.notifyConnected();
        }catch(IOException ex){
            connection.close(DcReason.error);
        }
    }

    private int generateId(){
        int[] id = {0}; //java lambda as just amazing aren't they????
        Rand rand = new Rand(); //not really concerned about allocating an object with two longs
        do{
            id[0] = rand.nextInt();
        }while(pendingConnections.containsKey(id[0]) || Structs.contains(connections, c -> c.id == id[0]));
        return id[0];
    }

    /**
     * Allows the connections used by the server to be subclassed. This can be
     * useful for storage per connection without an additional lookup.
     */
    protected Connection newConnection(){
        return new Connection();
    }

    private void addConnection(Connection connection){
        connections = Structs.add(connections, connection);

        if(connection.udpRemoteAddress != null){
            udpAddressToConnection.put(connection.udpRemoteAddress, connection);
        }
    }

    void removeConnection(Connection connection){
        connections = Structs.remove(connections, connection);

        pendingConnections.remove(connection.id);
        if(connection.udpRemoteAddress != null){
            udpAddressToConnection.remove(connection.udpRemoteAddress);
        }
    }

    public void sendToAllTCP(Object object){
        synchronized(bulkWriteLock){
            ByteBuffer buffer;
            try{
                buffer = serializeTCP(object);
            }catch(Throwable ex){
                ArcNet.handleError(ex);
                return;
            }

            Connection[] connections = this.connections;
            for(Connection con : connections){
                try{
                    con.sendTCPBuffer(buffer);
                }catch(Exception e){
                    ArcNet.handleError(e);
                    con.close(DcReason.error);
                }
            }
        }
    }

    public void sendToAllExceptTCP(int connectionID, Object object){
        synchronized(bulkWriteLock){
            ByteBuffer buffer;
            try{
                buffer = serializeTCP(object);
            }catch(Throwable ex){
                ArcNet.handleError(ex);
                return;
            }

            Connection[] connections = this.connections;
            for(Connection con : connections){
                if(con.id == connectionID) continue;
                try{
                    con.sendTCPBuffer(buffer);
                }catch(Exception e){
                    ArcNet.handleError(e);
                    con.close(DcReason.error);
                }
            }
        }
    }

    /** Sends the object over TCP to every connection in the list, serializing it only once. */
    public void sendToAllTCP(Object object, Iterable<Connection> connections){
        synchronized(bulkWriteLock){
            ByteBuffer buffer;
            try{
                buffer = serializeTCP(object);
            }catch(Throwable ex){
                ArcNet.handleError(ex);
                return;
            }

            for(Connection con : connections){
                if(!con.isConnected()) continue;
                try{
                    con.sendTCPBuffer(buffer);
                }catch(Exception e){
                    ArcNet.handleError(e);
                    con.close(DcReason.error);
                }
            }
        }
    }


    public void sendToAllUDP(Object object){
        if(udp == null) return;

        synchronized(bulkWriteLock){
            ByteBuffer buffer;
            try{
                buffer = serializeUDP(object);
            }catch(Throwable ex){
                ArcNet.handleError(ex);
                return;
            }

            Connection[] connections = this.connections;
            for(Connection con : connections){
                try{
                    con.sendUDPBuffer(buffer);
                }catch(Exception e){
                    //note: 'vanilla' kryonet doesn't do this, but mindustry does this in ArcConnection#send upon error, so it's probably best to close upon error here as well and not let it propagate
                    ArcNet.handleError(e);
                    con.close(DcReason.error);
                }
            }
        }
    }

    public void sendToAllExceptUDP(int connectionID, Object object){
        if(udp == null) return;

        synchronized(bulkWriteLock){
            ByteBuffer buffer;
            try{
                buffer = serializeUDP(object);
            }catch(Throwable ex){
                ArcNet.handleError(ex);
                return;
            }

            Connection[] connections = this.connections;
            for(Connection con : connections){
                if(con.id == connectionID) continue;
                try{
                    con.sendUDPBuffer(buffer);
                }catch(Exception e){
                    ArcNet.handleError(e);
                    con.close(DcReason.error);
                }
            }
        }
    }

    /** Sends the object over UDP to every connection in the list, serializing it only once. */
    public void sendToAllUDP(Object object, Iterable<Connection> connections){
        if(udp == null) return;

        synchronized(bulkWriteLock){
            ByteBuffer buffer;
            try{
                buffer = serializeUDP(object);
            }catch(Throwable ex){
                ArcNet.handleError(ex);
                return;
            }

            for(Connection con : connections){
                if(!con.isConnected()) continue; //note: since this method accepts a list of connections, there may be stale connections, so filter for that (not possible otherwise)
                try{
                    con.sendUDPBuffer(buffer);
                }catch(Exception e){
                    ArcNet.handleError(e);
                    con.close(DcReason.error);
                }
            }
        }
    }

    /** Writes an object to the bulk-write buffer for TCP, including the length prefix. */
    private ByteBuffer serializeTCP(Object object){
        synchronized(bulkWriteLock){
            bulkWriteBuffer.clear();
            int lengthLength = serializer.getLengthLength();
            try{
                bulkWriteBuffer.position(lengthLength);
                serializer.write(bulkWriteBuffer, object);
            }catch(Exception ex){
                throw new ArcNetException("Error serializing object of type: " + object.getClass().getName(), ex);
            }
            int end = bulkWriteBuffer.position();
            bulkWriteBuffer.position(0);
            serializer.writeLength(bulkWriteBuffer, end - lengthLength);
            bulkWriteBuffer.position(end);
            bulkWriteBuffer.flip();
            return bulkWriteBuffer;
        }
    }

    /** Writes an object to the bulk-write buffer. No length prefix. */
    private ByteBuffer serializeUDP(Object object){
        synchronized(bulkWriteLock){
            bulkWriteBuffer.clear();
            try{
                serializer.write(bulkWriteBuffer, object);
            }catch(Exception ex){
                throw new ArcNetException("Error serializing object of type: " + object.getClass().getName(), ex);
            }
            bulkWriteBuffer.flip();
            return bulkWriteBuffer;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Should be called before connect().
     */
    @Override
    public void addListener(NetListener listener){
        if(listener == null)
            throw new IllegalArgumentException("listener cannot be null.");
        synchronized(listenerLock){
            NetListener[] listeners = this.listeners;
            int n = listeners.length;
            for(NetListener netListener : listeners)
                if(listener == netListener)
                    return;
            NetListener[] newListeners = new NetListener[n + 1];
            newListeners[0] = listener;
            System.arraycopy(listeners, 0, newListeners, 1, n);
            this.listeners = newListeners;
        }
    }

    @Override
    public void removeListener(NetListener listener){
        if(listener == null)
            throw new IllegalArgumentException("listener cannot be null.");
        synchronized(listenerLock){
            NetListener[] listeners = this.listeners;
            int n = listeners.length;
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

    /**
     * Closes all open connections and the server port(s).
     */
    @Override
    public void close(){
        Connection[] connections = this.connections;
        for(Connection connection : connections) connection.close(DcReason.closed);
        this.connections = new Connection[0];

        ServerSocketChannel serverChannel = this.serverChannel;
        if(serverChannel != null){
            try{
                serverChannel.close();
            }catch(IOException ignored){
            }
            this.serverChannel = null;
        }

        if(discoveryReceiver != null){
            discoveryReceiver.close();
            discoveryReceiver = null;
        }

        UdpConnection udp = this.udp;
        if(udp != null){
            udp.close();
            this.udp = null;
        }

        synchronized(updateLock){
            // Blocks to avoid a select while the selector is used to bind the server connection.
        }
        // Select one last time to complete closing the socket.
        selector.wakeup();
        try{
            selector.selectNow();
        }catch(IOException ignored){
        }
    }

    /**
     * Releases the resources used by this server, which may no longer be used.
     */
    public void dispose() throws IOException{
        close();
        selector.close();
    }

    @Override
    public Thread getUpdateThread(){
        return updateThread;
    }

    /**
     * Returns the current connections. The array returned should not be
     * modified.
     */
    public Connection[] getConnections(){
        return connections;
    }

    class DiscoveryReceiver{
        MulticastSocket socket = null;
        Thread multicastThread;
        int multicastPort;

        DiscoveryReceiver(int multicastPort){
            this.multicastPort = multicastPort;
        }

        void close(){
            try{
                if(multicastThread != null) multicastThread.interrupt();
                if(socket != null){
                    socket.leaveGroup(multicastGroup);
                    socket.close();
                }
            }catch(IOException e){
                ArcNet.handleError(e);
            }
        }

        void start(){
            multicastThread = Threads.daemon("Server Multicast Discovery", () -> {
                try{
                    socket = new MulticastSocket(multicastPort);
                    socket.joinGroup(multicastGroup);
                    DatagramPacket packet = new DatagramPacket(new byte[512], 512);
                    while(true){
                        socket.receive(packet);
                        discoveryHandler.onDiscoverReceived(packet.getAddress(), buffer -> {
                            byte[] data = buffer.array();
                            DatagramPacket out = new DatagramPacket(data, data.length);
                            out.setSocketAddress(packet.getSocketAddress());
                            socket.send(out);
                        });
                    }
                }catch(IOException e){
                    if(!(e instanceof SocketException && "Socket closed".equals(e.getMessage()))){
                        ArcNet.handleError(e);
                    }
                }
            });
        }
    }

    public interface ServerConnectFilter{
        boolean accept(String address);
    }
}
