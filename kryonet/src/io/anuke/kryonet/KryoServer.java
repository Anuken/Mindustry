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
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Net.ServerProvider;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.Registrator;
import io.anuke.mindustry.net.Streamable;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static io.anuke.mindustry.Vars.headless;
import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public class KryoServer implements ServerProvider {
    final boolean debug = false;
    final Server server;
    final ByteSerializer serializer = new ByteSerializer();
    final ByteBuffer buffer = ByteBuffer.allocate(4096);
    final CopyOnWriteArrayList<KryoConnection> connections = new CopyOnWriteArrayList<>();
    final CopyOnWriteArraySet<Integer> missing = new CopyOnWriteArraySet<>();
    final Array<KryoConnection> array = new Array<>();
    SocketServer webServer;
    Thread serverThread;

    int lastconnection = 0;

    public KryoServer(){
        server = new Server(4096*2, 2048, connection -> new ByteSerializer());
        server.setDiscoveryHandler((datagramChannel, fromAddress) -> {
            ByteBuffer buffer = KryoRegistrator.writeServerData();
            buffer.position(0);
            datagramChannel.send(buffer, fromAddress);
            return true;
        });

        Listener listener = new Listener(){

            @Override
            public void connected (Connection connection) {
                String ip = connection.getRemoteAddressTCP().getAddress().getHostAddress();

                KryoConnection kn = new KryoConnection(lastconnection ++, ip, connection);

                Connect c = new Connect();
                c.id = kn.id;
                c.addressTCP = ip;

                Log.info("&bRecieved connection: {0} / {1}. Kryonet ID: {2}", c.id, c.addressTCP, connection.getID());

                connections.add(kn);
                Gdx.app.postRunnable(() ->  Net.handleServerReceived(kn.id, c));
            }

            @Override
            public void disconnected (Connection connection) {
                KryoConnection k = getByKryoID(connection.getID());
                Log.info("&bLost kryonet connection {0}", connection.getID());
                if(k == null) return;

                Disconnect c = new Disconnect();
                c.id = k.id;

                Log.info("&bLost connection: {0}", k.id);

                Gdx.app.postRunnable(() -> {
                    Net.handleServerReceived(k.id, c);
                    connections.remove(k);
                });
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
            server.addListener(new LagListener(KryoRegistrator.fakeLagMin, KryoRegistrator.fakeLagMax, listener));
        }else{
            server.addListener(listener);
        }
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
    public KryoConnection getByID(int id){
        for(int i = 0; i < connections.size(); i ++){
            KryoConnection con = connections.get(i);
            if(con.id == id){
                return con;
            }
        }

        return null;
    }

    @Override
    public void kick(int connection, KickReason reason) {
        KryoConnection con = getByID(connection);
        if(con == null){
            Log.err("Cannot kick unknown player!");
            return;
        }else{
            Log.info("Kicking connection #{0} / IP: {1}. Reason: {2}", connection, con.address, reason);
        }

        KickPacket p = new KickPacket();
        p.reason = reason;

        con.send(p, SendMode.tcp);
        Timers.runTask(2f, con::close);
    }

    @Override
    public void host(int port) throws IOException {
        lastconnection = 0;
        connections.clear();
        missing.clear();
        server.bind(port, port);
        webServer = new SocketServer(Vars.webPort);
        webServer.start();

        serverThread = new Thread(() -> {
            try{
                server.run();
            }catch (Throwable e){
                if(!(e instanceof ClosedSelectorException)) handleException(e);
            }
        }, "Kryonet Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void close() {
        UCore.setPrivate(server, "shutdown", true);
        connections.clear();
        lastconnection = 0;

        async(server::close);
        async(() -> {
            try {
                if (webServer != null) webServer.stop(1);
                //kill them all
                for (Thread worker : Thread.getAllStackTraces().keySet()) {
                    if (worker.getName().contains("WebSocketWorker")) {
                        worker.interrupt();
                    }
                }
            }catch (Exception e){
                handleException(e);
            }
        });
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
        if(conn == null){
            if(!missing.contains(id))
                Log.err("Failed to find connection with ID {0}.", id);
            missing.add(id);
            return;
        }
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
    public void dispose(){
        close();
        Log.info("Disposed server.");
    }

    private void handleException(Throwable e){
        Timers.run(0f, () -> { throw new RuntimeException(e);});
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

    void async(Runnable run){
        Thread thread = new Thread(run);
        thread.setDaemon(true);
        thread.start();
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
                        if(debug) Log.info("Sending object with ID {0}", Registrator.getID(object.getClass()));
                        serializer.write(buffer, object);
                        int pos = buffer.position();
                        buffer.position(0);
                        byte[] out = new byte[pos];
                        buffer.get(out);
                        String string = new String(Base64Coder.encode(out));
                        if(debug) Log.info("Sending string: {0}", string);
                        socket.send(string);
                    }
                }catch (WebsocketNotConnectedException e){
                    //don't log anything, it's not important
                    connections.remove(this);
                }catch (Exception e){
                    connections.remove(this);
                    e.printStackTrace();
                }
            }else if (connection != null) {
                try {
                    if (mode == SendMode.tcp) {
                        connection.sendTCP(object);
                    } else {
                        connection.sendUDP(object);
                    }
                }catch (Exception e){
                    Log.err(e);
                    Log.info("Disconnecting invalid client!");
                    try{
                        NetErrorPacket packet = new NetErrorPacket();
                        packet.message = Strings.parseException(e, true);
                        Timers.runTask(5f, connection::close);
                    }catch (Exception e2){
                        Log.err(e2);
                        connection.close();
                    }
                    connection.close();

                    KryoConnection k = getByKryoID(connection.getID());
                    if(k != null) connections.remove(k);
                    Log.info("Connection removed {0}", k);
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
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Connect connect = new Connect();
            connect.addressTCP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
            KryoConnection kn = new KryoConnection(lastconnection ++, connect.addressTCP, conn);

            Log.info("&bRecieved web connection: {0} {1}", kn.id, connect.addressTCP);
            connections.add(kn);

            Gdx.app.postRunnable(() -> Net.handleServerReceived(kn.id, connect));
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            if (conn == null) return;

            KryoConnection k = getBySocket(conn);
            if(k == null) return;

            Disconnect disconnect = new Disconnect();
            disconnect.id = k.id;
            Log.info("&bLost web connection: {0}", k.id);
            Gdx.app.postRunnable(() -> Net.handleServerReceived(k.id, disconnect));
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            try {
                KryoConnection k = getBySocket(conn);
                if (k == null) return;

                if(message.equals("_ping_")){
                    conn.send("---" + Vars.playerGroup.size() + "|" + (headless ? "Server" : Vars.player.name)
                    + "|" + world.getMap().name + "|" + state.wave + "|" + Version.build);
                    connections.remove(k);
                }else {
                    byte[] out = Base64Coder.decode(message);
                    ByteBuffer buffer = ByteBuffer.wrap(out);
                    Object o = serializer.read(buffer);
                    Gdx.app.postRunnable(() -> Net.handleServerReceived(k.id, o));
                }
            }catch (Exception e){
                Log.err(e);
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            Log.info("WS error: ");
            Log.err(ex);
            if(ex instanceof BindException){
                Net.closeServer();
                if(!headless) {
                    Net.showError("$text.server.addressinuse");
                }else{
                    Log.err("Web address in use!");
                }
            }else if(ex.getMessage().equals("Permission denied")){
                Net.closeServer();
                Net.showError("Permission denied.");
            }
        }

        @Override
        public void onStart() {}
    }

}
