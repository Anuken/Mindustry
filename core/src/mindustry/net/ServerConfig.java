package mindustry.net;

import mindustry.*;
import mindustry.game.*;
import mindustry.maps.Maps.*;

import java.lang.annotation.*;

import static mindustry.Vars.*;

/** Server configuration values. Loaded/saved from config.hjson by ServerControl; this class just holds defaults. */
public class ServerConfig{
    @Desc("The server name as displayed on clients.")
    public String name = "Server";

    @Desc("The server description, displayed under the name. Max 100 characters.")
    public String desc = "off";

    @Desc("The port to host on.")
    public int port = Vars.port;

    @Desc("Whether to auto-update and exit when a new bleeding-edge update arrives.")
    public boolean autoUpdate = false;

    @Desc("Whether to display connect/disconnect messages.")
    public boolean showConnectMessages = true;

    @Desc("Whether votekick is enabled.")
    public boolean enableVotekick = true;

    @Desc("Commands run at startup. This should be a comma-separated list.")
    public String startCommands = "";

    @Desc("Whether to log everything to files.")
    public boolean logging = true;

    @Desc("Whether strict mode is on - corrects positions and prevents duplicate UUIDs.")
    public boolean strict = true;

    @Desc("Whether spammers are automatically kicked and rate-limited.")
    public boolean antiSpam = headless;

    @Desc("Block interaction rate limit window, in seconds.")
    public int interactRateWindow = 6;

    @Desc("Block interaction rate limit.")
    public int interactRateLimit = 25;

    @Desc("How many times a player must interact inside the window to get kicked.")
    public int interactRateKick = 60;

    @Desc("Message rate limit in seconds. 0 to disable.")
    public int messageRateLimit = 0;

    @Desc("How many times a player must send a message before the cooldown to get kicked. 0 to disable.")
    public int messageSpamKick = 3;

    @Desc("Limit for packet count sent within 3sec that will lead to a blacklist + kick.")
    public int packetSpamLimit = 300;

    @Desc("Limit for how many UUID changes an IP can send in the time frame specified by uuidChangeTimePeriod before it gets banned.")
    public int uuidChangeLimit = 10;

    @Desc("Time window for the uuidChangeLimit config, in hours.")
    public int uuidChangeTimePeriod = 3;

    @Desc("Limit for chat packet count sent within 2sec that will lead to a blacklist + kick. Not the same as a rate limit.")
    public int chatSpamLimit = 20;

    @Desc("Allows a local application to control this server through a local TCP socket.")
    public boolean socketInput = false;

    @Desc("The port for socket input.")
    public int socketInputPort = 6859;

    @Desc("The bind address for socket input.")
    public String socketInputAddress = "localhost";

    @Desc("Whether custom clients are allowed to connect.")
    public boolean allowCustomClients = !headless;

    @Desc("Whether the whitelist is used.")
    public boolean whitelist = false;

    @Desc("The message displayed to people on connection.")
    public String motd = "off";

    @Desc("Whether the periodically save the map when playing.")
    public boolean autosave = false;

    @Desc("The maximum amount of autosaves. Older ones get replaced.")
    public int autosaveAmount = 10;

    @Desc("Spacing between autosaves in seconds.")
    public int autosaveSpacing = 60 * 5;

    @Desc("Enable debug logging.")
    public boolean debug = false;

    @Desc("Client entity snapshot interval in ms.")
    public int snapshotInterval = 200;

    @Desc("Whether the game should pause when nobody is online.")
    public boolean autoPause = false;

    @Desc("Time before loading a new map after the gameover, in seconds.")
    public int roundExtraTime = 12;

    @Desc("The Maximum log file size, in bytes.")
    public int maxLogLength = 1024 * 1024 * 5;

    @Desc("Whether player commands should be logged.")
    public boolean logCommands = true;

    @Desc("The mode for shuffling maps when the game ends.")
    public ShuffleMode shuffleMode = ShuffleMode.custom;

    @Desc("Gamemode to use when game-over triggers and a new map is loaded. This is overwritten when a new map is hosted via the host command.")
    public Gamemode lastGamemode = Gamemode.survival;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Desc{
        String value();
    }
}
