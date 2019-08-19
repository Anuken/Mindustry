package io.anuke.mindustry.desktopsdl.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamNetworking.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.*;

import java.nio.*;

public class ClientSteam implements SteamNetworkingCallback{
    private SteamNetworking steam;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 128);
    //maps steam ID -> valid net connection
    private IntMap<SteamConnection> connections = new IntMap<>();

    public ClientSteam(){
        steam = new SteamNetworking(this);

        new Thread(() -> {
            int length;
            SteamID from = new SteamID();
            while((length = steam.isP2PPacketAvailable(0)) != 0){
                try{
                    buffer.position(0);
                    steam.readP2PPacket(from, buffer, 0);
                }catch(SteamException e){
                    e.printStackTrace();
                }
            }
        }){{
            setDaemon(true);
        }}.start();
    }

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError){
        Log.info("{0} has disconnected: {1}", steamIDRemote.getAccountID(), sessionError);
    }

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote){
        steam.acceptP2PSessionWithUser(steamIDRemote);
    }

    class SteamConnection extends NetConnection{
        public final SteamID connection;

        public SteamConnection(int id, String address, SteamID connection){
            super(id, address);
            this.connection = connection;
        }

        @Override
        public boolean isConnected(){
            return false;//connection.isConnected();
        }

        @Override
        public void send(Object object, SendMode mode){
            //TODO
            /*
            try{
                if(mode == SendMode.tcp){
                    connection.sendTCP(object);
                }else{
                    connection.sendUDP(object);
                }
            }catch(Exception e){
                Log.err(e);
                Log.info("Error sending packet. Disconnecting invalid client!");
                connection.close();

                ArcNetServer.KryoConnection k = getByKryoID(connection.getID());
                if(k != null) connections.remove(k);
            }*/
        }

        @Override
        public void close(){
            //TODO
            //if(connection.isConnected()) connection.close();
        }
    }
}
