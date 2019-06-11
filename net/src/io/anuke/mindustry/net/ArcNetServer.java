package io.anuke.mindustry.net;

import com.dosse.upnp.UPnP;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.net.*;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Net.ServerProvider;
import io.anuke.mindustry.net.Packets.*;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static io.anuke.mindustry.Vars.*;

public class ArcNetServer implements ServerProvider{
    final Server server;
    final CopyOnWriteArrayList<KryoConnection> connections = new CopyOnWriteArrayList<>();
    final CopyOnWriteArraySet<Integer> missing = new CopyOnWriteArraySet<>();
    final Array<KryoConnection> array = new Array<>();
    final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
    Thread serverThread;

    int lastconnection = 0;

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

                KryoConnection kn = new KryoConnection(lastconnection++, ip, connection);

                Connect c = new Connect();
                c.id = kn.id;
                c.addressTCP = ip;

                Log.debug("&bRecieved connection: {0}", c.addressTCP);

                connections.add(kn);
                Core.app.post(() -> Net.handleServerReceived(kn.id, c));
            }

            @Override
            public void disconnected(Connection connection){
                KryoConnection k = getByKryoID(connection.getID());
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
                KryoConnection k = getByKryoID(connection.getID());
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
    public byte[] compressSnapshot(byte[] input){
        return compressor.compress(input);
    }

    @Override
    public Array<KryoConnection> getConnections(){
        array.clear();
        for(KryoConnection c : connections){
            array.add(c);
        }
        return array;
    }

    @Override
    public KryoConnection getByID(int id){
        for(int i = 0; i < connections.size(); i++){
            KryoConnection con = connections.get(i);
            if(con.id == id){
                return con;
            }
        }

        return null;
    }

    @Override
    public void host(int port) throws IOException{
        //attempt to open default ports if they're not already open
        //this only opens the default port due to security concerns (?)
        if(port == Vars.port){
            async(() -> {
                try{
                    if(!UPnP.isMappedTCP(port)) UPnP.openPortTCP(port);
                    if(!UPnP.isMappedUDP(port)) UPnP.openPortUDP(port);
                }catch(Throwable ignored){
                }
            });
        }

        lastconnection = 0;
        connections.clear();
        missing.clear();
        server.bind(port, port);

        serverThread = new Thread(() -> {
            try{
                server.run();
            }catch(Throwable e){
                if(!(e instanceof ClosedSelectorException)) handleException(e);
            }
        }, "Net Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void close(){
        connections.clear();
        lastconnection = 0;

        async(server::stop);
    }

    @Override
    public void sendStream(int id, Streamable stream){
        KryoConnection connection = getByID(id);
        if(connection == null) return;
        try{

            if(connection.connection != null){

                connection.connection.addListener(new InputStreamSender(stream.stream, 512){
                    int id;

                    protected void start(){
                        //send an object so the receiving side knows how to handle the following chunks
                        StreamBegin begin = new StreamBegin();
                        begin.total = stream.stream.available();
                        begin.type = Registrator.getID(stream.getClass());
                        connection.connection.sendTCP(begin);
                        id = begin.id;
                    }

                    protected Object next(byte[] bytes){
                        StreamChunk chunk = new StreamChunk();
                        chunk.id = id;
                        chunk.data = bytes;
                        return chunk; //wrap the byte[] with an object so the receiving side knows how to handle it.
                    }
                });
            }else{
                int cid;
                StreamBegin begin = new StreamBegin();
                begin.total = stream.stream.available();
                begin.type = Registrator.getID(stream.getClass());
                connection.send(begin, SendMode.tcp);
                cid = begin.id;

                while(stream.stream.available() > 0){
                    byte[] bytes = new byte[Math.min(512, stream.stream.available())];
                    stream.stream.read(bytes);

                    StreamChunk chunk = new StreamChunk();
                    chunk.id = cid;
                    chunk.data = bytes;
                    connection.send(chunk, SendMode.tcp);
                }
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(Object object, SendMode mode){
        for(int i = 0; i < connections.size(); i++){
            connections.get(i).send(object, mode);
        }
    }

    @Override
    public void sendTo(int id, Object object, SendMode mode){
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
    public void sendExcept(int id, Object object, SendMode mode){
        for(int i = 0; i < connections.size(); i++){
            KryoConnection conn = connections.get(i);
            if(conn.id != id) conn.send(object, mode);
        }
    }

    @Override
    public void dispose(){
        close();
    }

    private void handleException(Throwable e){
        Time.run(0f, () -> {
            throw new RuntimeException(e);
        });
    }

    KryoConnection getByKryoID(int id){
        for(int i = 0; i < connections.size(); i++){
            KryoConnection con = connections.get(i);
            if(con.connection != null && con.connection.getID() == id){
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
        public final Connection connection;

        public KryoConnection(int id, String address, Connection connection){
            super(id, address);
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

                KryoConnection k = getByKryoID(connection.getID());
                if(k != null) connections.remove(k);
            }
        }

        @Override
        public void close(){
            if(connection.isConnected()) connection.close();
        }
    }

}
