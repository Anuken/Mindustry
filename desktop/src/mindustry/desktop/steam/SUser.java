package mindustry.desktop.steam;

import com.codedisaster.steamworks.*;

public class SUser implements SteamUserCallback{
    public final SteamUser user = new SteamUser(this);
}
