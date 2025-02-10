package mindustry.net;

import arc.util.*;
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
    public final @Nullable String modeName;
    public int ping, port;

    public Host(int ping, String name, String address, int port, String mapname, int wave, int players, int version, String versionType, Gamemode mode, int playerLimit, String description, String modeName){
        this.ping = ping;
        this.name = name;
        this.address = address;
        this.port = port;
        this.mapname = mapname;
        this.wave = wave;
        this.players = players;
        this.version = version;
        this.versionType = versionType;
        this.mode = mode;
        this.playerLimit = playerLimit;
        this.description = description;
        this.modeName = modeName;
    }

    public Host(int ping, String name, String address, String mapname, int wave, int players, int version, String versionType, Gamemode mode, int playerLimit, String description, String modeName){
        this(ping, name, address, Vars.port, mapname, wave, players, version, versionType, mode, playerLimit, description, modeName);
    }
}
