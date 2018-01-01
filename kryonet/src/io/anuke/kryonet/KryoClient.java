package io.anuke.kryonet;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Registrator;

import java.io.IOException;

public class KryoClient implements ClientProvider{
    Client client;

    public KryoClient(){
        client = new Client();
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
                    Gdx.app.exit();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void disconnected (Connection connection) {
                Disconnect c = new Disconnect();

                try{
                    Net.handleClientReceived(c);
                }catch (Exception e){
                    Gdx.app.exit();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void received (Connection connection, Object object) {
                if(object instanceof FrameworkMessage) return;

                try{
                    Net.handleClientReceived(object);
                }catch (Exception e){
                    Gdx.app.exit();
                    throw new RuntimeException(e);
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
    public void register(Class<?>... types) {
        for(Class<?> c : types){
            client.getKryo().register(c);
        }
    }
}
