package io.anuke.mindustry.net;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.net.*;
import io.anuke.arc.util.async.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import static io.anuke.mindustry.Vars.*;

public class ArcNetClient implements ClientProvider{
    final Client client;
    final Supplier<DatagramPacket> packetSupplier = () -> new DatagramPacket(new byte[256], 256);

    public ArcNetClient(){
        client = new Client(8192, 4096, new PacketSerializer());
        client.setDiscoveryPacket(packetSupplier);

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
            public void disconnected(Connection connection, DcReason reason){
                if(connection.getLastProtocolError() != null){
                    netClient.setQuiet();
                }

                Disconnect c = new Disconnect();
                c.reason = reason.toString();
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
    public void connect(String ip, int port, Runnable success){
        Threads.daemon(() -> {
            try{
                //just in case
                client.stop();

                Threads.daemon("Net Client", () -> {
                    try{
                        client.run();
                    }catch(Exception e){
                        if(!(e instanceof ClosedSelectorException)) handleException(e);
                    }
                });

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
        Threads.daemon(() -> {
            try{
                DatagramSocket socket = new DatagramSocket();
                socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(address), port));
                socket.setSoTimeout(2000);

                DatagramPacket packet = packetSupplier.get();
                socket.receive(packet);

                ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                Host host = NetworkIO.readServerData(packet.getAddress().getHostAddress(), buffer);

                Core.app.post(() -> valid.accept(host));
            }catch(Exception e){
                Core.app.post(() -> invalid.accept(e));
            }
        });
    }

    @Override
    public void discover(Consumer<Host> callback, Runnable done){
        Array<InetAddress> foundAddresses = new Array<>();
        client.discoverHosts(port, multicastGroup, multicastPort, 3000, packet -> {
            Core.app.post(() -> {
                try{
                    if(foundAddresses.contains(address -> address.equals(packet.getAddress()) || (isLocal(address) && isLocal(packet.getAddress())))){
                        return;
                    }
                    ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                    Host host = NetworkIO.readServerData(packet.getAddress().getHostAddress(), buffer);
                    callback.accept(host);
                    foundAddresses.add(packet.getAddress());
                }catch(Exception e){
                    //don't crash when there's an error pinging a a server or parsing data
                    e.printStackTrace();
                }
            });
        }, () -> Core.app.post(done));
    }

    @Override
    public void dispose(){
        try{
            client.dispose();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private void handleException(Exception e){
        if(e instanceof ArcNetException){
            Core.app.post(() -> Net.showError(new IOException("mismatch")));
        }else if(e instanceof ClosedChannelException){
            Core.app.post(() -> Net.showError(new IOException("alreadyconnected")));
        }else{
            Core.app.post(() -> Net.showError(e));
        }
    }

}