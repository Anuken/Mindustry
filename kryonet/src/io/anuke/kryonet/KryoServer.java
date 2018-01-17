package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntArray;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.LagListener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.util.InputStreamSender;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Net.ServerProvider;
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

import java.io.IOException;
import java.nio.ByteBuffer;

public class KryoServer implements ServerProvider {
    Server server;
    IntArray connections = new IntArray();

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
                Connect c = new Connect();
                c.id = connection.getID();
                c.addressTCP = connection.getRemoteAddressTCP().toString();

                try {
                    Net.handleServerReceived(c, c.id);
                    connections.add(c.id);
                }catch (Exception e){
                    Gdx.app.postRunnable(() -> {throw new RuntimeException(e);});
                }
            }

            @Override
            public void disconnected (Connection connection) {
                connections.removeValue(connection.getID());

                Disconnect c = new Disconnect();
                c.id = connection.getID();

                try{
                    Net.handleServerReceived(c, c.id);
                }catch (Exception e){
                    Gdx.app.postRunnable(() -> {throw new RuntimeException(e);});
                }
            }

            @Override
            public void received (Connection connection, Object object) {
                if(object instanceof FrameworkMessage) return;

                try{
                    Net.handleServerReceived(object, connection.getID());
                }catch (Exception e){
                    //...do absolutely nothing.
                    e.printStackTrace();
                    //Gdx.app.postRunnable(() -> {throw new RuntimeException(e);});
                }
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
    public IntArray getConnections() {
        return connections;
    }

    @Override
    public void kick(int connection) {
        Connection conn = getByID(connection);

        if(conn == null){
            connections.removeValue(connection);
            return;
        }

        KickPacket p = new KickPacket();
        p.reason = KickReason.kick;

        conn.sendTCP(p);
        Timers.runTask(1f, () -> {
            if(conn.isConnected()){
                conn.close();
            }
        });
    }

    @Override
    public void host(int port) throws IOException {
        server.bind(port, port);

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

        new Thread(() -> server.close()).run();
    }

    @Override
    public void sendStream(int id, Streamable stream) {
        Connection connection = getByID(id);
        if(connection == null) return;

        connection.addListener(new InputStreamSender(stream.stream, 512) {
            int id;

            protected void start () {
                //send an object so the receiving side knows how to handle the following chunks
                StreamBegin begin = new StreamBegin();
                begin.total = stream.stream.available();
                begin.type = stream.getClass();
                connection.sendTCP(begin);
                id = begin.id;
            }

            protected Object next (byte[] bytes) {
                StreamChunk chunk = new StreamChunk();
                chunk.id = id;
                chunk.data = bytes;
                return chunk; //wrap the byte[] with an object so the receiving side knows how to handle it.
            }
        });
    }

    @Override
    public void send(Object object, SendMode mode) {
        if(mode == SendMode.tcp){
            server.sendToAllTCP(object);
        }else{
            server.sendToAllUDP(object);
        }
    }

    @Override
    public void sendTo(int id, Object object, SendMode mode) {
        if(mode == SendMode.tcp){
            server.sendToTCP(id, object);
        }else{
            server.sendToUDP(id, object);
        }
    }

    @Override
    public void sendExcept(int id, Object object, SendMode mode) {
        if(mode == SendMode.tcp){
            server.sendToAllExceptTCP(id, object);
        }else{
            server.sendToAllExceptUDP(id, object);
        }
    }

    @Override
    public int getPingFor(int connection) {
        return getByID(connection).getReturnTripTime();
    }

    @Override
    public void register(Class<?>... types) { }

    @Override
    public void dispose(){
        try {
            server.dispose();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private void handleException(Exception e){
        Gdx.app.postRunnable(() -> { throw new RuntimeException(e);});
    }

    Connection getByID(int id){
        for(Connection con : server.getConnections()){
            if(con.getID() == id){
                return con;
            }
        }

        return null;
    }
}
