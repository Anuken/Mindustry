package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
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
import java.util.Arrays;
import java.util.List;

public class KryoClient implements ClientProvider{
    Client client;

    public KryoClient(){
        client = new Client();
        client.setDiscoveryHandler(new ClientDiscoveryHandler() {
            @Override
            public DatagramPacket onRequestNewDatagramPacket() {
                return new DatagramPacket(new byte[4], 4);
            }

            @Override
            public void onDiscoveredHost(DatagramPacket datagramPacket) {
                //TODO doesn't send data
                UCore.log("DATA HOST FOUND: " + Arrays.toString(datagramPacket.getData()));
            }

            @Override
            public void onFinally() {

            }
        });
        client.start();
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
                    Gdx.app.postRunnable(() -> {throw new RuntimeException(e);});
                }

            }
        });

        register(Registrator.getClasses());
    }

    @Override
    public void connect(String ip, int port) throws IOException {
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
            if(!hostnames.contains(a.getHostName()))
                result.add(new Address(a.getCanonicalHostName(), a.getHostAddress()));
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

}
