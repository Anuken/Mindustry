package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.minlog.Log;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.Strings;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;

import static io.anuke.mindustry.Vars.*;

public class KryoClient implements ClientProvider{
    Client client;
    Consumer<Host> lastCallback;
    Array<InetAddress> foundAddresses = new Array<>();
    ClientDiscoveryHandler handler;
    LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();

    public KryoClient(){
        KryoCore.init();

        handler = new ClientDiscoveryHandler() {
            @Override
            public DatagramPacket onRequestNewDatagramPacket() {
                return new DatagramPacket(new byte[128], 128);
            }

            @Override
            public void onDiscoveredHost(DatagramPacket datagramPacket) {
                ByteBuffer buffer = ByteBuffer.wrap(datagramPacket.getData());
                Host host = NetworkIO.readServerData(datagramPacket.getAddress().getHostAddress(), buffer);
                for(InetAddress address : foundAddresses){
                    if(address.equals(datagramPacket.getAddress()) || (isLocal(address) && isLocal(datagramPacket.getAddress()))){
                        return;
                    }
                }
                Gdx.app.postRunnable(() -> lastCallback.accept(host));
                foundAddresses.add(datagramPacket.getAddress());
            }

            @Override
            public void onFinally() {

            }
        };

        client = new Client(8192, 4096, connection -> new ByteSerializer());
        client.setDiscoveryHandler(handler);

        Listener listener = new Listener(){
            @Override
            public void connected (Connection connection) {
                Connect c = new Connect();
                c.addressTCP = connection.getRemoteAddressTCP().getAddress().getHostAddress();
                c.id = connection.getID();
                if(connection.getRemoteAddressTCP() != null) c.addressTCP = connection.getRemoteAddressTCP().toString();

                threads.runDelay(() -> Net.handleClientReceived(c));
            }

            @Override
            public void disconnected (Connection connection) {
                Disconnect c = new Disconnect();

                threads.runDelay(() -> Net.handleClientReceived(c));
                if(connection.getLastProtocolError() != null) Log.error("\n\n\n\nProtocol error: " + connection.getLastProtocolError() + "\n\n\n\n");
            }

            @Override
            public void received (Connection connection, Object object) {
                if(object instanceof FrameworkMessage) return;

                threads.runDelay(() -> {
                    try{
                        Net.handleClientReceived(object);
                    }catch (Exception e){
                        e.printStackTrace();
                        if(e instanceof KryoNetException && e.getMessage() != null && e.getMessage().toLowerCase().contains("incorrect")) {
                            Net.showError("$text.server.mismatch");
                            netClient.disconnectQuietly();
                        }else{
                            throw new RuntimeException(e);
                        }
                    }
                });

            }
        };

        if(KryoCore.fakeLag){
            client.addListener(new Listener.LagListener(KryoCore.fakeLagMin, KryoCore.fakeLagMax, listener));
        }else{
            client.addListener(listener);
        }
    }

    private static boolean isLocal(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) return true;

        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
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
    public void connect(String ip, int port) throws IOException {
        //just in case
        client.stop();

        Thread updateThread = new Thread(() -> {
            try{
                client.run();
            }catch (Exception e){
                if(!(e instanceof ClosedSelectorException)) handleException(e);
            }
        }, "Kryonet Client");
        updateThread.setDaemon(true);
        updateThread.start();

        client.connect(5000, ip, port, port);
    }

    @Override
    public void disconnect() {
        client.close();
    }

    @Override
    public void send(Object object, SendMode mode) {
        if(mode == SendMode.tcp){
            client.sendTCP(object);
        }else{
            client.sendUDP(object);
        }

        Pooling.free(object);
    }

    @Override
    public void updatePing() {
        client.updateReturnTripTime();
    }

    @Override
    public int getPing() {
        return client.getReturnTripTime();
    }

    @Override
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> invalid){
        runAsync(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(address), port));

                socket.setSoTimeout(2000);

                lastCallback = valid;

                DatagramPacket packet = handler.onRequestNewDatagramPacket();

                socket.receive(packet);

                ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                Host host = NetworkIO.readServerData(packet.getAddress().getHostAddress(), buffer);

                Gdx.app.postRunnable(() -> valid.accept(host));
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> invalid.accept(e));
            }
        });
    }

    @Override
    public void discover(Consumer<Host> callback, Runnable done){
        runAsync(() -> {
            foundAddresses.clear();
            lastCallback = callback;
            client.discoverHosts(port, 3000);
            Gdx.app.postRunnable(done);
        });
    }

    @Override
    public void dispose(){
        try {
            client.dispose();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private void runAsync(Runnable run){
        Thread thread = new Thread(run, "Client Async Run");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleException(Exception e){
        e.printStackTrace();
        if(e instanceof KryoNetException){
            Gdx.app.postRunnable(() -> Net.showError("$text.server.mismatch"));
        }else{
            Net.showError(Strings.parseException(e, true));
            disconnect();
        }
    }

}
