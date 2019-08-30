package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamFriends.*;
import com.codedisaster.steamworks.SteamMatchmaking.*;
import com.codedisaster.steamworks.SteamNetworking.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;

import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

import static io.anuke.mindustry.Vars.*;

public class SteamCoreNetImpl implements SteamNetworkingCallback, SteamMatchmakingCallback, SteamFriendsCallback, ClientProvider, ServerProvider{
    public final SteamNetworking snet = new SteamNetworking(this);
    public final SteamMatchmaking smat = new SteamMatchmaking(this);
    public final SteamFriends friends = new SteamFriends(this);

    final PacketSerializer serializer = new PacketSerializer();
    final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024 * 4);
    final ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024 * 4);

    final CopyOnWriteArrayList<SteamConnection> connections = new CopyOnWriteArrayList<>();
    final IntMap<SteamConnection> steamConnections = new IntMap<>(); //maps steam ID -> valid net connection
    final ObjectMap<String, SteamID> lobbyIDs = new ObjectMap<>();

    SteamID currentLobby, currentServer;
    Consumer<Host> lobbyCallback;
    Runnable lobbyDoneCallback, joinCallback;

    public SteamCoreNetImpl(){
        Events.on(ClientLoadEvent.class, e -> Core.app.addListener(new ApplicationListener(){
            //read packets
            int length;
            SteamID from = new SteamID();

            @Override
            public void update(){
                while((length = snet.isP2PPacketAvailable(0)) != 0){
                    try{
                        readBuffer.position(0);
                        snet.readP2PPacket(from, readBuffer, 0);
                        int fromID = from.getAccountID();
                        Object output = serializer.read(readBuffer);

                        if(Net.server()){
                            SteamConnection con = steamConnections.get(fromID);
                            if(con != null){
                                Net.handleServerReceived(con.id, output);
                            }else{
                                Log.err("Unknown user with ID: {0}", fromID);
                            }
                        }else if(currentServer != null && fromID == currentServer.getAccountID()){
                            Net.handleClientReceived(output);
                        }
                    }catch(SteamException e){
                        e.printStackTrace();
                    }
                }
            }
        }));

        Events.on(WaveEvent.class, e -> {
            if(currentLobby != null && Net.server()){
                smat.setLobbyData(currentLobby, "wave", state.wave + "");
            }
        });
    }

    @Override
    public void connect(String ip, int port, Runnable success) throws IOException{
        if(ip.startsWith("steam:")){
            String lobbyname = ip.substring("steam:".length());
            SteamID lobby = lobbyIDs.get(lobbyname);
            if(lobby == null) throw new IOException("Lobby not found.");
            joinCallback = success;
            smat.joinLobby(lobby);
        }
        //else, no
    }

    @Override
    public void sendClient(Object object, SendMode mode){
        if(currentServer == null){
            Log.info("Not connected, quitting.");
            return;
        }

        try{
            writeBuffer.limit(writeBuffer.capacity());
            writeBuffer.position(0);
            serializer.write(writeBuffer, object);
            writeBuffer.flip();

            snet.sendP2PPacket(currentServer, writeBuffer, mode == SendMode.tcp ? P2PSend.Reliable : P2PSend.UnreliableNoDelay, 0);
        }catch(Exception e){
            Net.showError(e);
        }
        Pools.free(object);
    }

    @Override
    public void updatePing(){
        //no
    }

    @Override
    public int getPing(){
        //absolutely not
        return 0;
    }

    @Override
    public void disconnect(){
        if(currentLobby != null){
            smat.leaveLobby(currentLobby);
            snet.closeP2PSessionWithUser(currentServer);
            currentServer = null;
            currentLobby = null;
            Net.handleClientReceived(new Disconnect());
        }
    }

    @Override
    public void discover(Consumer<Host> callback, Runnable done){
        smat.addRequestLobbyListResultCountFilter(32);
        smat.requestLobbyList();
        lobbyCallback = callback;
        lobbyDoneCallback = done;
    }

    @Override
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed){
        //no
    }

    @Override
    public void host(int port) throws IOException{
        smat.createLobby(Core.settings.getBool("publichost") ? LobbyType.Public : LobbyType.FriendsOnly, 32);
    }

    public void updateLobby(){
        if(currentLobby != null && Net.server()){
            smat.setLobbyType(currentLobby, Core.settings.getBool("publichost") ? LobbyType.Public : LobbyType.FriendsOnly);
        }
    }

    @Override
    public void close(){
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

    void disconnectSteamUser(SteamID steamid){
        //a client left
        int sid = steamid.getAccountID();
        snet.closeP2PSessionWithUser(steamid);

        if(steamConnections.containsKey(sid)){
            SteamConnection con = steamConnections.get(sid);
            Net.handleServerReceived(con.id, new Disconnect());
            steamConnections.remove(sid);
            connections.remove(con);
        }
    }

    @Override
    public void onFavoritesListChanged(int i, int i1, int i2, int i3, int i4, boolean b, int i5){

    }

    @Override
    public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID){
        Log.info("lobby invite {0} {1} {2}", steamIDLobby.getAccountID(), steamIDUser.getAccountID(), gameID);

        //ignore invites when hosting.
        if(Net.server()) return;

        ui.showConfirm("Someone has invited you to a game.", "But do you accept?", () -> {
            smat.joinLobby(steamIDLobby);
        });
    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, ChatRoomEnterResponse response){
        Log.info("enter lobby {0} {1}", steamIDLobby.getAccountID(), response);
        currentLobby = steamIDLobby;
        currentServer = smat.getLobbyOwner(steamIDLobby);

        if(joinCallback != null){
            joinCallback.run();
            joinCallback = null;
        }

        Connect con = new Connect();
        con.addressTCP = "steam:" + currentServer.getAccountID();

        Net.setClientConnected();
        Net.handleClientReceived(con);
    }

    @Override
    public void onLobbyDataUpdate(SteamID steamID, SteamID steamID1, boolean b){

    }

    @Override
    public void onLobbyChatUpdate(SteamID lobby, SteamID who, SteamID changer, ChatMemberStateChange change){
        Log.info("lobby {0}: {1} caused {2}'s change: {3}", lobby.getAccountID(), who.getAccountID(), changer.getAccountID(), change);
        if(change == ChatMemberStateChange.Disconnected || change == ChatMemberStateChange.Left){
            if(Net.client()){
                Log.info("Current host left.");
                //host left, leave as well
                if(who == currentServer){
                    disconnect();
                }
            }else{
                //a client left
                disconnectSteamUser(who);
            }
        }

    }

    @Override
    public void onLobbyChatMessage(SteamID steamID, SteamID steamID1, ChatEntryType chatEntryType, int i){

    }

    @Override
    public void onLobbyGameCreated(SteamID steamID, SteamID steamID1, int i, short i1){

    }

    @Override
    public void onLobbyMatchList(int matches){
        Log.info("found {0} matches", matches);

        if(lobbyDoneCallback != null){
            for(int i = 0; i < matches; i++){
                SteamID lobby = smat.getLobbyByIndex(i);
                Host out = new Host(
                    smat.getLobbyData(lobby, "name"),
                    "steam:" + lobby.getAccountID(),
                    smat.getLobbyData(lobby, "mapname"),
                    Strings.parseInt(smat.getLobbyData(lobby, "wave"), -1),
                    smat.getNumLobbyMembers(lobby),
                    Strings.parseInt(smat.getLobbyData(lobby, "name"), -1),
                smat.getLobbyData(lobby, "versionType"));

                lobbyIDs.put(lobby.getAccountID() + "", lobby);

                lobbyCallback.accept(out);
            }

            lobbyDoneCallback.run();
        }
    }

    @Override
    public void onLobbyKicked(SteamID steamID, SteamID steamID1, boolean b){
        Log.info("Kicked: {0} {1} {2}", steamID, steamID1, b);
    }

    @Override
    public void onLobbyCreated(SteamResult result, SteamID steamID){
        if(!Net.server()){
            Log.info("Lobby created on server: {0}, ignoring.", steamID);
            return;
        }

        Log.info("callback run ON SERVER");
        Log.info("Lobby create callback");
        Log.info("Lobby {1} created? {0}", result, steamID.getAccountID());
        if(result == SteamResult.OK){
            currentLobby = steamID;

            smat.setLobbyData(steamID, "name", player.name);
            smat.setLobbyData(steamID, "mapname", world.getMap() == null ? "Unknown" : world.getMap().name());
            smat.setLobbyData(steamID, "version", Version.build + "");
            smat.setLobbyData(steamID, "versionType", Version.type);
            smat.setLobbyData(steamID, "wave", state.wave + "");

            friends.activateGameOverlayInviteDialog(currentLobby);
            Log.info("Activating overlay dialog");
        }
    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult steamResult){

    }

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError){
        if(Net.server()){
            Log.info("{0} has disconnected: {1}", steamIDRemote.getAccountID(), sessionError);
            disconnectSteamUser(steamIDRemote);
        }else if(steamIDRemote == currentServer){
            Log.info("Disconnected! {1}: {0}", steamIDRemote.getAccountID(), sessionError);
            Net.handleClientReceived(new Disconnect());
        }
    }

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote){
        Log.info("Connection request: {0}", steamIDRemote.getAccountID());
        if(currentServer != null && !Net.server()){
            Log.info("Am client");
            if(steamIDRemote == currentServer){
                snet.acceptP2PSessionWithUser(steamIDRemote);
            }
        }else if(Net.server()){
            Log.info("Am server, accepting request.");
            //accept users on request
            if(!steamConnections.containsKey(steamIDRemote.getAccountID())){
                SteamConnection con = new SteamConnection(steamIDRemote);
                Connect c = new Connect();
                c.id = con.id;
                c.addressTCP = "steam:" + steamIDRemote.getAccountID();

                Log.info("&bRecieved connection: {0}", c.addressTCP);

                steamConnections.put(steamIDRemote.getAccountID(), con);
                connections.add(con);
                Net.handleServerReceived(c.id, c);
            }

            snet.acceptP2PSessionWithUser(steamIDRemote);
        }
    }

    @Override
    public void onSetPersonaNameResponse(boolean b, boolean b1, SteamResult steamResult){

    }

    @Override
    public void onPersonaStateChange(SteamID steamID, PersonaChange personaChange){

    }

    @Override
    public void onGameOverlayActivated(boolean b){

    }

    @Override
    public void onGameLobbyJoinRequested(SteamID steamID, SteamID steamID1){

    }

    @Override
    public void onAvatarImageLoaded(SteamID steamID, int i, int i1, int i2){

    }

    @Override
    public void onFriendRichPresenceUpdate(SteamID steamID, int i){

    }

    @Override
    public void onGameRichPresenceJoinRequested(SteamID steamID, String s){

    }

    @Override
    public void onGameServerChangeRequested(String s, String s1){

    }

    public class SteamConnection extends NetConnection{
        final SteamID sid;
        final P2PSessionState state = new P2PSessionState();

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

                snet.sendP2PPacket(sid, writeBuffer, mode == SendMode.tcp ? object instanceof StreamChunk ? P2PSend.ReliableWithBuffering : P2PSend.Reliable : P2PSend.UnreliableNoDelay, 0);
            }catch(Exception e){
                Log.err(e);
                Log.info("Error sending packet. Disconnecting invalid client!");
                close();

                SteamConnection k = steamConnections.get(sid.getAccountID());
                if(k != null) steamConnections.remove(sid.getAccountID());
            }
        }

        @Override
        public boolean isConnected(){
            snet.getP2PSessionState(sid, state);
            return state.isConnectionActive();
        }

        @Override
        public void close(){
            snet.closeP2PSessionWithUser(sid);
        }
    }
}
