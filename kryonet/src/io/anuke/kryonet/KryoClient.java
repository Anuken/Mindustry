package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.kryonet.FrameworkMessage.DiscoverHost;
import com.esotericsoftware.kryonet.Listener.LagListener;
import com.esotericsoftware.kryonet.serialization.Serialization;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Registrator;
import io.anuke.ucore.UCore;
import io.anuke.ucore.function.Consumer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;

public class KryoClient implements ClientProvider{
    Client client;
    ObjectMap<InetAddress, Host> addresses = new ObjectMap<>();
    ClientDiscoveryHandler handler;

    public KryoClient(){
        handler = new ClientDiscoveryHandler() {
            @Override
            public DatagramPacket onRequestNewDatagramPacket() {
                return new DatagramPacket(new byte[32], 32);
            }

            @Override
            public void onDiscoveredHost(DatagramPacket datagramPacket) {
                ByteBuffer buffer = ByteBuffer.wrap(datagramPacket.getData());
                Host address = KryoRegistrator.readServerData(datagramPacket.getAddress(), buffer);
                addresses.put(datagramPacket.getAddress(), address);
                UCore.log("Host data found: " + buffer.capacity() + " bytes.");
            }

            @Override
            public void onFinally() {

            }
        };

        client = new Client(8192, 2048, connection -> new ByteSerializer());
        client.setDiscoveryHandler(handler);

        Listener listener = new Listener(){
            @Override
            public void connected (Connection connection) {
                Connect c = new Connect();
                c.id = connection.getID();
                if(connection.getRemoteAddressTCP() != null) c.addressTCP = connection.getRemoteAddressTCP().toString();

                try{
                    Net.handleClientReceived(c);
                }catch (Exception e){
                    Gdx.app.postRunnable(() -> {throw new RuntimeException(e);});
                }
            }

            @Override
            public void disconnected (Connection connection) {
                Disconnect c = new Disconnect();

                try{
                    Net.handleClientReceived(c);
                }catch (Exception e){
                    Gdx.app.postRunnable(() -> {throw new RuntimeException(e);});
                }
            }

            @Override
            public void received (Connection connection, Object object) {
                if(object instanceof FrameworkMessage) return;

                try{
                    Net.handleClientReceived(object);
                }catch (Exception e){
                    if(e instanceof KryoNetException && e.getMessage() != null && e.getMessage().toLowerCase().contains("incorrect")) {
                        Gdx.app.postRunnable(() -> Vars.ui.showError("$text.server.mismatch"));
                        Vars.netClient.disconnectQuietly();
                    }else{
                        Gdx.app.postRunnable(() -> {
                            throw new RuntimeException(e);
                        });
                    }
                }
            }
        };

        if(KryoRegistrator.fakeLag){
            client.addListener(new LagListener(0, KryoRegistrator.fakeLagAmount, listener));
        }else{
            client.addListener(listener);
        }

        register(Registrator.getClasses());
    }

    @Override
    public void connect(String ip, int port) throws IOException {
        //just in case
        client.stop();

        Thread updateThread = new Thread(() -> {
            try{
                client.run();
            }catch (Exception e){
                handleException(e);
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
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<IOException> invalid){
        Thread thread = new Thread(() -> {
            try {

                Serialization ser = (Serialization) UCore.getPrivate(client, "serialization");
                DatagramSocket socket = new DatagramSocket();
                ByteBuffer dataBuffer = ByteBuffer.allocate(64);
                ser.write(dataBuffer, new DiscoverHost());

                dataBuffer.flip();
                byte[] data = new byte[dataBuffer.limit()];
                dataBuffer.get(data);
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(address), port));

                socket.setSoTimeout(2000);

                addresses.clear();

                DatagramPacket packet = handler.onRequestNewDatagramPacket();

                socket.receive(packet);

                handler.onDiscoveredHost(packet);

                Host host = addresses.values().next();

                if (host != null) {
                    Gdx.app.postRunnable(() -> valid.accept(host));
                } else {
                    Gdx.app.postRunnable(() -> invalid.accept(new IOException("Outdated server.")));
                }
            } catch (IOException e) {
                Gdx.app.postRunnable(() -> invalid.accept(e));
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Array<Host> discover(){
        addresses.clear();
        List<InetAddress> list = client.discoverHosts(Vars.port, 3000);
        ObjectSet<String> hostnames = new ObjectSet<>();
        Array<Host> result = new Array<>();

        for(InetAddress a : list){
            if(!hostnames.contains(a.getHostName())) {
                Host address = addresses.get(a);
                if(address != null) result.add(address);

            }
            hostnames.add(a.getHostName());
        }

        return result;
    }

    @Override
    public void register(Class<?>... types) { }

    @Override
    public void dispose(){
        try {
            client.dispose();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private void handleException(Exception e){
        e.printStackTrace();
        if(e instanceof KryoNetException){
            Gdx.app.postRunnable(() -> Vars.ui.showError("$text.server.mismatch"));
        }else{
            //TODO better exception handling.
            disconnect();
        }
    }

}
