package io.anuke.mindustry.net;

import io.anuke.arc.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mnet.*;
import io.anuke.mnet.MServerSocket;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.*;

public class MServer implements ServerProvider, ApplicationListener{
    final CopyOnWriteArrayList<MConnectionImpl> connections = new CopyOnWriteArrayList<>();
    MServerSocket socket;

    public MServer(){
        Events.on(AppLoadEvent.class, e -> {
            Core.app.addListener(this);
        });
    }

    public void host(int port) throws IOException{
        socket = new MServerSocket(port, con -> {
            MSocket sock = con.accept(null);

            MConnectionImpl kn = new MConnectionImpl(sock);
            sock.setUserData(kn);

            String ip = sock.getRemoteAddress().getHostAddress();

            Connect c = new Connect();
            c.id = kn.id;
            c.addressTCP = ip;

            Log.info("&bRecieved connection: {0} / {1}", c.id, c.addressTCP);

            connections.add(kn);
            Core.app.post(() -> Net.handleServerReceived(kn.id, c));

            sock.addDcListener((socket, message) -> {
                Log.info("&bLost connection {0}. Reason: {1}", kn.id, message);

                Disconnect dc = new Disconnect();
                dc.id = kn.id;

                Core.app.post(() -> {
                    Net.handleServerReceived(kn.id, dc);
                    connections.remove(kn);
                });
            });
        }, PacketSerializer::new, () -> {
            ByteBuffer buf = NetworkIO.writeServerData();
            byte[] bytes = buf.array();
            return new DatagramPacket(bytes, bytes.length);
        });

        connections.clear();
    }

    public void update(){
        if(socket == null) return;

        socket.update();
        for(MSocket socket : socket.getSockets()){
            MConnectionImpl c = socket.getUserData();
            socket.update((s, msg) -> Core.app.post(() -> {
                try{
                    Net.handleServerReceived(c.id, msg);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }));
        }
    }

    public void close(){
        if(socket != null) socket.close();
    }

    public Iterable<? extends NetConnection> getConnections(){
        return connections;
    }

    public MConnectionImpl getByID(int id){
        for(MConnectionImpl n : connections){
            if(n.id == id){
                return n;
            }
        }
        return null;
    }

    class MConnectionImpl extends NetConnection{
        private final MSocket sock; //sock.

        public MConnectionImpl(MSocket con){
            super(con.getRemoteAddress().getHostAddress());
            this.sock = con;
        }

        @Override
        public void send(Object object, SendMode mode){
            if(mode == SendMode.tcp){
                sock.send(object);
            }else{
                sock.sendUnreliable(object);
            }

        }

        @Override
        public void close(){
            sock.close();
        }
    }
}
