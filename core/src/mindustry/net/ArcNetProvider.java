package mindustry.net;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.net.*;
import arc.net.FrameworkMessage.*;
import arc.net.dns.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.net.Administration.*;
import mindustry.net.Net.*;
import mindustry.net.Packets.*;
import net.jpountz.lz4.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

public class ArcNetProvider implements NetProvider{
    final Client client;
    final Prov<DatagramPacket> packetSupplier = () -> new DatagramPacket(new byte[512], 512);

    final Server server;
    final CopyOnWriteArrayList<ArcConnection> connections = new CopyOnWriteArrayList<>();
    Thread serverThread;

    private static final LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
    private static final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();

    private volatile int playerLimitCache, packetSpamLimit;

    public ArcNetProvider(){
        ArcNet.errorHandler = e -> {
            if(Log.level == LogLevel.debug){
                Log.debug(Strings.getStackTrace(e));
            }
        };

        //fetch this in the main thread to prevent threading issues
        Events.run(Trigger.update, () -> {
            playerLimitCache = netServer.admins.getPlayerLimit();
            packetSpamLimit = Config.packetSpamLimit.num();
        });

        client = new Client(8192, 8192, new PacketSerializer());
        client.setDiscoveryPacket(packetSupplier);
        client.addListener(new NetListener(){
            @Override
            public void connected(Connection connection){
                Connect c = new Connect();
                c.addressTCP = connection.getRemoteAddressTCP().getAddress().getHostAddress();
                if(connection.getRemoteAddressTCP() != null) c.addressTCP = connection.getRemoteAddressTCP().toString();

                Core.app.post(() -> net.handleClientReceived(c));
            }

            @Override
            public void disconnected(Connection connection, DcReason reason){
                if(connection.getLastProtocolError() != null){
                    netClient.setQuiet();
                }

                Disconnect c = new Disconnect();
                c.reason = reason.toString();
                Core.app.post(() -> net.handleClientReceived(c));
            }

            @Override
            public void received(Connection connection, Object object){
                if(!(object instanceof Packet p)) return;

                Core.app.post(() -> {
                    try{
                        net.handleClientReceived(p);
                    }catch(Throwable e){
                        net.handleException(e);
                    }
                });

            }
        });

        server = new Server(32768, 8192, new PacketSerializer());
        server.setMulticast(multicastGroup, multicastPort);
        server.setDiscoveryHandler((address, handler) -> {
            ByteBuffer buffer = NetworkIO.writeServerData();
            buffer.position(0);
            handler.respond(buffer);
        });

        server.addListener(new NetListener(){

            @Override
            public void connected(Connection connection){
                String ip = connection.getRemoteAddressTCP().getAddress().getHostAddress();

                //kill connections above the limit to prevent spam
                if((playerLimitCache > 0 && server.getConnections().length > playerLimitCache) || netServer.admins.isDosBlacklisted(ip)){
                    connection.close(DcReason.closed);
                    return;
                }

                ArcConnection kn = new ArcConnection(ip, connection);

                Connect c = new Connect();
                c.addressTCP = ip;

                Log.debug("&bReceived connection: @", c.addressTCP);

                connections.add(kn);
                Core.app.post(() -> net.handleServerReceived(kn, c));
            }

            @Override
            public void disconnected(Connection connection, DcReason reason){
                ArcConnection k = getByArcID(connection.getID());
                if(k == null) return;

                Disconnect c = new Disconnect();
                c.reason = reason.toString();

                Core.app.post(() -> {
                    net.handleServerReceived(k, c);
                    connections.remove(k);
                });
            }

            @Override
            public void received(Connection connection, Object object){
                ArcConnection k = getByArcID(connection.getID());
                if(!(object instanceof Packet pack) || k == null) return;

                if(packetSpamLimit > 0 && !k.packetRate.allow(3000, packetSpamLimit)){
                    Log.warn("Blacklisting IP '@' as potential DOS attack - packet spam.", k.address);
                    connection.close(DcReason.closed);
                    netServer.admins.blacklistDos(k.address);
                    return;
                }

                Core.app.post(() -> {
                    try{
                        net.handleServerReceived(k, pack);
                    }catch(Throwable e){
                        Log.err(e);
                    }
                });
            }
        });
    }

    private static boolean isLocal(InetAddress addr){
        if(addr.isAnyLocalAddress() || addr.isLoopbackAddress()) return true;

        try{
            return NetworkInterface.getByInetAddress(addr) != null;
        }catch(Exception e){
            return false;
        }
    }

    @Override
    public void connectClient(String ip, int port, Runnable success){
        Threads.daemon(() -> {
            try{
                //just in case
                client.stop();

                Threads.daemon("Net Client", () -> {
                    try{
                        client.run();
                    }catch(Exception e){
                        if(!(e instanceof ClosedSelectorException)) net.handleException(e);
                    }
                });

                client.connect(5000, ip, port, port);
                success.run();
            }catch(Exception e){
                if(netClient.isConnecting()){
                    net.handleException(e);
                }
            }
        });
    }

    @Override
    public void disconnectClient(){
        client.close();
    }

    @Override
    public void sendClient(Object object, boolean reliable){
        try{
            if(reliable){
                client.sendTCP(object);
            }else{
                client.sendUDP(object);
            }
            //sending things can cause an under/overflow, catch it and disconnect instead of crashing
        }catch(BufferOverflowException | BufferUnderflowException e){
            net.showError(e);
        }
    }

    @Override
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> invalid){
        try{
            var host = pingHostImpl(address, port);
            Core.app.post(() -> valid.get(host));
        }catch(IOException e){
            if(port == Vars.port){
                for(var record : ArcDns.getSrvRecords("_mindustry._tcp." + address)){
                    try{
                        var host = pingHostImpl(record.target, record.port);
                        Core.app.post(() -> valid.get(host));
                        return;
                    }catch(IOException ignored){
                    }
                }
            }
            Core.app.post(() -> invalid.get(e));
        }
    }

    private Host pingHostImpl(String address, int port) throws IOException{
        try(DatagramSocket socket = new DatagramSocket()){
            long time = Time.millis();

            socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(address), port));
            socket.setSoTimeout(2000);

            DatagramPacket packet = packetSupplier.get();
            socket.receive(packet);

            ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
            Host host = NetworkIO.readServerData((int)Time.timeSinceMillis(time), packet.getAddress().getHostAddress(), buffer);
            host.port = port;
            return host;
        }
    }

    @Override
    public void discoverServers(Cons<Host> callback, Runnable done){
        Seq<InetAddress> foundAddresses = new Seq<>();
        long time = Time.millis();

        client.discoverHosts(port, multicastGroup, multicastPort, 3000, packet -> {
            synchronized(foundAddresses){
                try{
                    if(foundAddresses.contains(address -> address.equals(packet.getAddress()) || (isLocal(address) && isLocal(packet.getAddress())))){
                        return;
                    }
                    ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                    Host host = NetworkIO.readServerData((int)Time.timeSinceMillis(time), packet.getAddress().getHostAddress(), buffer);
                    Core.app.post(() -> callback.get(host));
                    foundAddresses.add(packet.getAddress());
                }catch(Exception e){
                    //don't crash when there's an error pinging a server or parsing data
                    e.printStackTrace();
                }
            }
        }, () -> Core.app.post(done));
    }

    @Override
    public void dispose(){
        disconnectClient();
        closeServer();
        try{
            client.dispose();
        }catch(IOException ignored){
        }
    }

    @Override
    public Iterable<ArcConnection> getConnections(){
        return connections;
    }

    @Override
    public void hostServer(int port) throws IOException{
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
    public void closeServer(){
        connections.clear();
        mainExecutor.submit(server::stop);
    }

    ArcConnection getByArcID(int id){
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
        public void sendStream(Streamable stream){
            connection.addListener(new InputStreamSender(stream.stream, 512){
                int id;

                @Override
                protected void start(){
                    //send an object so the receiving side knows how to handle the following chunks
                    StreamBegin begin = new StreamBegin();
                    begin.total = stream.stream.available();
                    begin.type = Net.getPacketId(stream);
                    connection.sendTCP(begin);
                    id = begin.id;
                }

                @Override
                protected Object next(byte[] bytes){
                    StreamChunk chunk = new StreamChunk();
                    chunk.id = id;
                    chunk.data = bytes;
                    return chunk; //wrap the byte[] with an object so the receiving side knows how to handle it.
                }
            });
        }

        @Override
        public void send(Object object, boolean reliable){
            try{
                if(reliable){
                    connection.sendTCP(object);
                }else{
                    connection.sendUDP(object);
                }
            }catch(Exception e){
                Log.err(e);
                Log.info("Error sending packet. Disconnecting invalid client!");
                connection.close(DcReason.error);

                ArcConnection k = getByArcID(connection.getID());
                if(k != null) connections.remove(k);
            }
        }

        @Override
        public void close(){
            if(connection.isConnected()) connection.close(DcReason.closed);
        }
    }

    public static class PacketSerializer implements NetSerializer{
        //for debugging total read/write speeds
        private static final boolean debug = false;

        ThreadLocal<ByteBuffer> decompressBuffer = Threads.local(() -> ByteBuffer.allocate(32768));
        ThreadLocal<Reads> reads = Threads.local(() -> new Reads(new ByteBufferInput(decompressBuffer.get())));
        ThreadLocal<Writes> writes = Threads.local(() -> new Writes(new ByteBufferOutput(decompressBuffer.get())));

        //for debugging network write counts
        static WindowedMean upload = new WindowedMean(5), download = new WindowedMean(5);
        static long lastUpload, lastDownload, uploadAccum, downloadAccum;
        static int lastPos;

        @Override
        public Object read(ByteBuffer byteBuffer){
            if(debug){
                if(Time.timeSinceMillis(lastDownload) >= 1000){
                    lastDownload = Time.millis();
                    download.add(downloadAccum);
                    downloadAccum = 0;
                    Log.info("Download: @ b/s", download.mean());
                }
                downloadAccum += byteBuffer.remaining();
            }

            byte id = byteBuffer.get();
            if(id == -2){
                return readFramework(byteBuffer);
            }else{
                //read length int, followed by compressed lz4 data
                Packet packet = Net.newPacket(id);
                var buffer = decompressBuffer.get();
                int length = byteBuffer.getShort() & 0xffff;
                byte compression = byteBuffer.get();

                //no compression, copy over buffer
                if(compression == 0){
                    buffer.position(0).limit(length);
                    buffer.put(byteBuffer.array(), byteBuffer.position(), length);
                    buffer.position(0);
                    packet.read(reads.get(), length);
                    //move read packets forward
                    byteBuffer.position(byteBuffer.position() + buffer.position());
                }else{
                    //decompress otherwise
                    int read = decompressor.decompress(byteBuffer, byteBuffer.position(), buffer, 0, length);

                    buffer.position(0);
                    buffer.limit(length);
                    packet.read(reads.get(), length);
                    //move buffer forward based on bytes read by decompressor
                    byteBuffer.position(byteBuffer.position() + read);
                }

                return packet;
            }
        }

        @Override
        public void write(ByteBuffer byteBuffer, Object o){
            if(debug){
                lastPos = byteBuffer.position();
            }

            //write raw buffer
            if(o instanceof ByteBuffer raw){
                byteBuffer.put(raw);
            }else if(o instanceof FrameworkMessage msg){
                byteBuffer.put((byte)-2); //code for framework message
                writeFramework(byteBuffer, msg);
            }else{
                if(!(o instanceof Packet pack)) throw new RuntimeException("All sent objects must implement be Packets! Class: " + o.getClass());
                byte id = Net.getPacketId(pack);
                byteBuffer.put(id);

                var temp = decompressBuffer.get();
                temp.position(0);
                temp.limit(temp.capacity());
                pack.write(writes.get());

                short length = (short)temp.position();

                //write length, uncompressed
                byteBuffer.putShort(length);

                //don't bother with small packets
                if(length < 36 || pack instanceof StreamChunk){
                    //write direct contents...
                    byteBuffer.put((byte)0); //0 = no compression
                    byteBuffer.put(temp.array(), 0, length);
                }else{
                    byteBuffer.put((byte)1); //1 = compression
                    //write compressed data; this does not modify position!
                    int written = compressor.compress(temp, 0, temp.position(), byteBuffer, byteBuffer.position(), byteBuffer.remaining());
                    //skip to indicate the written, compressed data
                    byteBuffer.position(byteBuffer.position() + written);
                }
            }

            if(debug){
                if(Time.timeSinceMillis(lastUpload) >= 1000){
                    lastUpload = Time.millis();
                    upload.add(uploadAccum);
                    uploadAccum = 0;
                    Log.info("Upload: @ b/s", upload.mean());
                }
                uploadAccum += byteBuffer.position() - lastPos;
            }
        }

        public void writeFramework(ByteBuffer buffer, FrameworkMessage message){
            if(message instanceof Ping p){
                buffer.put((byte)0);
                buffer.putInt(p.id);
                buffer.put(p.isReply ? 1 : (byte)0);
            }else if(message instanceof DiscoverHost){
                buffer.put((byte)1);
            }else if(message instanceof KeepAlive){
                buffer.put((byte)2);
            }else if(message instanceof RegisterUDP p){
                buffer.put((byte)3);
                buffer.putInt(p.connectionID);
            }else if(message instanceof RegisterTCP p){
                buffer.put((byte)4);
                buffer.putInt(p.connectionID);
            }
        }

        public FrameworkMessage readFramework(ByteBuffer buffer){
            byte id = buffer.get();

            if(id == 0){
                Ping p = new Ping();
                p.id = buffer.getInt();
                p.isReply = buffer.get() == 1;
                return p;
            }else if(id == 1){
                return FrameworkMessage.discoverHost;
            }else if(id == 2){
                return FrameworkMessage.keepAlive;
            }else if(id == 3){
                RegisterUDP p = new RegisterUDP();
                p.connectionID = buffer.getInt();
                return p;
            }else if(id == 4){
                RegisterTCP p = new RegisterTCP();
                p.connectionID = buffer.getInt();
                return p;
            }else{
                throw new RuntimeException("Unknown framework message!");
            }
        }
    }

}
