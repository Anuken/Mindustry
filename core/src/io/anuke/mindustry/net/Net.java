package io.anuke.mindustry.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.net.Packets.StreamBegin;
import io.anuke.mindustry.net.Packets.StreamChunk;
import io.anuke.mindustry.net.Streamable.StreamBuilder;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.BiConsumer;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Pooling;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Net{
    private static boolean server;
    private static boolean active;
    private static boolean clientLoaded;
    private static String lastIP;
    private static Array<Object> packetQueue = new Array<>();
    private static ObjectMap<Class<?>, Consumer> clientListeners = new ObjectMap<>();
    private static ObjectMap<Class<?>, BiConsumer<Integer, Object>> serverListeners = new ObjectMap<>();
    private static ClientProvider clientProvider;
    private static ServerProvider serverProvider;

    private static IntMap<StreamBuilder> streams = new IntMap<>();

    public static boolean hasClient(){
        return clientProvider != null;
    }

    public static boolean hasServer(){
        return serverProvider != null;
    }

    /**Display a network error. Call on the graphics thread.*/
    public static void showError(Throwable e){

        if(!headless){

            Throwable t = e;
            while(t.getCause() != null){
                t = t.getCause();
            }

            String error = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            String type = t.getClass().toString().toLowerCase();

            if(error.equals("mismatch")){
                error = Bundles.get("text.error.mismatch");
            }else if(error.contains("port out of range") || error.contains("invalid argument") || (error.contains("invalid") && error.contains("address"))){
                error = Bundles.get("text.error.invalidaddress");
            }else if(error.contains("connection refused") || error.contains("route to host") || type.contains("unknownhost")){
                error = Bundles.get("text.error.unreachable");
            }else if(type.contains("timeout")){
                error = Bundles.get("text.error.timedout");
            }else if(error.equals("alreadyconnected")){
                error = Bundles.get("text.error.alreadyconnected");
            }else if(!error.isEmpty()){
                error = Bundles.get("text.error.any");
            }

            ui.showText("", Bundles.format("text.connectfail", error));
            ui.loadfrag.hide();

            if(Net.client()){
                netClient.disconnectQuietly();
            }
        }

        Log.err(e);
    }

    /**
     * Sets the client loaded status, or whether it will recieve normal packets from the server.
     */
    public static void setClientLoaded(boolean loaded){
        clientLoaded = loaded;

        if(loaded){
            //handle all packets that were skipped while loading
            for(int i = 0; i < packetQueue.size; i++){
                Log.info("Processing {0} packet post-load.", packetQueue.get(i).getClass());
                handleClientReceived(packetQueue.get(i));
            }
        }
        //clear inbound packet queue
        packetQueue.clear();
    }

    /**
     * Connect to an address.
     */
    public static void connect(String ip, int port, Runnable success){
        try{
            lastIP = ip + ":" + port;
            if(!active){
                clientProvider.connect(ip, port, success);
                active = true;
                server = false;
            }else{
                throw new IOException("alreadyconnected");
            }
        }catch(IOException e){
            showError(e);
        }
    }

    /**Returns the last IP connected to.*/
    public static String getLastIP() {
        return lastIP;
    }

    /**
     * Host a server at an address.
     */
    public static void host(int port) throws IOException{
        serverProvider.host(port);
        active = true;
        server = true;

        Timers.runTask(60f, Platform.instance::updateRPC);
    }

    /**
     * Closes the server.
     */
    public static void closeServer(){
        for(NetConnection con : getConnections()){
            Call.onKick(con.id, KickReason.serverClose);
        }

        serverProvider.close();
        server = false;
        active = false;
    }

    public static void disconnect(){
        clientProvider.disconnect();
        server = false;
        active = false;
    }

    public static byte[] compressSnapshot(byte[] input){
        return serverProvider.compressSnapshot(input);
    }

    public static byte[] decompressSnapshot(byte[] input, int size){
        return clientProvider.decompressSnapshot(input, size);
    }

    /**
     * Starts discovering servers on a different thread.
     * Callback is run on the main libGDX thread.
     */
    public static void discoverServers(Consumer<Host> cons, Runnable done){
        clientProvider.discover(cons, done);
    }

    /**
     * Returns a list of all connections IDs.
     */
    public static Array<NetConnection> getConnections(){
        return (Array<NetConnection>) serverProvider.getConnections();
    }

    /**
     * Returns a connection by ID
     */
    public static NetConnection getConnection(int id){
        return serverProvider.getByID(id);
    }

    /**
     * Send an object to all connected clients, or to the server if this is a client.
     */
    public static void send(Object object, SendMode mode){
        if(server){
            if(serverProvider != null) serverProvider.send(object, mode);
        }else{
            if(clientProvider != null) clientProvider.send(object, mode);
        }
    }

    /**
     * Send an object to a certain client. Server-side only
     */
    public static void sendTo(int id, Object object, SendMode mode){
        serverProvider.sendTo(id, object, mode);
    }

    /**
     * Send an object to everyone EXCEPT certain client. Server-side only
     */
    public static void sendExcept(int id, Object object, SendMode mode){
        serverProvider.sendExcept(id, object, mode);
    }

    /**
     * Send a stream to a specific client. Server-side only.
     */
    public static void sendStream(int id, Streamable stream){
        serverProvider.sendStream(id, stream);
    }

    /**
     * Sets the net clientProvider, e.g. what handles sending, recieving and connecting to a server.
     */
    public static void setClientProvider(ClientProvider provider){
        Net.clientProvider = provider;
    }

    /**
     * Sets the net serverProvider, e.g. what handles hosting a server.
     */
    public static void setServerProvider(ServerProvider provider){
        Net.serverProvider = provider;
    }

    /**
     * Registers a client listener for when an object is recieved.
     */
    public static <T> void handleClient(Class<T> type, Consumer<T> listener){
        clientListeners.put(type, listener);
    }

    /**
     * Registers a server listener for when an object is recieved.
     */
    public static <T> void handleServer(Class<T> type, BiConsumer<Integer, T> listener){
        serverListeners.put(type, (BiConsumer<Integer, Object>) listener);
    }

    /**
     * Call to handle a packet being recieved for the client.
     */
    public static void handleClientReceived(Object object){

        if(object instanceof StreamBegin){
            StreamBegin b = (StreamBegin) object;
            streams.put(b.id, new StreamBuilder(b));
        }else if(object instanceof StreamChunk){
            StreamChunk c = (StreamChunk) object;
            StreamBuilder builder = streams.get(c.id);
            if(builder == null){
                throw new RuntimeException("Recieved stream chunk without a StreamBegin beforehand!");
            }
            builder.add(c.data);
            if(builder.isDone()){
                streams.remove(builder.id);
                handleClientReceived(builder.build());
            }
        }else if(clientListeners.get(object.getClass()) != null){

            if(clientLoaded || ((object instanceof Packet) && ((Packet) object).isImportant())){
                if(clientListeners.get(object.getClass()) != null)
                    clientListeners.get(object.getClass()).accept(object);
                Pooling.free(object);
            }else if(!((object instanceof Packet) && ((Packet) object).isUnimportant())){
                packetQueue.add(object);
                Log.info("Queuing packet {0}", object);
            }else{
                Pooling.free(object);
            }
        }else{
            Log.err("Unhandled packet type: '{0}'!", object);
        }
    }

    /**
     * Call to handle a packet being recieved for the server.
     */
    public static void handleServerReceived(int connection, Object object){

        if(serverListeners.get(object.getClass()) != null){
            if(serverListeners.get(object.getClass()) != null)
                serverListeners.get(object.getClass()).accept(connection, object);
            Pooling.free(object);
        }else{
            Log.err("Unhandled packet type: '{0}'!", object.getClass());
        }
    }

    /**
     * Pings a host in an new thread. If an error occured, failed() should be called with the exception.
     */
    public static void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed){
        clientProvider.pingHost(address, port, valid, failed);
    }

    /**
     * Update client ping.
     */
    public static void updatePing(){
        clientProvider.updatePing();
    }

    /**
     * Get the client ping. Only valid after updatePing().
     */
    public static int getPing(){
        return server() ? 0 : clientProvider.getPing();
    }

    /**
     * Whether the net is active, e.g. whether this is a multiplayer game.
     */
    public static boolean active(){
        return active;
    }

    /**
     * Whether this is a server or not.
     */
    public static boolean server(){
        return server && active;
    }

    /**
     * Whether this is a client or not.
     */
    public static boolean client(){
        return !server && active;
    }

    public static void dispose(){
        if(clientProvider != null) clientProvider.dispose();
        if(serverProvider != null) serverProvider.dispose();
        clientProvider = null;
        serverProvider = null;
        server = false;
        active = false;
    }

    public static void http(String url, String method, Consumer<String> listener, Consumer<Throwable> failure){
        http(url, method, null, listener, failure);
    }

    public static void http(String url, String method, String body, Consumer<String> listener, Consumer<Throwable> failure){
        HttpRequest req = new HttpRequestBuilder().newRequest()
        .method(method).url(url).content(body).build();

        Gdx.net.sendHttpRequest(req, new HttpResponseListener(){
            @Override
            public void handleHttpResponse(HttpResponse httpResponse){
                String result = httpResponse.getResultAsString();
                Gdx.app.postRunnable(() -> listener.accept(result));
            }

            @Override
            public void failed(Throwable t){
                Gdx.app.postRunnable(() -> failure.accept(t));
            }

            @Override
            public void cancelled(){
            }
        });
    }

    public enum SendMode{
        tcp, udp
    }

    /**Client implementation.*/
    public interface ClientProvider{
        /**Connect to a server.*/
        void connect(String ip, int port, Runnable success) throws IOException;

        /**Send an object to the server.*/
        void send(Object object, SendMode mode);

        /**Update the ping. Should be done every second or so.*/
        void updatePing();

        /**Get ping in milliseconds. Will only be valid after a call to updatePing.*/
        int getPing();

        /**Disconnect from the server.*/
        void disconnect();

        /**Decompress an input snapshot byte array.*/
        byte[] decompressSnapshot(byte[] input, int size);

        /**
         * Discover servers. This should run the callback regardless of whether any servers are found. Should not block.
         * Callback should be run on libGDX main thread.
         * @param done is the callback that should run after discovery.
         */
        void discover(Consumer<Host> callback, Runnable done);

        /**Ping a host. If an error occured, failed() should be called with the exception.*/
        void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed);

        /**Close all connections.*/
        void dispose();
    }

    /**Server implementation.*/
    public interface ServerProvider{
        /**Host a server at specified port.*/
        void host(int port) throws IOException;

        /**Sends a large stream of data to a specific client.*/
        void sendStream(int id, Streamable stream);

        /**Send an object to everyone connected.*/
        void send(Object object, SendMode mode);

        /**Send an object to a specific client ID.*/
        void sendTo(int id, Object object, SendMode mode);

        /**Send an object to everyone <i>except</i> a client ID.*/
        void sendExcept(int id, Object object, SendMode mode);

        /**Close the server connection.*/
        void close();

        /**Compress an input snapshot byte array.*/
        byte[] compressSnapshot(byte[] input);

        /**Return all connected users.*/
        Array<? extends NetConnection> getConnections();

        /**Returns a connection by ID.*/
        NetConnection getByID(int id);

        /**Close all connections.*/
        void dispose();
    }
}
