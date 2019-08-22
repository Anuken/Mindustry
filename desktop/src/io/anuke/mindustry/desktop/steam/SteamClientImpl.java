package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamMatchmaking.*;
import com.codedisaster.steamworks.SteamNetworking.*;
import io.anuke.arc.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.async.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;
import net.jpountz.lz4.*;

import java.io.*;
import java.nio.*;

import static io.anuke.mindustry.Vars.ui;

public class SteamClientImpl implements SteamNetworkingCallback, SteamMatchmakingCallback, ClientProvider{
    final SteamNetworking snet = new SteamNetworking(this);
    final SteamMatchmaking smat = new SteamMatchmaking(this);

    final PacketSerializer serializer = new PacketSerializer();
    final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024 * 4);
    final ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024 * 4);
    final LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();

    SteamID currentLobby, currentServer;

    public SteamClientImpl(){
        //snet = new SteamNetworking(this);
        //smat = new SteamMatchmaking(this);

        //Log.info("Calling createLobby");
        //SteamAPICall call = smat.createLobby(LobbyType.FriendsOnly, 16);

        /*
        new Thread(() -> {
            int length;
            SteamID from = new SteamID();
            while((length = snet.isP2PPacketAvailable(0)) != 0){
                try{
                    buffer.position(0);
                    snet.readP2PPacket(from, buffer, 0);
                }catch(SteamException e){
                    e.printStackTrace();
                }
            }
        }){{
            setDaemon(true);
        }}.start();*/

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
                        //TODO make sure ID is host ID
                        Object output = serializer.read(readBuffer);

                        if(fromID == currentServer.getAccountID()){
                            Core.app.post(() -> Net.handleClientReceived(output));
                        }
                    }catch(SteamException e){
                        e.printStackTrace();
                    }
                }

                Threads.sleep(1000 / 10);
            }
        });
    }

    @Override
    public void connect(String ip, int port, Runnable success) throws IOException{
        //no
    }

    @Override
    public void send(Object object, SendMode mode){
        if(currentServer == null) return;

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

    }

    @Override
    public int getPing(){
        return 0;
    }

    @Override
    public void disconnect(){
        if(currentLobby != null){
            smat.leaveLobby(currentLobby);
            snet.closeP2PSessionWithUser(currentServer);
            currentServer = null;
            currentLobby = null;
        }
    }

    @Override
    public byte[] decompressSnapshot(byte[] input, int size){
        return decompressor.decompress(input, size);
    }

    @Override
    public void discover(Consumer<Host> callback, Runnable done){

    }

    @Override
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed){

    }

    @Override
    public void dispose(){
        disconnect();
    }

    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, P2PSessionError sessionError){
        Log.info("{0} has disconnected: {1}", steamIDRemote.getAccountID(), sessionError);
    }

    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote){
        snet.acceptP2PSessionWithUser(steamIDRemote);
    }

    @Override
    public void onFavoritesListChanged(int ip, int queryPort, int connPort, int appID, int flags, boolean add, int accountID){

    }

    @Override
    public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID){
        Log.info("lobby invite {0} {1} {2}", steamIDLobby.getAccountID(), steamIDUser.getAccountID(), gameID);
        ui.showConfirm("Someone has invited you to a game.", "But do you accept?", () -> {
            smat.joinLobby(steamIDLobby);
        });
    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, ChatRoomEnterResponse response){
        currentLobby = steamIDLobby;
        currentServer = smat.getLobbyOwner(steamIDLobby);

        Connect con = new Connect();
        con.addressTCP = "steam:" + currentServer.getAccountID();

        Net.handleClientReceived(con);
        Log.info("enter lobby {0} {1}", steamIDLobby.getAccountID(), response);
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
    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult result){

    }
}
