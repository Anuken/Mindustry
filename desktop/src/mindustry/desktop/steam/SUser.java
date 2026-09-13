package mindustry.desktop.steam;

import steamworks.*;

public class SUser implements SteamUserCallback{
    public final SteamUser user = new SteamUser(this);
}
