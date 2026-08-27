package mindustry.desktop.steam;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.net.ArcNetProvider.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.net.Packets.*;
import steamworks.*;
import steamworks.SteamMatchmaking.*;
import steamworks.SteamNetworkingSockets.*;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

public class SNet implements SteamNetworkingSocketsCallback, SteamMatchmakingCallback, SteamFriendsCallback, NetProvider{
    public final SteamNetworkingSockets snet = new SteamNetworkingSockets(this);
    public final SteamMatchmaking smat = new SteamMatchmaking(this);
    public final SteamFriends friends = new SteamFriends(this);

    final NetProvider provider;

    final PacketSerializer serializer = new PacketSerializer();
    final ByteBuffer clientWriteBuffer = ByteBuffer.allocateDirect(16384);

    final ByteBuffer netReadBuffer = ByteBuffer.allocateDirect(16384);
    final ByteBuffer netReadBufferCopy = ByteBuffer.allocate(netReadBuffer.capacity());
    Thread readThread;
    volatile boolean running;

    final CopyOnWriteArrayList<SteamConnection> connections = new CopyOnWriteArrayList<>();
    final IntMap<SteamConnection> steamConnections = new IntMap<>(); //maps steam ID -> valid net connection

    Socket listenSocket;
    volatile Connection clientConnection; //our connection to the server, when we are the client

    SteamID currentLobby, currentServer;
    Cons<Host> lobbyCallback;
    Runnable lobbyDoneCallback, joinCallback;

    public SNet(NetProvider provider){
        this.provider = provider;

        Events.on(WaveEvent.class, e -> updateWave());
        Events.run(Trigger.newGame, this::updateWave);

        Events.on(PlayerIpBanEvent.class, e -> updateBans(e.ip));
        Events.on(PlayerUnbanEvent.class, e -> {
            // updateBans works off of ip ban list. Unbanning a player does not unban their ip but since this is steam, their "ip" is just their steam id (which is their uuid as well) prefixed with steam:
            netServer.admins.unbanPlayerIP("steam:" + e.uuid);
            updateBans(null);
        });
    }

    public boolean isSteamClient(){
        return currentServer != null;
    }

    void stopNetThread(){
        if(readThread != null){
            running = false;
            readThread.interrupt();
            readThread = null;
        }
    }

    void startNetThread(){
        stopNetThread();

        running = true;
        readThread = new Thread(this::readLoop, "steam-net-read");
        readThread.setDaemon(true);
        readThread.start();
    }

    void readLoop(){
        while(running){
            boolean readAny = false;

            try{
                if(net.server()){
                    for(SteamConnection con : connections){
                        readAny |= pollConnection(con.connection, con);
                    }

                }else{
                    Connection cc = clientConnection;
                    if(cc != null) readAny |= pollConnection(cc, null);
                }
            }catch(Exception e){
                Core.app.post(() -> {
                    if(net.server()){
                        Log.err(e);
                    }else{
                        net.showError(e);
                    }
                });
            }

            if(!readAny){
                try{
                    Thread.sleep(15);
                }catch(InterruptedException ignored){
                    return; //thread stopped
                }
            }
        }
    }

    /**
     * Drains all currently-pending messages on one connection.
     * @return true if anything was read.
     * */
    boolean pollConnection(Connection connection, SteamConnection con) throws Exception{
        boolean readAny = false;
        int length;

        while((length = snet.receiveMessageOnConnection(connection, netReadBuffer.clear())) != 0){
            readAny = true;

            //lz4 chokes on direct buffers
            netReadBufferCopy.position(0).limit(length);
            netReadBufferCopy.put(0, netReadBuffer, 0, length);

            Object output = serializer.read(netReadBufferCopy);
            if(!(output instanceof Packet pack)) continue;

            Core.app.post(() -> {
                if(net.server()){
                    try{
                        net.handleServerReceived(con, pack);
                    }catch(Throwable t){
                        Log.err(t);
                    }
                }else{
                    try{
                        net.handleClientReceived(pack);
                    }catch(Throwable t){
                        net.handleException(t);
                    }
                }
            });
        }

        if(con != null){
            con.pollWrites();
        }

        return readAny;
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
        }else if (ip.startsWith("steamserver:")){
            String server = ip.substring("steamserver:".length());
            try{
                SteamID serverID = SteamID.createFromNativeHandle(Long.parseLong(server));
                if(!serverID.isValid()) throw new IOException("Invalid Steam ID structure: " + server);

                Core.app.post(() -> {
                    currentLobby = null;
                    currentServer = serverID;
                    joinCallback = success;

                    //begin the handshake; success/handleClientReceived/setClientConnected fire once onConnectionStatusChanged reports Connected
                    clientConnection = snet.connectP2P(serverID, 0);

                    Core.app.post(() -> {  // TODO: This gets hidden and I can't figure out how to not do so.
                        ui.loadfrag.show("@connecting");
                        ui.loadfrag.setButton(() -> {
                            ui.loadfrag.hide();
                            netClient.disconnectQuietly();
                        });
                    });

                    Log.info("Initiated direct Steam P2P connection to server: @", currentServer.getAccountID());
                });
            }catch(NumberFormatException e){
                throw new IOException("Failed to parse server Steam ID: " + server);
            }
        }else{
            provider.connectClient(ip, port, success);
        }
    }

    @Override
    public void sendClient(Object object, boolean reliable){
        if(isSteamClient()){
            if(currentServer == null || clientConnection == null){
                Log.info("Not connected, quitting.");
                return;
            }

            try{
                clientWriteBuffer.limit(clientWriteBuffer.capacity());
                clientWriteBuffer.position(0);
                serializer.write(clientWriteBuffer, object);
                int length = clientWriteBuffer.position();
                clientWriteBuffer.flip();

                var result = snet.sendMessageToConnection(clientConnection, clientWriteBuffer, reliable || length >= 1000 ? SendFlags.ReliableNoNagle : SendFlags.UnreliableNoDelay);

                if(result == SteamResult.InvalidParam || result == SteamResult.NoConnection || result == SteamResult.InvalidState){
                    throw new IOException("Failed to send packet: " + result);
                }
            }catch(Exception e){
                net.showError(e);
            }
        }else{
            provider.sendClient(object, reliable);
        }
    }

    @Override
    public void disconnectClient(){
        stopNetThread();

        if(isSteamClient()){
            if(currentLobby != null) smat.leaveLobby(currentLobby);
            if(clientConnection != null) snet.closeConnection(clientConnection, 0, false);
            clientConnection = null;
            currentServer = null;
            currentLobby = null;
            net.handleClientReceived(new Disconnect());
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
        listenSocket = snet.createListenSocketP2P(0);
        smat.createLobby(Core.settings.getBool("steampublichost2") ? LobbyType.Public : LobbyType.FriendsOnly, Core.settings.getInt("playerlimit"));
        startNetThread();
    }

    public void updateLobby(){
        if(currentLobby != null && net.server()){
            smat.setLobbyType(currentLobby, Core.settings.getBool("steampublichost2") ? LobbyType.Public : LobbyType.FriendsOnly);
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

    /** Updates the ban list so that lobbies don't appear for banned players. The list will only be updated when a steam player is banned/unbanned. */
    void updateBans(String changed){
        if(changed != null && !changed.startsWith("steam:")) return; //hacky way to ignore non-steam ids
        smat.setLobbyData(currentLobby, "banned", netServer.admins.bannedIPs.select(ip -> ip.contains("steam:")).reduce(new StringBuilder(), (ip, str) -> str.append(ip.substring(6)).append(',')).toString()); //list of handles split by commas
    }

    @Override
    public void closeServer(){
        provider.closeServer();
        stopNetThread();

        if(currentLobby != null){
            smat.leaveLobby(currentLobby);
            for(SteamConnection con : steamConnections.values()){
                con.close();
            }
            currentLobby = null;
        }

        if(listenSocket != null){
            snet.closeListenSocket(listenSocket);
            listenSocket = null;
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

    /** Closes out and unregisters a connected steam user, if one is registered under this ID. */
    void disconnectSteamUser(SteamID steamid){
        int sid = steamid.getAccountID();
        SteamConnection con = steamConnections.get(sid);

        if(con != null){
            snet.closeConnection(con.connection, 0, false);
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
        boolean hidden = smat.getLobbyData(steamIDLobby, "hidden").equals("true");

        //check version
        if(version != Version.build && !hidden){
            ui.loadfrag.hide();
            ui.showInfo("[scarlet]" + (version > Version.build ? KickReason.clientOutdated : KickReason.serverOutdated) + "\n[]" +
            Core.bundle.format("server.versions", Version.build, version));
            smat.leaveLobby(steamIDLobby);
            return;
        }

        if(clientConnection != null){
            Log.info("onLobbyEnter fired with an existing clientConnection @, closing it before reconnecting.", clientConnection);
            snet.closeConnection(clientConnection, 0, false);
            clientConnection = null;
        }

        ui.editor.hide();

        //delay joining by one frame because the editor bugs out if you don't
        Core.app.post(() -> {
            logic.reset();
            net.reset();

            currentLobby = steamIDLobby;
            currentServer = smat.getLobbyOwner(steamIDLobby);

            Log.info("Connecting to owner @: @", currentServer.getAccountID(), friends.getFriendPersonaName(currentServer));

           clientConnection = snet.connectP2P(currentServer, 0);
        });
    }

    @Override
    public void onLobbyChatUpdate(SteamID lobby, SteamID who, SteamID changer, ChatMemberStateChange change){
        Log.info("lobby @: @ caused @'s change: @", lobby.getAccountID(), changer.getAccountID(), who.getAccountID(), change);
        if(net.server() && change == ChatMemberStateChange.Entered && SteamAdmin.isAdmin("steam:" + who.getAccountID())) SteamAdmin.fetch(true); //fetch on admin join
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

                    String banList = smat.getLobbyData(lobby, "banned");

                    boolean banned = banList.length() > 0 && Structs.contains(banList.split(","), SVars.user.user.getSteamID().getAccountID() + "");

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
                        banned ? "[banned]" : "",
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
            updateBans(null);
        }
    }

    public void showFriendInvites(){
        if(currentLobby != null){
            friends.activateGameOverlayInviteDialog(currentLobby);
            Log.info("Activating overlay dialog");
        }
    }

    @Override
    public void onConnectionStatusChanged(Connection connection, SteamID remote, ConnectionState state, ConnectionState prevState){
        Log.info("Connection @ (steam @) changed: @ -> @", connection, remote.getAccountID(), prevState, state);

        try{
            if(net.server()){
                if(state == ConnectionState.Connecting){
                    //256kb -> 1mb/sec for large worlds
                    int limit = 1 * 1024 * 1024;
                    snet.setConnectionConfigValue(connection, SteamNetworkingConfigValue.SendRateMax, limit);
                    snet.setConnectionConfigValue(connection, SteamNetworkingConfigValue.SendRateMin, limit);

                    //incoming connection request arriving through our listen socket; accept it
                    SteamResult result = snet.acceptConnection(connection);
                    if(result != SteamResult.OK){
                        Log.err("Failed to accept incoming Steam connection: @", result);
                        snet.closeConnection(connection, 0, false);
                    }
                }else if(state == ConnectionState.Connected && prevState != ConnectionState.Connected){
                    int fromID = remote.getAccountID();

                    SteamConnection existing = steamConnections.get(fromID);
                    if(existing != null){
                        //close out stale connections
                        Log.info("Duplicate connection from @ (old=@ new=@), closing old.", fromID, existing.connection, connection);
                        snet.closeConnection(existing.connection, 0, false);
                        steamConnections.remove(fromID);
                        connections.remove(existing);
                    }

                    SteamConnection con = new SteamConnection(remote, connection);
                    Connect c = new Connect();
                    c.addressTCP = "steam:" + fromID;

                    Log.info("&bReceived STEAM connection: @", c.addressTCP);

                    steamConnections.put(fromID, con);
                    connections.add(con);
                    net.handleServerReceived(con, c);
                }else if(state == ConnectionState.ClosedByPeer || state == ConnectionState.ProblemDetectedLocally){
                    Log.info("@ has disconnected: @", remote.getAccountID(), state);
                    disconnectSteamUser(remote);
                }
            }else if(currentServer != null && remote.getAccountID() == currentServer.getAccountID()){
                if(state == ConnectionState.Connected && prevState != ConnectionState.Connected){
                    startNetThread();

                    if(joinCallback != null){
                        joinCallback.run();
                        joinCallback = null;
                    }

                    Connect con = new Connect();
                    con.addressTCP = "steam:" + currentServer.getAccountID();

                    net.setClientConnected();
                    net.handleClientReceived(con);
                }else if(state == ConnectionState.ClosedByPeer || state == ConnectionState.ProblemDetectedLocally){
                    Log.info("Disconnected! @: @", remote.getAccountID(), state);

                    Core.app.post(() -> {
                        ui.loadfrag.hide();
                        ui.showErrorMessage(Core.bundle.format("disconnect.reason", state.name()));
                        net.handleClientReceived(new Disconnect());
                        currentServer = null;
                        clientConnection = null;
                    });
                }
            }
        }catch(Exception e){
            Log.err("Error processing connection status change", e);
        }
    }

    @Override
    public void onGameLobbyJoinRequested(SteamID lobby, SteamID steamIDFriend){
        Log.info("onGameLobbyJoinRequested @ @", lobby, steamIDFriend);
        smat.joinLobby(lobby);

        //prevents awkward pause when joining
        ui.loadfrag.show("@connecting");
        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
        });
    }

    public class SteamConnection extends NetConnection{
        final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(16384);
        final SteamID sid;
        final Connection connection;

        //outgoing queue of not-yet-accepted messages, in send order
        private final ArrayDeque<QueuedMessage> outgoing = new ArrayDeque<>();
        private static final int maxQueuedBytes = 50 * 1024 * 1024; //50mb (for large data asset maps)
        private int queuedBytes = 0;

        public SteamConnection(SteamID sid, Connection connection){
            super("steam:" + sid.getAccountID());
            this.sid = sid;
            this.connection = connection;
            Log.info("Created Steam connection: @", sid.getAccountID());
        }

        /** Called in an external thread at around 60fps per client connection. */
        public void pollWrites(){
            synchronized(outgoing){
                QueuedMessage msg;
                while((msg = outgoing.peek()) != null){
                    SteamResult result;
                    try{
                        result = snet.sendMessageToConnection(connection, msg.buffer, msg.flags);
                    }catch(Exception e){
                        handleError(e);
                        return;
                    }

                    if(result == SteamResult.OK){
                        outgoing.poll();
                        queuedBytes -= msg.buffer.capacity();
                    }else if(result == SteamResult.LimitExceeded){
                        //Steam's send buffer is full; stop draining and retry the same message (preserving order) next poll instead of silently losing it
                        break;
                    }else if(result == SteamResult.Ignored){
                        //only possible with NoDelay-flagged unreliable sends; fine to drop and move on
                        outgoing.poll();
                        queuedBytes -= msg.buffer.capacity();
                    }else{
                        //InvalidParam / InvalidState / NoConnection: connection is dead, nothing to retry
                        outgoing.clear();
                        queuedBytes = 0;
                        handleError(new IOException("Failed to send packet: " + result));
                        return;
                    }
                }
            }
        }

        /** Can be called on any thread. Serializes immediately, but only queues the send. Actual transmission and backpressure handling happens in {@link #pollWrites()}. */
        @Override
        public void send(Object object, boolean reliable){
            try{
                ByteBuffer buffer;
                int flags;

                synchronized(writeBuffer){
                    writeBuffer.limit(writeBuffer.capacity());
                    writeBuffer.position(0);
                    serializer.write(writeBuffer, object);
                    int length = writeBuffer.position();
                    writeBuffer.flip();

                    flags = reliable || length >= 1000 ? SendFlags.ReliableNoNagle : SendFlags.UnreliableNoDelay;

                    buffer = ByteBuffer.allocateDirect(length);
                    buffer.put(writeBuffer);
                    buffer.flip();
                }

                synchronized(outgoing){
                    if(queuedBytes + buffer.capacity() > maxQueuedBytes){
                        throw new IOException("Send queue overflow (" + queuedBytes + " bytes); disconnecting client");
                    }

                    outgoing.add(new QueuedMessage(buffer, flags));
                    queuedBytes += buffer.capacity();
                }
            }catch(Exception e){
                handleError(e);
            }
        }

        @Override
        public boolean isConnected(){
            return connection.isValid();
        }

        @Override
        protected void kickDisconnect(){
            //delay the close so the kick packet can be sent on steam
            Time.runTask(10f, this::close);
        }

        @Override
        public void close(){
            disconnectSteamUser(sid);
        }

        private void handleError(Exception e){
            //handle errors on the main thread
            Core.app.post(() -> {
                Log.err("Error sending packet. Disconnecting invalid client!", e);
                close();

                SteamConnection k = steamConnections.get(sid.getAccountID());
                if(k != null) steamConnections.remove(sid.getAccountID());
            });
        }

        private static final class QueuedMessage{
            final ByteBuffer buffer;
            final int flags;

            QueuedMessage(ByteBuffer buffer, int flags){
                this.buffer = buffer;
                this.flags = flags;
            }
        }
    }
}