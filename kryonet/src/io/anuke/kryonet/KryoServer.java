package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntArray;
import com.esotericsoftware.kryonet.*;
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

public class KryoServer implements ServerProvider {
    Server server;
    IntArray connections = new IntArray();

    public KryoServer(){
        server = new Server(4096*2, 2048); //TODO tweak
        server.setDiscoveryHandler(new ServerDiscoveryHandler() {
            private ByteBuffer buffer = ByteBuffer.allocate(4);

            {
                buffer.putInt(987264236);
            }

            @Override
            public boolean onDiscoverHost(DatagramChannel datagramChannel, InetSocketAddress fromAddress) throws IOException {
                //TODO doesn't send data
                UCore.log("SENDING DATA: " + Arrays.toString(buffer.array()));
                datagramChannel.send(this.buffer, fromAddress);
                return true;
            }
        });

        Thread thread = new Thread(server, "Kryonet Server");
        thread.setDaemon(true);
        thread.start();
        server.addListener(new Listener(){

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
        });

        register(Registrator.getClasses());
    }

    @Override
    public IntArray getConnections() {
        return connections;
    }

    @Override
    public void kick(int connection) {
        Connection conn;
        try {
            conn = getByID(connection);
        }catch (Exception e){
            e.printStackTrace();
            connections.removeValue(connection);
            return;
        }
        KickPacket p = new KickPacket();
        p.reason = (byte)KickReason.kick.ordinal();

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
    }

    @Override
    public void close() {
        server.close();
    }

    @Override
    public void sendStream(int id, Streamable stream) {
        Connection connection = getByID(id);

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
    public void register(Class<?>... types) {
        for(Class<?> c : types){
            server.getKryo().register(c);
        }
        KryoRegistrator.register(server.getKryo());
    }

    @Override
    public void dispose(){
        try {
            server.dispose();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    Connection getByID(int id){
        for(Connection con : server.getConnections()){
            if(con.getID() == id){
                return con;
            }
        }

        throw new RuntimeException("Unable to find connection with ID " + id + "! Current connections: "
                + Arrays.toString(server.getConnections()));
    }
}
