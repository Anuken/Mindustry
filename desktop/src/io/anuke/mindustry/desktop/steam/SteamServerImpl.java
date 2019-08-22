package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamMatchmaking.*;
import com.codedisaster.steamworks.SteamNetworking.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.async.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Packets.*;
import net.jpountz.lz4.*;

import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

public class SteamServerImpl implements ServerProvider, SteamNetworkingCallback, SteamMatchmakingCallback{
    private final static int maxLobbyPlayers = 32;

    final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
    final PacketSerializer serializer = new PacketSerializer();
    final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024 * 4);
    final ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024 * 4);
    final SteamNetworking snet = new SteamNetworking(this);
    final SteamMatchmaking smat = new SteamMatchmaking(this);
    final SteamFriendsImpl friends = new SteamFriendsImpl();
    final CopyOnWriteArrayList<SteamConnection> connections = new CopyOnWriteArrayList<>();
    //private final ServerProvider server;

    //maps steam ID -> valid net connection
    IntMap<SteamConnection> steamConnections = new IntMap<>();
    SteamID currentLobby;

    public SteamServerImpl(ServerProvider server){
        //this.server = server;

        //start recieving packets
        Threads.daemon(() -> {
            int length;
            SteamID from = new SteamID();
            while(true){
                while((length = snet.isP2PPacketAvailable(0)) != 0){
                    try{
                        readBuffer.position(0);
                        snet.readP2PPacket(from, readBuffer, 0);
                        int fromID = from.getAccountID();
                        Object output = serializer.read(readBuffer);

                        Core.app.post(() -> {
                            SteamConnection con = steamConnections.get(fromID);
                            if(con != null){
                                Net.handleServerReceived(con.id, output);
                            }else{
                                Log.err("Unknown user with ID: {0}", fromID);
                            }
                        });
                    }catch(SteamException e){
                        e.printStackTrace();
                    }
                }

                Threads.sleep(1000 / 10);
            }
        });
    }

    //server overrides

    @Override
    public void host(int port) throws IOException{
        //server.host(port);
        smat.createLobby(LobbyType.values()[Core.settings.getInt("lobbytype", 1)], maxLobbyPlayers);
    }

    @Override
    public void close(){
       // server.close();
        if(currentLobby != null){
            smat.leaveLobby(currentLobby);
            for(SteamConnection con : steamConnections.values()){
                con.close();
            }
            currentLobby = null;
        }

        steamConnections.clear();
    }

    @Override
    public byte[] compressSnapshot(byte[] input){
        return compressor.compress(input);
    }

    @Override
    public Iterable<? extends NetConnection> getConnections(){
        return connections;
    }

    @Override
    public NetConnection getByID(int id){
        for(int i = 0; i < connections.size(); i++){
            SteamConnection con = connections.get(i);
            if(con.id == id){
                return con;
            }
        }

        return null;
    }

    //steam lobby overrides

    @Override
    public void onFavoritesListChanged(int ip, int queryPort, int connPort, int appID, int flags, boolean add, int accountID){

    }

    @Override
    public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID){

    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, ChatRoomEnterResponse response){

    }

    @Override
    public void onLobbyDataUpdate(SteamID steamIDLobby, SteamID steamIDMember, boolean success){

    }

    @Override
    public void onLobbyChatUpdate(SteamID steamIDLobby, SteamID steamIDUserChanged, SteamID steamIDMakingChange, ChatMemberStateChange stateChange){

    }

    @Override
    public void onLobbyChatMessage(SteamID steamIDLobby, SteamID steamIDUser, ChatEntryType entryType, int chatID){

    }

    @Override
    public void onLobbyGameCreated(SteamID steamIDLobby, SteamID steamIDGameServer, int ip, short port){

    }

    @Override
    public void onLobbyMatchList(int lobbiesMatching){

    }

    @Override
    public void onLobbyKicked(SteamID steamIDLobby, SteamID steamIDAdmin, boolean kickedDueToDisconnect){

    }

    @Override
    public void onLobbyCreated(SteamResult result, SteamID steamIDLobby){
        Log.info("Lobby create callback");
        Log.info("Lobby {1} created? {0}", result, steamIDLobby.getAccountID());
        if(result == SteamResult.OK){
            currentLobby = steamIDLobby;
            friends.friends.activateGameOverlayInviteDialog(currentLobby);
        }
    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult result){

    }

    //steam p2p network overrides

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError){
        Log.info("{0} has disconnected: {1}", steamIDRemote.getAccountID(), sessionError);

        if(Net.server()){
            int id = steamIDRemote.getAccountID();

            if(steamConnections.containsKey(id)){
                Net.handleServerReceived(id, new Disconnect());
                steamConnections.remove(id);
            }
        }
    }

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote){
        //accept users on request
        snet.acceptP2PSessionWithUser(steamIDRemote);
        if(!steamConnections.containsKey(steamIDRemote.getAccountID())){
            SteamConnection con = new SteamConnection(steamIDRemote);
            Connect c = new Connect();
            c.id = con.id;
            c.addressTCP = "steam:" + steamIDRemote.getAccountID();

            Log.debug("&bRecieved connection: {0}", c.addressTCP);

            connections.add(con);
            Core.app.post(() -> Net.handleServerReceived(c.id, c));
        }
    }

    public class SteamConnection extends NetConnection{
        final SteamID sid;

        public SteamConnection(SteamID sid){
            super(sid.getAccountID() + "");
            this.sid = sid;
        }

        @Override
        public void send(Object object, SendMode mode){
            try{
                writeBuffer.limit(writeBuffer.capacity());
                writeBuffer.position(0);
                serializer.write(writeBuffer, object);
                writeBuffer.flip();

                snet.sendP2PPacket(sid, writeBuffer, mode == SendMode.tcp ? P2PSend.Reliable : P2PSend.UnreliableNoDelay, 0);
            }catch(Exception e){
                Log.err(e);
                Log.info("Error sending packet. Disconnecting invalid client!");
                close();

                SteamConnection k = steamConnections.get(sid.getAccountID());
                if(k != null) steamConnections.remove(sid.getAccountID());
            }
        }

        @Override
        public void close(){
            snet.closeP2PSessionWithUser(sid);
        }
    }
}
