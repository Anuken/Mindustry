package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.esotericsoftware.kryonet.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Address;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Registrator;
import io.anuke.ucore.UCore;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class KryoClient implements ClientProvider{
    Client client;
    ObjectMap<InetAddress, Address> addresses = new ObjectMap<>();

    public KryoClient(){
        client = new Client();
        client.setDiscoveryHandler(new ClientDiscoveryHandler() {
            @Override
            public DatagramPacket onRequestNewDatagramPacket() {
                return new DatagramPacket(new byte[32], 32);
            }

            @Override
            public void onDiscoveredHost(DatagramPacket datagramPacket) {
                ByteBuffer buffer = ByteBuffer.wrap(datagramPacket.getData());
                Address address = KryoRegistrator.readServerData(datagramPacket.getAddress(), buffer);
                addresses.put(datagramPacket.getAddress(), address);
                UCore.log("Host data found: " + Arrays.toString(datagramPacket.getData()));
            }

            @Override
            public void onFinally() {

            }
        });

        client.addListener(new Listener(){
            @Override
            public void connected (Connection connection) {
                Connect c = new Connect();
                c.id = connection.getID();
                c.addressTCP = connection.getRemoteAddressTCP().toString();

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
                        UCore.log("Mismatch!");
                    }else{
                        Gdx.app.postRunnable(() -> {
                            throw new RuntimeException(e);
                        });
                    }
                }

            }
        });

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
    public Array<Address> discover(){
        List<InetAddress> list = client.discoverHosts(Vars.port, 3000);
        ObjectSet<String> hostnames = new ObjectSet<>();
        Array<Address> result = new Array<>();

        for(InetAddress a : list){
            if(!hostnames.contains(a.getHostName())) {
                Address address = addresses.get(a);
                if(address != null) result.add(address);

            }
            hostnames.add(a.getHostName());
        }

        return result;
    }

    @Override
    public void register(Class<?>... types) {
        for(Class<?> c : types){
            client.getKryo().register(c);
        }
        KryoRegistrator.register(client.getKryo());
    }

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
