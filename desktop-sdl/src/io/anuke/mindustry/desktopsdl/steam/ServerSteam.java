package io.anuke.mindustry.desktopsdl.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamMatchmaking.*;
import com.codedisaster.steamworks.SteamNetworking.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.desktopsdl.steam.ClientSteam.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.*;

import java.io.*;
import java.nio.*;

public class ServerSteam implements ServerProvider, SteamNetworkingCallback, SteamMatchmakingCallback{
    private final static int maxLobbyPlayers = 32;

    private final SteamNetworking snet = new SteamNetworking(this);
    private final SteamMatchmaking smat = new SteamMatchmaking(this);
    private final ServerProvider server;

    //buffer for steam API calls
    private ByteBuffer steamBuffer = ByteBuffer.allocateDirect(1024 * 128);
    //maps steam ID -> valid net connection
    private IntMap<SteamConnection> steamConnections = new IntMap<>();

    private SteamID currentLobby;

    public ServerSteam(ServerProvider server){
        this.server = server;
    }

    //server overrides

    @Override
    public void host(int port) throws IOException{
        server.host(port);
        smat.createLobby(LobbyType.values()[Core.settings.getInt("lobbytype", 1)], maxLobbyPlayers);
    }

    @Override
    public void sendStream(int id, Streamable stream){
        server.sendStream(id, stream);
    }

    @Override
    public void send(Object object, SendMode mode){
        server.send(object, mode);
    }

    @Override
    public void sendTo(int id, Object object, SendMode mode){
        server.sendTo(id, object, mode);
    }

    @Override
    public void sendExcept(int id, Object object, SendMode mode){
        server.sendExcept(id, object, mode);
    }

    @Override
    public void close(){
        server.close();
        if(currentLobby != null){
            //TODO kick everyone who is in this lobby?
            smat.leaveLobby(currentLobby);
            currentLobby = null;
        }
    }

    @Override
    public byte[] compressSnapshot(byte[] input){
        return server.compressSnapshot(input);
    }

    @Override
    public Array<? extends NetConnection> getConnections(){
        return server.getConnections();
    }

    @Override
    public NetConnection getByID(int id){
        return server.getByID(id);
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
        }
    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult result){

    }

    //steam p2p network overrides

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError){

    }

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote){
        //accept users on request
        snet.acceptP2PSessionWithUser(steamIDRemote);
    }
}
