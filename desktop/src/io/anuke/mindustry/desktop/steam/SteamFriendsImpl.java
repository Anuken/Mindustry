package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamFriends.*;

public class SteamFriendsImpl implements SteamFriendsCallback{
    public final SteamFriends friends;

    public SteamFriendsImpl(){
        friends = new SteamFriends(this);
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
}
