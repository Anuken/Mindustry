package mindustry.desktop.steam;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamMatchmaking.*;
import com.codedisaster.steamworks.SteamNetworking.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.net.ArcNetProvider.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.net.Packets.*;

import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

public class SNet implements SteamNetworkingCallback, SteamMatchmakingCallback, SteamFriendsCallback, NetProvider{
    public final SteamNetworking snet = new SteamNetworking(this);
    public final SteamMatchmaking smat = new SteamMatchmaking(this);
    public final SteamFriends friends = new SteamFriends(this);

    final NetProvider provider;

    final PacketSerializer serializer = new PacketSerializer();
    final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(16384);
    final ByteBuffer readBuffer = ByteBuffer.allocateDirect(16384);
    final ByteBuffer readCopyBuffer = ByteBuffer.allocate(writeBuffer.capacity());

    final CopyOnWriteArrayList<SteamConnection> connections = new CopyOnWriteArrayList<>();
    final IntMap<SteamConnection> steamConnections = new IntMap<>(); //maps steam ID -> valid net connection

    SteamID currentLobby, currentServer;
    Cons<Host> lobbyCallback;
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
                        readBuffer.position(0).limit(readBuffer.capacity());
                        //lz4 chokes on direct buffers, so copy the bytes over
                        int len = snet.readP2PPacket(from, readBuffer, 0);
                        readBuffer.limit(len);
                        readCopyBuffer.position(0);
                        readCopyBuffer.put(readBuffer);
                        readCopyBuffer.position(0);
                        int fromID = from.getAccountID();
                        Object output = serializer.read(readCopyBuffer);

                        //it may be theoretically possible for this to be a framework message, if the packet is malicious or corrupted
                        if(!(output instanceof Packet)) return;

                        Packet pack = (Packet)output;

                        if(net.server()){
                            SteamConnection con = steamConnections.get(fromID);
                            try{
                                //accept users on request
                                if(con == null){
                                    con = new SteamConnection(SteamID.createFromNativeHandle(from.handle()));
                                    Connect c = new Connect();
                                    c.addressTCP = "steam:" + from.getAccountID();

                                    Log.info("&bReceived STEAM connection: @", c.addressTCP);

                                    steamConnections.put(from.getAccountID(), con);
                                    connections.add(con);
                                    net.handleServerReceived(con, c);
                                }

                                net.handleServerReceived(con, pack);
                            }catch(Throwable e){
                                Log.err(e);
                            }
                        }else if(currentServer != null && fromID == currentServer.getAccountID()){
                            try{
                                net.handleClientReceived(pack);
                            }catch(Throwable t){
                                net.handleException(t);
                            }
                        }
                    }catch(Exception e){
                        if(net.server()){
                            Log.err(e);
                        }else{
                            net.showError(e);
                        }
                    }
                }
            }
        }));

        Events.on(WaveEvent.class, e -> updateWave());
        Events.run(Trigger.newGame, this::updateWave);
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
    public void sendClient(Object object, boolean reliable){
        if(isSteamClient()){
            if(currentServer == null){
                Log.info("Not connected, quitting.");
                return;
            }

            try{
                writeBuffer.limit(writeBuffer.capacity());
                writeBuffer.position(0);
                serializer.write(writeBuffer, object);
                int length = writeBuffer.position();
                writeBuffer.flip();

                snet.sendP2PPacket(currentServer, writeBuffer, reliable || length >= 1000 ? P2PSend.Reliable : P2PSend.UnreliableNoDelay, 0);
            }catch(Exception e){
                net.showError(e);
            }
        }else{
            provider.sendClient(object, reliable);
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
    public void discoverServers(Cons<Host> callback, Runnable done){
        smat.addRequestLobbyListResultCountFilter(32);
        smat.addRequestLobbyListDistanceFilter(LobbyDistanceFilter.Worldwide);
        smat.requestLobbyList();
        lobbyCallback = callback;

        //after the steam lobby is done discovering, look for local network servers.
        lobbyDoneCallback = () -> provider.discoverServers(callback, done);
    }

    @Override
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed){
        provider.pingHost(address, port, valid, failed);
    }

    @Override
    public void hostServer(int port) throws IOException{
        provider.hostServer(port);
        smat.createLobby(Core.settings.getBool("steampublichost") ? LobbyType.Public : LobbyType.FriendsOnly, Core.settings.getInt("playerlimit"));

        Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> Log.info("Server: @\nClient: @\nActive: @", net.server(), net.client(), net.active()))));
    }

    public void updateLobby(){
        if(currentLobby != null && net.server()){
            smat.setLobbyType(currentLobby, Core.settings.getBool("steampublichost") ? LobbyType.Public : LobbyType.FriendsOnly);
            smat.setLobbyMemberLimit(currentLobby, Core.settings.getInt("playerlimit"));
        }
    }
    
    void updateWave(){
        if(currentLobby != null && net.server()){
            smat.setLobbyData(currentLobby, "mapname", state.map.name());
            smat.setLobbyData(currentLobby, "wave", state.wave + "");
            smat.setLobbyData(currentLobby, "gamemode", state.rules.mode().name() + "");
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
    public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID){
        Log.info("onLobbyInvite @ @ @", steamIDLobby.getAccountID(), steamIDUser.getAccountID(), gameID);
    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, ChatRoomEnterResponse response){
        Log.info("onLobbyEnter @ @", steamIDLobby.getAccountID(), response);

        if(response != ChatRoomEnterResponse.Success){
            ui.loadfrag.hide();
            ui.showErrorMessage(Core.bundle.format("cantconnect", response.toString()));
            return;
        }

        int version = Strings.parseInt(smat.getLobbyData(steamIDLobby, "version"), -1);

        //check version
        if(version != Version.build){
            ui.loadfrag.hide();
            ui.showInfo("[scarlet]" + (version > Version.build ? KickReason.clientOutdated : KickReason.serverOutdated).toString() + "\n[]" +
                Core.bundle.format("server.versions", Version.build, version));
            smat.leaveLobby(steamIDLobby);
            return;
        }

        logic.reset();
        net.reset();

        currentLobby = steamIDLobby;
        currentServer = smat.getLobbyOwner(steamIDLobby);

        Log.info("Connect to owner @: @", currentServer.getAccountID(), friends.getFriendPersonaName(currentServer));

        if(joinCallback != null){
            joinCallback.run();
            joinCallback = null;
        }

        Connect con = new Connect();
        con.addressTCP = "steam:" + currentServer.getAccountID();

        net.setClientConnected();
        net.handleClientReceived(con);

        Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> Log.info("Server: @\nClient: @\nActive: @", net.server(), net.client(), net.active()))));
    }

    @Override
    public void onLobbyChatUpdate(SteamID lobby, SteamID who, SteamID changer, ChatMemberStateChange change){
        Log.info("lobby @: @ caused @'s change: @", lobby.getAccountID(), who.getAccountID(), changer.getAccountID(), change);
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
    public void onLobbyMatchList(int matches){
        Log.info("found @ matches", matches);

        if(lobbyDoneCallback != null){
            Seq<Host> hosts = new Seq<>();
            for(int i = 0; i < matches; i++){
                try{
                    SteamID lobby = smat.getLobbyByIndex(i);
                    if(smat.getLobbyData(lobby, "hidden").equals("true")) continue;
                    String mode = smat.getLobbyData(lobby, "gamemode");
                    //make sure versions are equal, don't list incompatible lobbies
                    if(mode == null || mode.isEmpty() || (Version.build != -1 && Strings.parseInt(smat.getLobbyData(lobby, "version"), -1) != Version.build)) continue;
                    Host out = new Host(
                        -1, //invalid ping
                        smat.getLobbyData(lobby, "name"),
                        "steam:" + lobby.handle(),
                        smat.getLobbyData(lobby, "mapname"),
                        Strings.parseInt(smat.getLobbyData(lobby, "wave"), -1),
                        smat.getNumLobbyMembers(lobby),
                        Strings.parseInt(smat.getLobbyData(lobby, "version"), -1),
                        smat.getLobbyData(lobby, "versionType"),
                        Gamemode.valueOf(mode),
                        smat.getLobbyMemberLimit(lobby),
                        "",
                        null
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
    public void onLobbyCreated(SteamResult result, SteamID steamID){
        if(!net.server()){
            Log.info("Lobby created on server: @, ignoring.", steamID);
            return;
        }

        Log.info("Lobby @ created? @", result, steamID.getAccountID());
        if(result == SteamResult.OK){
            currentLobby = steamID;

            smat.setLobbyData(steamID, "name", player.name);
            smat.setLobbyData(steamID, "mapname", state.map.name());
            smat.setLobbyData(steamID, "version", Version.build + "");
            smat.setLobbyData(steamID, "versionType", Version.type);
            smat.setLobbyData(steamID, "wave", state.wave + "");
            smat.setLobbyData(steamID, "gamemode", state.rules.mode().name() + "");
        }
    }

    public void showFriendInvites(){
        if(currentLobby != null){
            friends.activateGameOverlayInviteDialog(currentLobby);
            Log.info("Activating overlay dialog");
        }
    }

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError){
        if(net.server()){
            Log.info("@ has disconnected: @", steamIDRemote.getAccountID(), sessionError);
            disconnectSteamUser(steamIDRemote);
        }else if(steamIDRemote.equals(currentServer)){
            Log.info("Disconnected! @: @", steamIDRemote.getAccountID(), sessionError);
            net.handleClientReceived(new Disconnect());
        }
    }

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote){
        Log.info("Connection request: @", steamIDRemote.getAccountID());
        if(net.server()){
            Log.info("Am server, accepting request from " + steamIDRemote.getAccountID());
            snet.acceptP2PSessionWithUser(steamIDRemote);
        }
    }

    @Override
    public void onGameLobbyJoinRequested(SteamID lobby, SteamID steamIDFriend){
        Log.info("onGameLobbyJoinRequested @ @", lobby, steamIDFriend);
        smat.joinLobby(lobby);
    }

    public class SteamConnection extends NetConnection{
        final SteamID sid;

        public SteamConnection(SteamID sid){
            super(sid.getAccountID() + "");
            this.sid = sid;
            Log.info("Created STEAM connection: @", sid.getAccountID());
        }

        @Override
        public void send(Object object, boolean reliable){
            try{
                writeBuffer.limit(writeBuffer.capacity());
                writeBuffer.position(0);
                serializer.write(writeBuffer, object);
                int length = writeBuffer.position();
                writeBuffer.flip();

                snet.sendP2PPacket(sid, writeBuffer, reliable || length >= 1000 ? object instanceof StreamChunk ? P2PSend.ReliableWithBuffering : P2PSend.Reliable : P2PSend.UnreliableNoDelay, 0);
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
            //TODO ???
            //snet.getP2PSessionState(sid, state);
            return true;//state.isConnectionActive();
        }

        @Override
        public void close(){
            disconnectSteamUser(sid);
        }
    }
}
