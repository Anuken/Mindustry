package io.anuke.mindustry.net;

import io.anuke.arc.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.async.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mnet.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import static io.anuke.mindustry.Vars.*;

public class MClient implements ClientProvider, ApplicationListener{
    MSocket socket;

    public MClient(){
        Events.on(AppLoadEvent.class, event -> Core.app.addListener(this));
    }

    public void connect(String ip, int port, Runnable success) throws IOException{
        socket = new MSocket(InetAddress.getByName(ip), port, PacketSerializer::new);
        socket.addDcListener((sock, reason) -> Core.app.post(() -> Net.handleClientReceived(new Disconnect())));
        socket.connectAsync(null, 2000, response -> {
            if(response.getType() == ResponseType.ACCEPTED){
                Core.app.post(() -> {
                    success.run();
                    Net.handleClientReceived(new Connect());
                });
            }else if(response.getType() == ResponseType.WRONG_STATE){
                Core.app.post(() -> Net.showError(new IOException("alreadyconnected")));
            }else{
                Core.app.post(() -> Net.showError(new IOException("connection refused")));
            }
        });
    }

    @Override
    public void update(){
        if(socket == null) return;

        socket.update((sock, object) -> {
            try{
                Net.handleClientReceived(object);
            }catch(Exception e){
                Net.showError(e);
                netClient.disconnectQuietly();
            }
        });
    }

    @Override
    public void updatePing(){

    }

    @Override
    public void dispose(){
        disconnect();
    }

    public void send(Object object, SendMode mode){
        if(mode == SendMode.tcp){
            socket.send(object);
        }else{
            socket.sendUnreliable(object);
        }

        Pools.free(object);
    }

    public int getPing(){
        return socket == null ? 0 : (int)socket.getPing();
    }

    public void disconnect(){
        if(socket != null) socket.close();
    }

    public void discover(Consumer<Host> callback, Runnable done){
        Threads.daemon(() -> {
            byte[] bytes = new byte[512];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            ArrayList<InetAddress> foundAddresses = new ArrayList<>();

            try(DatagramSocket socket = new DatagramSocket()){
                broadcast(port, socket);

                socket.setSoTimeout(4000);

                outer:
                while(true){

                    try{
                        socket.receive(packet);
                    }catch(SocketTimeoutException ex){
                        done.run();
                        return;
                    }

                    buffer.position(0);

                    InetAddress address = ((InetSocketAddress)packet.getSocketAddress()).getAddress();

                    for(InetAddress other : foundAddresses){
                        if(other.equals(address) || (isLocal(other) && isLocal(address))){
                            continue outer;
                        }
                    }

                    Host host = NetworkIO.readServerData(address.getHostName(), buffer);
                    callback.accept(host);
                    foundAddresses.add(address);
                }
            }catch(IOException ex){
                done.run();
            }
        });
    }

    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed){
        Threads.daemon(() -> {
            try{
                DatagramPacket packet = new DatagramPacket(new byte[512], 512);

                DatagramSocket socket = new DatagramSocket();
                socket.send(new DatagramPacket(new byte[]{-2}, 1, InetAddress.getByName(address), port));
                socket.setSoTimeout(4000);
                socket.receive(packet);

                ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                Host host = NetworkIO.readServerData(packet.getAddress().getHostAddress(), buffer);

                Core.app.post(() -> valid.accept(host));
            }catch(Exception e){
                Core.app.post(() -> failed.accept(e));
            }
        });
    }
    private void broadcast (int udpPort, DatagramSocket socket) throws IOException{
        byte[] data = {-2};

        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())){
            for (InetAddress address : Collections.list(iface.getInetAddresses())){

                byte[] ip = address.getAddress(); //255.255.255.255
                try{
                    socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), udpPort));
                }catch (Exception ignored){}
                ip[3] = -1; //255.255.255.0
                try{
                    socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), udpPort));
                }catch (Exception ignored){}
                ip[2] = -1; //255.255.0.0
                try{
                    socket.send(new DatagramPacket(data, data.length, InetAddress.getByAddress(ip), udpPort));
                }catch (Exception ignored){}
            }
        }
    }

    private boolean isLocal(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) return true;

        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
