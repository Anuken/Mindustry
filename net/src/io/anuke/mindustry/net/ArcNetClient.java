package io.anuke.mindustry.net;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.net.*;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.ClosedSelectorException;

import static io.anuke.mindustry.Vars.netClient;
import static io.anuke.mindustry.Vars.port;

public class ArcNetClient implements ClientProvider{
    final Client client;
    final Array<InetAddress> foundAddresses = new Array<>();
    final ClientDiscoveryHandler handler;
    final LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
    Consumer<Host> lastCallback;

    public ArcNetClient(){
        handler = new ClientDiscoveryHandler(){
            @Override
            public DatagramPacket newDatagramPacket(){
                return new DatagramPacket(new byte[256], 256);
            }

            @Override
            public void discoveredHost(DatagramPacket datagramPacket){
                ByteBuffer buffer = ByteBuffer.wrap(datagramPacket.getData());
                Host host = NetworkIO.readServerData(datagramPacket.getAddress().getHostAddress(), buffer);
                for(InetAddress address : foundAddresses){
                    if(address.equals(datagramPacket.getAddress()) || (isLocal(address) && isLocal(datagramPacket.getAddress()))){
                        return;
                    }
                }
                Core.app.post(() -> lastCallback.accept(host));
                foundAddresses.add(datagramPacket.getAddress());
            }

            @Override
            public void finish(){

            }
        };

        client = new Client(8192, 4096, new PacketSerializer());
        client.setDiscoveryHandler(handler);

        NetListener listener = new NetListener(){
            @Override
            public void connected(Connection connection){
                Connect c = new Connect();
                c.addressTCP = connection.getRemoteAddressTCP().getAddress().getHostAddress();
                c.id = connection.getID();
                if(connection.getRemoteAddressTCP() != null) c.addressTCP = connection.getRemoteAddressTCP().toString();

                Core.app.post(() -> Net.handleClientReceived(c));
            }

            @Override
            public void disconnected(Connection connection){
                if(connection.getLastProtocolError() != null){
                    netClient.setQuiet();
                }

                Disconnect c = new Disconnect();
                Core.app.post(() -> Net.handleClientReceived(c));
            }

            @Override
            public void received(Connection connection, Object object){
                if(object instanceof FrameworkMessage) return;

                Core.app.post(() -> {
                    try{
                        Net.handleClientReceived(object);
                    }catch(Exception e){
                        handleException(e);
                    }
                });

            }
        };

        client.addListener(listener);
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
    public byte[] decompressSnapshot(byte[] input, int size){
        byte[] result = new byte[size];
        decompressor.decompress(input, result);
        return result;
    }

    @Override
    public void connect(String ip, int port, Runnable success){
        runAsync(() -> {
            try{
                //just in case
                client.stop();

                Thread updateThread = new Thread(() -> {
                    try{
                        client.run();
                    }catch(Exception e){
                        if(!(e instanceof ClosedSelectorException)) handleException(e);
                    }
                }, "Net Client");
                updateThread.setDaemon(true);
                updateThread.start();

                client.connect(5000, ip, port, port);
                success.run();
            }catch(Exception e){
                handleException(e);
            }
        });
    }

    @Override
    public void disconnect(){
        client.close();
    }

    @Override
    public void send(Object object, SendMode mode){
        try{
            if(mode == SendMode.tcp){
                client.sendTCP(object);
            }else{
                client.sendUDP(object);
            }
            //sending things can cause an under/overflow, catch it and disconnect instead of crashing
        }catch(BufferOverflowException | BufferUnderflowException e){
            Net.showError(e);
        }

        Pools.free(object);
    }

    @Override
    public void updatePing(){
        client.updateReturnTripTime();
    }

    @Override
    public int getPing(){
        return client.getReturnTripTime();
    }

    @Override
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> invalid){
        runAsync(() -> {
            synchronized(handler){
                try{
                    DatagramSocket socket = new DatagramSocket();
                    socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(address), port));

                    socket.setSoTimeout(2000);

                    lastCallback = valid;

                    DatagramPacket packet = handler.newDatagramPacket();

                    socket.receive(packet);

                    ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                    Host host = NetworkIO.readServerData(packet.getAddress().getHostAddress(), buffer);

                    Core.app.post(() -> valid.accept(host));
                }catch(Exception e){
                    Core.app.post(() -> invalid.accept(e));
                }
            }
        });
    }

    @Override
    public void discover(Consumer<Host> callback, Runnable done){
        runAsync(() -> {
            synchronized(handler){
                foundAddresses.clear();
                lastCallback = callback;
                client.discoverHosts(port, 3000);
                Core.app.post(done);
            }
        });
    }

    @Override
    public void dispose(){
        try{
            client.dispose();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private void runAsync(Runnable run){
        Thread thread = new Thread(run, "Client Async Run");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleException(Exception e){
        if(e instanceof ArcNetException){
            Core.app.post(() -> Net.showError(new IOException("mismatch")));
        }else{
            Core.app.post(() -> Net.showError(e));
        }
    }

}
