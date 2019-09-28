package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamAuth.*;

public class SUser implements SteamUserCallback{
    public final SteamUser user = new SteamUser(this);

    @Override
    public void onValidateAuthTicket(SteamID steamID, AuthSessionResponse authSessionResponse, SteamID ownerSteamID){

    }

    @Override
    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized){

    }

    @Override
    public void onEncryptedAppTicket(SteamResult result){

    }
}
