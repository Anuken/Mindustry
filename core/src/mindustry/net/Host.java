package mindustry.net;

import mindustry.*;
import mindustry.game.*;

public class Host{
    public final String name;
    public final String address;
    public final String mapname, description;
    public final int wave;
    public final int players, playerLimit;
    public final int version;
    public final String versionType;
    public final Gamemode mode;
    public int ping, port = Vars.port;

    public Host(String name, String address, String mapname, int wave, int players, int version, String versionType, Gamemode mode, int playerLimit, String description){
        this.name = name;
        this.address = address;
        this.players = players;
        this.mapname = mapname;
        this.wave = wave;
        this.version = version;
        this.versionType = versionType;
        this.playerLimit = playerLimit;
        this.mode = mode;
        this.description = description;
    }
}
