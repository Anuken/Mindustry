package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.LagListener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.util.InputStreamSender;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Net.ServerProvider;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Packets.KickPacket;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.net.Registrator;
import io.anuke.mindustry.net.Streamable;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

public class KryoServer implements ServerProvider {
    final boolean debug = false;
    final Server server;
    final ByteSerializer serializer = new ByteSerializer();
    final ByteBuffer buffer = ByteBuffer.allocate(4096);
    final CopyOnWriteArrayList<KryoConnection> connections = new CopyOnWriteArrayList<>();
    final Array<KryoConnection> array = new Array<>();
    SocketServer webServer;

    int lastconnection = 0;

    public KryoServer(){
        server = new Server(4096*2, 2048, connection -> new ByteSerializer()); //TODO tweak
        server.setDiscoveryHandler((datagramChannel, fromAddress) -> {
            ByteBuffer buffer = KryoRegistrator.writeServerData();
            UCore.log("Replying to discover request with buffer of size " + buffer.capacity());
            buffer.position(0);
            datagramChannel.send(buffer, fromAddress);
            return true;
        });

        Listener listener = new Listener(){

            @Override
            public void connected (Connection connection) {
                KryoConnection kn = new KryoConnection(lastconnection ++, connection.getRemoteAddressTCP().toString(), connection);

                Connect c = new Connect();
                c.id = kn.id;
                c.addressTCP = connection.getRemoteAddressTCP().toString();

                connections.add(kn);
                UCore.log("Adding connection #" + kn.id + " to list.");
                Gdx.app.postRunnable(() ->  Net.handleServerReceived(kn.id, c));
            }

            @Override
            public void disconnected (Connection connection) {
                KryoConnection k = getByKryoID(connection.getID());
                if(k == null) return;
                connections.remove(k);

                Disconnect c = new Disconnect();
                c.id = k.id;

                Gdx.app.postRunnable(() -> Net.handleServerReceived(k.id, c));
            }

            @Override
            public void received (Connection connection, Object object) {
                KryoConnection k = getByKryoID(connection.getID());
                if(object instanceof FrameworkMessage || k == null) return;

                Gdx.app.postRunnable(() -> {
                    try{
                        Net.handleServerReceived(k.id, object);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
            }
        };

        if(KryoRegistrator.fakeLag){
            server.addListener(new LagListener(0, KryoRegistrator.fakeLagAmount, listener));
        }else{
            server.addListener(listener);
        }

        register(Registrator.getClasses());
    }

    @Override
    public Array<KryoConnection> getConnections() {
        array.clear();
        for(KryoConnection c : connections){
            array.add(c);
        }
        return array;
    }

    @Override
    public void kick(int connection) {
        KryoConnection con = getByID(connection);

        KickPacket p = new KickPacket();
        p.reason = KickReason.kick;

        con.send(p, SendMode.tcp);
        Timers.runTask(1f, con::close);
    }

    @Override
    public void host(int port) throws IOException {
        lastconnection = 0;
        connections.clear();
        server.bind(port, port);
        webServer = new SocketServer(Vars.webPort);
        webServer.start();

        Thread thread = new Thread(() -> {
            try{
                server.run();
            }catch (Exception e){
                handleException(e);
            }
        }, "Kryonet Server");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() {
        UCore.setPrivate(server, "shutdown", true);
        connections.clear();
        lastconnection = 0;

        Thread thread = new Thread(() ->{
            try {
                server.close();
                try {
                    if (webServer != null) webServer.stop(1); //please die, right now
                }catch(Exception e){
                    e.printStackTrace();
                }
                //kill them all
                for(Thread worker : Thread.getAllStackTraces().keySet()){
                    if(worker.getName().contains("WebSocketWorker")){
                        worker.interrupt();
                    }
                }
            }catch (Exception e){
                Gdx.app.postRunnable(() -> {throw new RuntimeException(e);});
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void sendStream(int id, Streamable stream) {
        KryoConnection connection = getByID(id);
        if(connection == null) return;
        try {

            if (connection.connection != null) {

                connection.connection.addListener(new InputStreamSender(stream.stream, 512) {
                    int id;

                    protected void start() {
                        //send an object so the receiving side knows how to handle the following chunks
                        StreamBegin begin = new StreamBegin();
                        begin.total = stream.stream.available();
                        begin.type = stream.getClass();
                        connection.connection.sendTCP(begin);
                        id = begin.id;
                    }

                    protected Object next(byte[] bytes) {
                        StreamChunk chunk = new StreamChunk();
                        chunk.id = id;
                        chunk.data = bytes;
                        return chunk; //wrap the byte[] with an object so the receiving side knows how to handle it.
                    }
                });
            } else {
                int cid;
                StreamBegin begin = new StreamBegin();
                begin.total = stream.stream.available();
                begin.type = stream.getClass();
                connection.send(begin, SendMode.tcp);
                cid = begin.id;

                while (stream.stream.available() > 0) {
                    byte[] bytes = new byte[Math.min(512, stream.stream.available())];
                    stream.stream.read(bytes);

                    StreamChunk chunk = new StreamChunk();
                    chunk.id = cid;
                    chunk.data = bytes;
                    connection.send(chunk, SendMode.tcp);
                }
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(Object object, SendMode mode) {
        for(int i = 0; i < connections.size(); i ++){
            connections.get(i).send(object, mode);
        }
    }

    @Override
    public void sendTo(int id, Object object, SendMode mode) {
        NetConnection conn = getByID(id);
        if(conn == null) throw new RuntimeException("Unable to find connection with ID " + id + "!");
        conn.send(object, mode);
    }

    @Override
    public void sendExcept(int id, Object object, SendMode mode) {
        for(int i = 0; i < connections.size(); i ++){
            KryoConnection conn = connections.get(i);
            if(conn.id != id) conn.send(object, mode);
        }
    }

    @Override
    public int getPingFor(NetConnection con) {
        KryoConnection k = (KryoConnection)con;
        return k.connection == null ? 0 : k.connection.getReturnTripTime();
    }

    @Override
    public void register(Class<?>... types) { }

    @Override
    public void dispose(){
        try {
            server.dispose();
            if(webServer != null) webServer.stop(1);
            //kill them all
            for(Thread thread : Thread.getAllStackTraces().keySet()){
                if(thread.getName().contains("WebSocketWorker")){
                    thread.interrupt();
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private void handleException(Exception e){
        Gdx.app.postRunnable(() -> { throw new RuntimeException(e);});
    }

    KryoConnection getByID(int id){
        for(int i = 0; i < connections.size(); i ++){
            KryoConnection con = connections.get(i);
            if(con.id == id){
                return con;
            }
        }

        return null;
    }

    KryoConnection getByKryoID(int id){
        for(int i = 0; i < connections.size(); i ++){
            KryoConnection con = connections.get(i);
            if(con.connection != null && con.connection.getID() == id){
                return con;
            }
        }

        return null;
    }

    KryoConnection getBySocket(WebSocket socket){
        for(int i = 0; i < connections.size(); i ++){
            KryoConnection con = connections.get(i);
            if(con.socket == socket){
                return con;
            }
        }

        return null;
    }

    class KryoConnection extends NetConnection{
        public final WebSocket socket;
        public final Connection connection;

        public KryoConnection(int id, String address, WebSocket socket) {
            super(id, address);
            this.socket = socket;
            this.connection = null;
        }

        public KryoConnection(int id, String address, Connection connection) {
            super(id, address);
            this.socket = null;
            this.connection = connection;
        }

        @Override
        public void send(Object object, SendMode mode){
            if(socket != null){
                try {
                    synchronized (buffer) {
                        buffer.position(0);
                        if(debug) UCore.log("Sending object with ID " + Registrator.getID(object.getClass()));
                        serializer.write(buffer, object);
                        int pos = buffer.position();
                        buffer.position(0);
                        byte[] out = new byte[pos];
                        buffer.get(out);
                        String string = new String(Base64Coder.encode(out));
                        if(debug) UCore.log("Sending string: " + string);
                        socket.send(string);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    connections.remove(this);
                }
            }else if (connection != null) {
                if (mode == SendMode.tcp) {
                    connection.sendTCP(object);
                } else {
                    connection.sendUDP(object);
                }
            }
        }

        @Override
        public void close(){
            if(socket != null){
                if(socket.isOpen()) socket.close();
            }else if (connection != null) {
                if(connection.isConnected()) connection.close();
            }
        }

    }

    class SocketServer extends WebSocketServer {

        public SocketServer(int port) {
            super(new InetSocketAddress(port));
            //setWebSocketFactory(factory);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Connect connect = new Connect();
            connect.addressTCP = conn.getRemoteSocketAddress().toString();
            UCore.log("Websocket connection recieved: " + connect.addressTCP);
            KryoConnection kn = new KryoConnection(lastconnection ++, connect.addressTCP, conn);
            connections.add(kn);
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            if (conn == null) return;
            KryoConnection k = getBySocket(conn);
            if(k == null) return;
            Disconnect disconnect = new Disconnect();
            disconnect.id = k.id;
            Net.handleServerReceived(k.id, disconnect);
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            try {
                KryoConnection k = getBySocket(conn);
                if (k == null) return;

                if(message.equals("_ping_")){
                    conn.send(connections.size() + "|" + Vars.player.name);
                    connections.remove(k);
                }else {
                    if (debug) UCore.log("Got message: " + message);

                    byte[] out = Base64Coder.decode(message);
                    if (debug) UCore.log("Decoded: " + Arrays.toString(out));
                    ByteBuffer buffer = ByteBuffer.wrap(out);
                    Object o = serializer.read(buffer);
                    Net.handleServerReceived(k.id, o);
                }
            }catch (Exception e){
                UCore.log("Error reading message!");
                e.printStackTrace();
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            UCore.log("WS error:");
            ex.printStackTrace();
            if(ex instanceof BindException){
                Net.closeServer();
                Vars.ui.showError("$text.server.addressinuse");
            }else if(ex.getMessage().equals("Permission denied")){
                Net.closeServer();
                Vars.ui.showError("Permission denied.");
            }
        }

        @Override
        public void onStart() {
            UCore.log("Web server started.");
        }
    }

}
