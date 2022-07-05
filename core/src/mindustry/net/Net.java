package mindustry.net;

import arc.*;
import arc.func.*;
import arc.net.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.net.Packets.*;
import mindustry.net.Streamable.*;
import net.jpountz.lz4.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;

@SuppressWarnings("unchecked")
public class Net{
    private static Seq<Prov<? extends Packet>> packetProvs = new Seq<>();
    private static Seq<Class<? extends Packet>> packetClasses = new Seq<>();
    private static ObjectIntMap<Class<?>> packetToId = new ObjectIntMap<>();

    private boolean server;
    private boolean active;
    private boolean clientLoaded;
    private @Nullable StreamBuilder currentStream;

    private final Seq<Packet> packetQueue = new Seq<>();
    private final ObjectMap<Class<?>, Cons> clientListeners = new ObjectMap<>();
    private final ObjectMap<Class<?>, Cons2<NetConnection, Object>> serverListeners = new ObjectMap<>();
    private final IntMap<StreamBuilder> streams = new IntMap<>();
    private final ExecutorService pingExecutor = Threads.unboundedExecutor();

    private final NetProvider provider;

    static{
        registerPacket(StreamBegin::new);
        registerPacket(StreamChunk::new);
        registerPacket(WorldStream::new);
        registerPacket(ConnectPacket::new);

        //register generated packet classes
        Call.registerPackets();
    }

    /** Registers a new packet type for serialization. */
    public static <T extends Packet> void registerPacket(Prov<T> cons){
        packetProvs.add(cons);
        var t = cons.get();
        packetClasses.add(t.getClass());
        packetToId.put(t.getClass(), packetProvs.size - 1);
    }

    public static byte getPacketId(Packet packet){
        int id = packetToId.get(packet.getClass(), -1);
        if(id == -1) throw new ArcRuntimeException("Unknown packet type: " + packet.getClass());
        return (byte)id;
    }

    public static <T extends Packet> T newPacket(byte id){
        return ((Prov<T>)packetProvs.get(id & 0xff)).get();
    }

    public Net(NetProvider provider){
        this.provider = provider;
    }

    public void handleException(Throwable e){
        if(e instanceof ArcNetException){
            Core.app.post(() -> showError(new IOException("mismatch", e)));
        }else if(e instanceof ClosedChannelException){
            Core.app.post(() -> showError(new IOException("alreadyconnected", e)));
        }else{
            Core.app.post(() -> showError(e));
        }
    }

    /** Display a network error. Call on the graphics thread. */
    public void showError(Throwable e){

        if(!headless){

            Throwable t = e;
            while(t.getCause() != null){
                t = t.getCause();
            }

            String baseError = Strings.getFinalMessage(e);

            String error = baseError == null ? "" : baseError.toLowerCase();
            String type = t.getClass().toString().toLowerCase();
            boolean isError = false;

            if(e instanceof BufferUnderflowException || e instanceof BufferOverflowException || e.getCause() instanceof EOFException){
                error = Core.bundle.get("error.io");
            }else if(error.equals("mismatch") || e instanceof LZ4Exception || (e instanceof IndexOutOfBoundsException && e.getStackTrace().length > 0 && e.getStackTrace()[0].getClassName().contains("java.nio"))){
                error = Core.bundle.get("error.mismatch");
            }else if(error.contains("port out of range") || error.contains("invalid argument") || (error.contains("invalid") && error.contains("address")) || Strings.neatError(e).contains("address associated")){
                error = Core.bundle.get("error.invalidaddress");
            }else if(error.contains("connection refused") || error.contains("route to host") || type.contains("unknownhost")){
                error = Core.bundle.get("error.unreachable");
            }else if(type.contains("timeout")){
                error = Core.bundle.get("error.timedout");
            }else if(error.equals("alreadyconnected") || error.contains("connection is closed")){
                error = Core.bundle.get("error.alreadyconnected");
            }else if(!error.isEmpty()){
                error = Core.bundle.get("error.any");
                isError = true;
            }

            if(isError){
                ui.showException("@error.any", e);
            }else{
                ui.showText("", Core.bundle.format("connectfail", error));
            }
            ui.loadfrag.hide();

            if(client()){
                netClient.disconnectQuietly();
            }
        }

        Log.err(e);
    }

    /**
     * Sets the client loaded status, or whether it will receive normal packets from the server.
     */
    public void setClientLoaded(boolean loaded){
        clientLoaded = loaded;

        if(loaded){
            //handle all packets that were skipped while loading
            for(int i = 0; i < packetQueue.size; i++){
                handleClientReceived(packetQueue.get(i));
            }
        }
        //clear inbound packet queue
        packetQueue.clear();
    }

    public void setClientConnected(){
        active = true;
        server = false;
    }

    /**
     * Connect to an address.
     */
    public void connect(String ip, int port, Runnable success){
        try{
            if(!active){
                provider.connectClient(ip, port, success);
                active = true;
                server = false;
            }else{
                throw new IOException("alreadyconnected");
            }
        }catch(IOException e){
            showError(e);
        }
    }

    /**
     * Host a server at an address.
     */
    public void host(int port) throws IOException{
        provider.hostServer(port);
        active = true;
        server = true;

        Time.runTask(60f, platform::updateRPC);
    }

    /**
     * Closes the server.
     */
    public void closeServer(){
        for(NetConnection con : getConnections()){
            Call.kick(con, KickReason.serverClose);
        }

        provider.closeServer();
        server = false;
        active = false;
    }

    public void reset(){
        closeServer();
        netClient.disconnectNoReset();
    }

    public void disconnect(){
        if(active && !server){
            Log.info("Disconnecting.");
        }
        provider.disconnectClient();
        server = false;
        active = false;
    }

    /**
     * Starts discovering servers on a different thread.
     * Callback is run on the main Arc thread.
     */
    public void discoverServers(Cons<Host> cons, Runnable done){
        provider.discoverServers(cons, done);
    }

    /**
     * Returns a list of all connections IDs.
     */
    public Iterable<NetConnection> getConnections(){
        return (Iterable<NetConnection>)provider.getConnections();
    }

    /** Send an object to all connected clients, or to the server if this is a client.*/
    public void send(Object object, boolean reliable){
        if(server){
            for(NetConnection con : provider.getConnections()){
                con.send(object, reliable);
            }
        }else{
            provider.sendClient(object, reliable);
        }
    }

    /** Send an object to everyone EXCEPT a certain client. Server-side only.*/
    public void sendExcept(NetConnection except, Object object, boolean reliable){
        for(NetConnection con : getConnections()){
            if(con != except){
                con.send(object, reliable);
            }
        }
    }

    public @Nullable StreamBuilder getCurrentStream(){
        return currentStream;
    }

    /**
     * Registers a client listener for when an object is received.
     */
    public <T> void handleClient(Class<T> type, Cons<T> listener){
        clientListeners.put(type, listener);
    }

    /**
     * Registers a server listener for when an object is received.
     */
    public <T> void handleServer(Class<T> type, Cons2<NetConnection, T> listener){
        serverListeners.put(type, (Cons2<NetConnection, Object>)listener);
    }

    /**
     * Call to handle a packet being received for the client.
     */
    public void handleClientReceived(Packet object){
        object.handled();

        if(object instanceof StreamBegin b){
            streams.put(b.id, currentStream = new StreamBuilder(b));

        }else if(object instanceof StreamChunk c){
            StreamBuilder builder = streams.get(c.id);
            if(builder == null){
                throw new RuntimeException("Received stream chunk without a StreamBegin beforehand!");
            }
            builder.add(c.data);

            ui.loadfrag.setProgress(builder.progress());
            ui.loadfrag.snapProgress();
            netClient.resetTimeout();

            if(builder.isDone()){
                streams.remove(builder.id);
                handleClientReceived(builder.build());
                currentStream = null;
            }
        }else{
            int p = object.getPriority();

            if(clientLoaded || p == Packet.priorityHigh){
                if(clientListeners.get(object.getClass()) != null){
                    clientListeners.get(object.getClass()).get(object);
                }else{
                    object.handleClient();
                }
            }else if(p != Packet.priorityLow){
                packetQueue.add(object);
            }
        }
    }

    /**
     * Call to handle a packet being received for the server.
     */
    public void handleServerReceived(NetConnection connection, Packet object){
        object.handled();

        try{
            //handle object normally
            if(serverListeners.get(object.getClass()) != null){
                serverListeners.get(object.getClass()).get(connection, object);
            }else{
                object.handleServer(connection);
            }
        }catch(ValidateException e){
            //ignore invalid actions
            debug("Validation failed for '@': @", e.player, e.getMessage());
        }catch(RuntimeException e){
            //ignore indirect ValidateException-s
            if(e.getCause() instanceof ValidateException v){
                debug("Validation failed for '@': @", v.player, v.getMessage());
            }else{
                //rethrow if not ValidateException
                throw e;
            }
        }
    }

    /**
     * Pings a host in a pooled thread. If an error occurred, failed() should be called with the exception.
     * If the port is the default mindustry port, SRV records are checked too.
     */
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed){
        pingExecutor.submit(() -> provider.pingHost(address, port, valid, failed));
    }

    /**
     * Whether the net is active, e.g. whether this is a multiplayer game.
     */
    public boolean active(){
        return active;
    }

    /**
     * Whether this is a server or not.
     */
    public boolean server(){
        return server && active;
    }

    /**
     * Whether this is a client or not.
     */
    public boolean client(){
        return !server && active;
    }

    public void dispose(){
        provider.dispose();
        server = false;
        active = false;
    }

    /** Networking implementation. */
    public interface NetProvider{
        /** Connect to a server. */
        void connectClient(String ip, int port, Runnable success) throws IOException;

        /** Send an object to the server. */
        void sendClient(Object object, boolean reliable);

        /** Disconnect from the server. */
        void disconnectClient();

        /**
         * Discover servers. This should run the callback regardless of whether any servers are found. Should not block.
         * Callback should be run on the main thread.
         * @param done is the callback that should run after discovery.
         */
        void discoverServers(Cons<Host> callback, Runnable done);

        /**
         * Ping a host. If an error occurred, failed() should be called with the exception. This method should block.
         * If the port is the default mindustry port (6567), SRV records are checked too.
         */
        void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed);

        /** Host a server at specified port. */
        void hostServer(int port) throws IOException;

        /** Return all connected users. */
        Iterable<? extends NetConnection> getConnections();

        /** Close the server connection. */
        void closeServer();

        /** Close all connections. */
        default void dispose(){
            disconnectClient();
            closeServer();
        }
    }
}
