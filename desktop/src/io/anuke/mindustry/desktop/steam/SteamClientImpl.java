package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamFriends.*;
import com.codedisaster.steamworks.SteamMatchmaking.*;
import com.codedisaster.steamworks.SteamNetworking.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.desktop.steam.SteamServerImpl.*;

import java.nio.*;

public class SteamClientImpl implements SteamNetworkingCallback, SteamMatchmakingCallback{
    private SteamNetworking snet;
    private SteamMatchmaking smat;

    private ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 128);
    //maps steam ID -> valid net connection
    private IntMap<SteamConnection> connections = new IntMap<>();

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
            SteamFriends friends = new SteamFriends(new SteamFriendsCallback(){
                @Override
                public void onSetPersonaNameResponse(boolean success, boolean localSuccess, SteamResult result){

                }

                @Override
                public void onPersonaStateChange(SteamID steamID, PersonaChange change){

                }

                @Override
                public void onGameOverlayActivated(boolean active){

                }

                @Override
                public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend){
                    Log.info("Requested {0} to join lobby {1}", steamIDFriend.getAccountID(), steamIDLobby.getAccountID());
                }

                @Override
                public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height){

                }

                @Override
                public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID){

                }

                @Override
                public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect){

                }

                @Override
                public void onGameServerChangeRequested(String server, String password){

                }
            });

            //friends.activateGameOverlay(OverlayDialog.Friends);
            friends.activateGameOverlayInviteDialog(steamIDLobby);
        }
    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult result){

    }
}
