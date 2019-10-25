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
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.core.Version;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.net.ArcNetImpl.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;

import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

import static io.anuke.mindustry.Vars.*;

public class SNet implements SteamNetworkingCallback, SteamMatchmakingCallback, SteamFriendsCallback, NetProvider{
    public final SteamNetworking snet = new SteamNetworking(this);
    public final SteamMatchmaking smat = new SteamMatchmaking(this);
    public final SteamFriends friends = new SteamFriends(this);

    final NetProvider provider;

    final PacketSerializer serializer = new PacketSerializer();
    final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024 * 4);
    final ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024 * 4);

    final CopyOnWriteArrayList<SteamConnection> connections = new CopyOnWriteArrayList<>();
    final IntMap<SteamConnection> steamConnections = new IntMap<>(); //maps steam ID -> valid net connection

    SteamID currentLobby, currentServer;
    Consumer<Host> lobbyCallback;
    Runnable lobbyDoneCallback, joinCallback;

    public SNet(NetProvider provider){
        this.provider = provider;

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

                        if(net.server()){
                            SteamConnection con = steamConnections.get(fromID);
                            try{
                                //accept users on request
                                if(con == null){
                                    con = new SteamConnection(SteamID.createFromNativeHandle(SteamNativeHandle.getNativeHandle(from)));
                                    Connect c = new Connect();
                                    c.addressTCP = "steam:" + from.getAccountID();

                                    Log.info("&bRecieved STEAM connection: {0}", c.addressTCP);

                                    steamConnections.put(from.getAccountID(), con);
                                    connections.add(con);
                                    net.handleServerReceived(con, c);
                                }

                                net.handleServerReceived(con, output);
                            }catch(RuntimeException e){
                                if(e.getCause() instanceof ValidateException){
                                    ValidateException v = (ValidateException)e.getCause();
                                    Log.err("Validation failed: {0} ({1})", v.player.name, v.getMessage());
                                }else{
                                    Log.err(e);
                                }
                            }catch(Exception e){
                                Log.err(e);
                            }
                        }else if(currentServer != null && fromID == currentServer.getAccountID()){
                            net.handleClientReceived(output);
                        }
                    }catch(SteamException e){
                        e.printStackTrace();
                    }
                }
            }
        }));

        Events.on(WaveEvent.class, e -> {
            if(currentLobby != null && net.server()){
                smat.setLobbyData(currentLobby, "wave", state.wave + "");
            }
        });
    }

    public boolean isSteamClient(){
        return currentServer != null;
    }

    @Override
    public void connectClient(String ip, int port, Runnable success) throws IOException{
        if(ip.startsWith("steam:")){
            String lobbyname = ip.substring("steam:".length());
            try{
                SteamID lobby = SteamID.createFromNativeHandle(Long.parseLong(lobbyname));
                joinCallback = success;
                smat.joinLobby(lobby);
            }catch(NumberFormatException e){
                throw new IOException("Invalid Steam ID: " + lobbyname);
            }
        }else{
            provider.connectClient(ip, port, success);
        }
    }

    @Override
    public void sendClient(Object object, SendMode mode){
        if(isSteamClient()){
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
                net.showError(e);
            }
            Pools.free(object);
        }else{
            provider.sendClient(object, mode);
        }
    }


    @Override
    public void disconnectClient(){
        if(isSteamClient()){
            if(currentLobby != null){
                smat.leaveLobby(currentLobby);
                snet.closeP2PSessionWithUser(currentServer);
                currentServer = null;
                currentLobby = null;
                net.handleClientReceived(new Disconnect());
            }
        }else{
            provider.disconnectClient();
        }
    }

    @Override
    public void discoverServers(Consumer<Host> callback, Runnable done){
        smat.addRequestLobbyListResultCountFilter(32);
        smat.requestLobbyList();
        lobbyCallback = callback;
        lobbyDoneCallback = done;
    }

    @Override
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed){
        provider.pingHost(address, port, valid, failed);
    }

    @Override
    public void hostServer(int port) throws IOException{
        provider.hostServer(port);
        smat.createLobby(Core.settings.getBool("publichost") ? LobbyType.Public : LobbyType.FriendsOnly, 16);

        Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> Log.info("Server: {0}\nClient: {1}\nActive: {2}", net.server(), net.client(), net.active()))));
    }

    public void updateLobby(){
        if(currentLobby != null && net.server()){
            smat.setLobbyType(currentLobby, Core.settings.getBool("publichost") ? LobbyType.Public : LobbyType.FriendsOnly);
        }
    }

    @Override
    public void closeServer(){
        provider.closeServer();

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
        //merge provider connections
        CopyOnWriteArrayList<NetConnection> connectionsOut = new CopyOnWriteArrayList<>(connections);
        for(NetConnection c : provider.getConnections()) connectionsOut.add(c);
        return connectionsOut;
    }

    void disconnectSteamUser(SteamID steamid){
        //a client left
        int sid = steamid.getAccountID();
        snet.closeP2PSessionWithUser(steamid);

        if(steamConnections.containsKey(sid)){
            SteamConnection con = steamConnections.get(sid);
            net.handleServerReceived(con, new Disconnect());
            steamConnections.remove(sid);
            connections.remove(con);
        }
    }

    @Override
    public void onFavoritesListChanged(int i, int i1, int i2, int i3, int i4, boolean b, int i5){

    }

    @Override
    public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID){
        Log.info("onLobbyInvite {0} {1} {2}", steamIDLobby.getAccountID(), steamIDUser.getAccountID(), gameID);
    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, ChatRoomEnterResponse response){
        Log.info("enter lobby {0} {1}", steamIDLobby.getAccountID(), response);

        if(response != ChatRoomEnterResponse.Success){
            ui.loadfrag.hide();
            ui.showErrorMessage(Core.bundle.format("cantconnect", response.toString()));
            return;
        }

        if(net.active()){
            net.disconnect();
            net.closeServer();
            logic.reset();
            state.set(State.menu);
        }

        currentLobby = steamIDLobby;
        currentServer = smat.getLobbyOwner(steamIDLobby);

        Log.info("Connect to owner {0}: {1}", currentServer.getAccountID(), friends.getFriendPersonaName(currentServer));

        if(joinCallback != null){
            joinCallback.run();
            joinCallback = null;
        }

        Connect con = new Connect();
        con.addressTCP = "steam:" + currentServer.getAccountID();

        net.setClientConnected();
        net.handleClientReceived(con);

        Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> Log.info("Server: {0}\nClient: {1}\nActive: {2}", net.server(), net.client(), net.active()))));
    }

    @Override
    public void onLobbyDataUpdate(SteamID steamID, SteamID steamID1, boolean b){

    }

    @Override
    public void onLobbyChatUpdate(SteamID lobby, SteamID who, SteamID changer, ChatMemberStateChange change){
        Log.info("lobby {0}: {1} caused {2}'s change: {3}", lobby.getAccountID(), who.getAccountID(), changer.getAccountID(), change);
        if(change == ChatMemberStateChange.Disconnected || change == ChatMemberStateChange.Left){
            if(net.client()){
                //host left, leave as well
                if(who.equals(currentServer) || who.equals(currentLobby)){
                    net.disconnect();
                    Log.info("Current host left.");
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
        Log.info("found {0} matches {1}", matches, lobbyDoneCallback);

        if(lobbyDoneCallback != null){
            Array<Host> hosts = new Array<>();
            for(int i = 0; i < matches; i++){
                try{
                    SteamID lobby = smat.getLobbyByIndex(i);
                    Host out = new Host(
                        smat.getLobbyData(lobby, "name"),
                        "steam:" + SteamNativeHandle.getNativeHandle(lobby),
                        smat.getLobbyData(lobby, "mapname"),
                        Strings.parseInt(smat.getLobbyData(lobby, "wave"), -1),
                        smat.getNumLobbyMembers(lobby),
                        Strings.parseInt(smat.getLobbyData(lobby, "version"), -1),
                        smat.getLobbyData(lobby, "versionType"),
                        Gamemode.valueOf(smat.getLobbyData(lobby, "gamemode")),
                        smat.getLobbyMemberLimit(lobby)
                    );
                    hosts.add(out);
                }catch(Exception e){
                    Log.err(e);
                }
            }

            hosts.sort(Structs.comparingInt(h -> -h.players));
            hosts.each(lobbyCallback);

            lobbyDoneCallback.run();
        }
    }

    @Override
    public void onLobbyKicked(SteamID steamID, SteamID steamID1, boolean b){
        Log.info("Kicked: {0} {1} {2}", steamID, steamID1, b);
    }

    @Override
    public void onLobbyCreated(SteamResult result, SteamID steamID){
        if(!net.server()){
            Log.info("Lobby created on server: {0}, ignoring.", steamID);
            return;
        }

        Log.info("Lobby {1} created? {0}", result, steamID.getAccountID());
        if(result == SteamResult.OK){
            currentLobby = steamID;

            smat.setLobbyData(steamID, "name", player.name);
            smat.setLobbyData(steamID, "mapname", world.getMap() == null ? "Unknown" : state.rules.zone == null ? world.getMap().name() : state.rules.zone.localizedName);
            smat.setLobbyData(steamID, "version", Version.build + "");
            smat.setLobbyData(steamID, "versionType", Version.type);
            smat.setLobbyData(steamID, "wave", state.wave + "");
            smat.setLobbyData(steamID, "gamemode", Gamemode.bestFit(state.rules).name() + "");
        }
    }

    public void showFriendInvites(){
        if(currentLobby != null){
            friends.activateGameOverlayInviteDialog(currentLobby);
            Log.info("Activating overlay dialog");
        }
    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult steamResult){

    }

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError){
        if(net.server()){
            Log.info("{0} has disconnected: {1}", steamIDRemote.getAccountID(), sessionError);
            disconnectSteamUser(steamIDRemote);
        }else if(steamIDRemote.equals(currentServer)){
            Log.info("Disconnected! {1}: {0}", steamIDRemote.getAccountID(), sessionError);
            net.handleClientReceived(new Disconnect());
        }
    }

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote){
        Log.info("Connection request: {0}", steamIDRemote.getAccountID());
        if(net.server()){
            Log.info("Am server, accepting request from " + steamIDRemote.getAccountID());
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
    public void onGameLobbyJoinRequested(SteamID lobby, SteamID steamIDFriend){
        Log.info("onGameLobbyJoinRequested {0} {1}", lobby, steamIDFriend);
        smat.joinLobby(lobby);
    }

    @Override
    public void onAvatarImageLoaded(SteamID steamID, int i, int i1, int i2){

    }

    @Override
    public void onFriendRichPresenceUpdate(SteamID steamID, int i){

    }

    @Override
    public void onGameRichPresenceJoinRequested(SteamID steamID, String connect){
        Log.info("onGameRichPresenceJoinRequested {0} {1}", steamID, connect);
    }

    @Override
    public void onGameServerChangeRequested(String server, String password){

    }

    public class SteamConnection extends NetConnection{
        final SteamID sid;
        final P2PSessionState state = new P2PSessionState();

        public SteamConnection(SteamID sid){
            super(sid.getAccountID() + "");
            this.sid = sid;
            Log.info("Create STEAM client {0}", sid.getAccountID());
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
            return true;//state.isConnectionActive();
        }

        @Override
        public void close(){
            disconnectSteamUser(sid);
        }
    }
}
