package io.anuke.mindustry.net;

import io.anuke.arc.*;
import io.anuke.arc.net.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.async.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;

import static io.anuke.mindustry.Min.*;

public class ArcNetServer implements ServerProvider{
    final Server server;
    final CopyOnWriteArrayList<ArcConnection> connections = new CopyOnWriteArrayList<>();
    Thread serverThread;

    public ArcNetServer(){
        server = new Server(4096 * 2, 4096, new PacketSerializer());
        server.setMulticast(multicastGroup, multicastPort);
        server.setDiscoveryHandler((address, handler) -> {
            ByteBuffer buffer = NetworkIO.writeServerData();
            buffer.position(0);
            handler.respond(buffer);
        });

        NetListener listener = new NetListener(){

            @Override
            public void connected(Connection connection){
                String ip = connection.getRemoteAddressTCP().getAddress().getHostAddress();

                ArcConnection kn = new ArcConnection(ip, connection);

                Connect c = new Connect();
                c.id = kn.id;
                c.addressTCP = ip;

                Log.debug("&bRecieved connection: {0}", c.addressTCP);

                connections.add(kn);
                Core.app.post(() -> Net.handleServerReceived(kn.id, c));
            }

            @Override
            public void disconnected(Connection connection){
                ArcConnection k = getByKryoID(connection.getID());
                if(k == null) return;

                Disconnect c = new Disconnect();
                c.id = k.id;

                Core.app.post(() -> {
                    Net.handleServerReceived(k.id, c);
                    connections.remove(k);
                });
            }

            @Override
            public void received(Connection connection, Object object){
                ArcConnection k = getByKryoID(connection.getID());
                if(object instanceof FrameworkMessage || k == null) return;

                Core.app.post(() -> {
                    try{
                        Net.handleServerReceived(k.id, object);
                    }catch(RuntimeException e){
                        if(e.getCause() instanceof ValidateException){
                            ValidateException v = (ValidateException)e.getCause();
                            Log.err("Validation failed: {0} ({1})", v.player.name, v.getMessage());
                        }else{
                            e.printStackTrace();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                });
            }
        };

        server.addListener(listener);
    }

    @Override
    public Iterable<ArcConnection> getConnections(){
        return connections;
    }

    @Override
    public ArcConnection getByID(int id){
        for(int i = 0; i < connections.size(); i++){
            ArcConnection con = connections.get(i);
            if(con.id == id){
                return con;
            }
        }

        return null;
    }

    @Override
    public void host(int port) throws IOException{
        connections.clear();
        server.bind(port, port);

        serverThread = new Thread(() -> {
            try{
                server.run();
            }catch(Throwable e){
                if(!(e instanceof ClosedSelectorException)) Threads.throwAppException(e);
            }
        }, "Net Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void close(){
        connections.clear();
        Threads.daemon(server::stop);
    }

    ArcConnection getByKryoID(int id){
        for(int i = 0; i < connections.size(); i++){
            ArcConnection con = connections.get(i);
            if(con.connection != null && con.connection.getID() == id){
                return con;
            }
        }

        return null;
    }

    class ArcConnection extends NetConnection{
        public final Connection connection;

        public ArcConnection(String address, Connection connection){
            super(address);
            this.connection = connection;
        }

        @Override
        public boolean isConnected(){
            return connection.isConnected();
        }

        @Override
        public void send(Object object, SendMode mode){
            try{
                if(mode == SendMode.tcp){
                    connection.sendTCP(object);
                }else{
                    connection.sendUDP(object);
                }
            }catch(Exception e){
                Log.err(e);
                Log.info("Error sending packet. Disconnecting invalid client!");
                connection.close();

                ArcConnection k = getByKryoID(connection.getID());
                if(k != null) connections.remove(k);
            }
        }

        @Override
        public void close(){
            if(connection.isConnected()) connection.close();
        }
    }

}