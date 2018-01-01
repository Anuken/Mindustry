package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntArray;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.util.InputStreamSender;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Net.ServerProvider;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Registrator;
import io.anuke.mindustry.net.Streamable;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.ucore.UCore;

import java.io.IOException;
import java.util.Arrays;

public class KryoServer implements ServerProvider {
    Server server;
    IntArray connections = new IntArray();

    public KryoServer(){
        server = new Server();
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
                    Gdx.app.exit();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void disconnected (Connection connection) {
                Disconnect c = new Disconnect();
                c.id = connection.getID();

                try{
                    Net.handleServerReceived(c, c.id);
                }catch (Exception e){
                    Gdx.app.exit();
                    throw new RuntimeException(e);
                }

                connections.removeValue(c.id);
            }

            @Override
            public void received (Connection connection, Object object) {
                if(object instanceof FrameworkMessage) return;

                try{
                    Net.handleServerReceived(object, connection.getID());
                }catch (Exception e){
                    Gdx.app.exit();
                    throw new RuntimeException(e);
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
                UCore.log("Sending begin packet: " + begin);
            }

            protected Object next (byte[] bytes) {
                StreamChunk chunk = new StreamChunk();
                chunk.id = id;
                chunk.data = bytes;
                UCore.log("Sending chunk of size " + chunk.data.length);
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
