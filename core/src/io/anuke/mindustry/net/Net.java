package io.anuke.mindustry.net;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.Streamable.*;
import net.jpountz.lz4.*;

import java.io.*;
import java.nio.*;

import static io.anuke.mindustry.Vars.*;

@SuppressWarnings("unchecked")
public class Net{
    private boolean server;
    private boolean active;
    private boolean clientLoaded;
    private @Nullable
    StreamBuilder currentStream;

    private final Array<Object> packetQueue = new Array<>();
    private final ObjectMap<Class<?>, Consumer> clientListeners = new ObjectMap<>();
    private final ObjectMap<Class<?>, BiConsumer<NetConnection, Object>> serverListeners = new ObjectMap<>();
    private final IntMap<StreamBuilder> streams = new IntMap<>();

    private final NetProvider provider;
    private final LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
    private final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();

    public Net(NetProvider provider){
        this.provider = provider;
    }

    /** Display a network error. Call on the graphics thread. */
    public void showError(Throwable e){

        if(!headless){

            Throwable t = e;
            while(t.getCause() != null){
                t = t.getCause();
            }

            String baseError = Strings.getFinalMesage(e);

            String error = baseError == null ? "" : baseError.toLowerCase();
            String type = t.getClass().toString().toLowerCase();
            boolean isError = false;

            if(e instanceof BufferUnderflowException || e instanceof BufferOverflowException){
                error = Core.bundle.get("error.io");
            }else if(error.equals("mismatch")){
                error = Core.bundle.get("error.mismatch");
            }else if(error.contains("port out of range") || error.contains("invalid argument") || (error.contains("invalid") && error.contains("address")) || Strings.parseException(e, true).contains("address associated")){
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
                ui.showException("$error.any", e);
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
     * Sets the client loaded status, or whether it will recieve normal packets from the server.
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
            Call.onKick(con, KickReason.serverClose);
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
        provider.disconnectClient();
        server = false;
        active = false;
    }

    public byte[] compressSnapshot(byte[] input){
        return compressor.compress(input);
    }

    public byte[] decompressSnapshot(byte[] input, int size){
        return decompressor.decompress(input, size);
    }

    /**
     * Starts discovering servers on a different thread.
     * Callback is run on the main libGDX thread.
     */
    public void discoverServers(Consumer<Host> cons, Runnable done){
        provider.discoverServers(cons, done);
    }

    /**
     * Returns a list of all connections IDs.
     */
    public Iterable<NetConnection> getConnections(){
        return (Iterable<NetConnection>)provider.getConnections();
    }

    /** Send an object to all connected clients, or to the server if this is a client.*/
    public void send(Object object, SendMode mode){
        if(server){
            for(NetConnection con : provider.getConnections()){
                con.send(object, mode);
            }
        }else{
            provider.sendClient(object, mode);
        }
    }

    /** Send an object to everyone EXCEPT a certain client. Server-side only.*/
    public void sendExcept(NetConnection except, Object object, SendMode mode){
        for(NetConnection con : getConnections()){
            if(con != except){
                con.send(object, mode);
            }
        }
    }

    public @Nullable StreamBuilder getCurrentStream(){
        return currentStream;
    }

    /**
     * Registers a client listener for when an object is recieved.
     */
    public <T> void handleClient(Class<T> type, Consumer<T> listener){
        clientListeners.put(type, listener);
    }

    /**
     * Registers a server listener for when an object is recieved.
     */
    public <T> void handleServer(Class<T> type, BiConsumer<NetConnection, T> listener){
        serverListeners.put(type, (BiConsumer<NetConnection, Object>)listener);
    }

    /**
     * Call to handle a packet being recieved for the client.
     */
    public void handleClientReceived(Object object){

        if(object instanceof StreamBegin){
            StreamBegin b = (StreamBegin)object;
            streams.put(b.id, currentStream = new StreamBuilder(b));

        }else if(object instanceof StreamChunk){
            StreamChunk c = (StreamChunk)object;
            StreamBuilder builder = streams.get(c.id);
            if(builder == null){
                throw new RuntimeException("Recieved stream chunk without a StreamBegin beforehand!");
            }
            builder.add(c.data);
            if(builder.isDone()){
                streams.remove(builder.id);
                handleClientReceived(builder.build());
                currentStream = null;
            }
        }else if(clientListeners.get(object.getClass()) != null){

            if(clientLoaded || ((object instanceof Packet) && ((Packet)object).isImportant())){
                if(clientListeners.get(object.getClass()) != null)
                    clientListeners.get(object.getClass()).accept(object);
                Pools.free(object);
            }else if(!((object instanceof Packet) && ((Packet)object).isUnimportant())){
                packetQueue.add(object);
            }else{
                Pools.free(object);
            }
        }else{
            Log.err("Unhandled packet type: '{0}'!", object);
        }
    }

    /**
     * Call to handle a packet being recieved for the server.
     */
    public void handleServerReceived(NetConnection connection, Object object){

        if(serverListeners.get(object.getClass()) != null){
            if(serverListeners.get(object.getClass()) != null)
                serverListeners.get(object.getClass()).accept(connection, object);
            Pools.free(object);
        }else{
            Log.err("Unhandled packet type: '{0}'!", object.getClass());
        }
    }

    /**
     * Pings a host in an new thread. If an error occured, failed() should be called with the exception.
     */
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed){
        provider.pingHost(address, port, valid, failed);
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

    public enum SendMode{
        tcp, udp
    }

    /** Networking implementation. */
    public interface NetProvider{
        /** Connect to a server. */
        void connectClient(String ip, int port, Runnable success) throws IOException;

        /** Send an object to the server. */
        void sendClient(Object object, SendMode mode);

        /** Disconnect from the server. */
        void disconnectClient();

        /**
         * Discover servers. This should run the callback regardless of whether any servers are found. Should not block.
         * Callback should be run on the main thread.
         * @param done is the callback that should run after discovery.
         */
        void discoverServers(Consumer<Host> callback, Runnable done);

        /** Ping a host. If an error occured, failed() should be called with the exception. */
        void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed);

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
