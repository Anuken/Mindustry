package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.util.List;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.net.Net.packetPoolLock;

public class KryoClient implements ClientProvider{
    Client client;
    ObjectMap<InetAddress, Host> addresses = new ObjectMap<>();
    ClientDiscoveryHandler handler;

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
                Host address = NetworkIO.readServerData(datagramPacket.getAddress().getHostAddress(), buffer);
                addresses.put(datagramPacket.getAddress(), address);
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

        synchronized (packetPoolLock) {
            Pooling.free(object);
        }
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

                addresses.clear();

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
    public void discover(Consumer<Array<Host>> callback){
        runAsync(() -> {
            addresses.clear();
            List<InetAddress> list = client.discoverHosts(port, 3000);
            ObjectSet<String> hostnames = new ObjectSet<>();
            Array<Host> result = new Array<>();

            for(InetAddress a : list){
                if(!hostnames.contains(a.getHostName())) {
                    Host address = addresses.get(a);
                    if(address != null) result.add(address);

                }
                hostnames.add(a.getHostName());
            }

            Gdx.app.postRunnable(() -> callback.accept(result));
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
