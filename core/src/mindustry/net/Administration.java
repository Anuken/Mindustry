package mindustry.net;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.pooling.Pool.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.game.EventType.*;

public class Administration{
    public Seq<String> bannedIPs = new Seq<>();
    public Seq<String> whitelist = new Seq<>();
    public Seq<ChatFilter> chatFilters = new Seq<>();
    public Seq<ActionFilter> actionFilters = new Seq<>();
    public Seq<String> subnetBans = new Seq<>();
    public ObjectSet<String> dosBlacklist = new ObjectSet<>();
    public ObjectMap<String, Long> kickedIPs = new ObjectMap<>();

    /** All player info. Maps UUIDs to info. This persists throughout restarts. Do not access directly. */
    private ObjectMap<String, PlayerInfo> playerInfo = new ObjectMap<>();

    public Administration(){
        load();

        //anti-spam
        addChatFilter((player, message) -> {
            long resetTime = Config.messageRateLimit.num() * 1000L;
            if(Config.antiSpam.bool() && !player.isLocal() && !player.admin){
                //prevent people from spamming messages quickly
                if(resetTime > 0 && Time.timeSinceMillis(player.getInfo().lastMessageTime) < resetTime){
                    //supress message
                    player.sendMessage("[scarlet]You may only send messages every " + Config.messageRateLimit.num() + " seconds.");
                    player.getInfo().messageInfractions ++;
                    //kick player for spamming and prevent connection if they've done this several times
                    if(player.getInfo().messageInfractions >= Config.messageSpamKick.num() && Config.messageSpamKick.num() != 0){
                        player.con.kick("You have been kicked for spamming.", 1000 * 60 * 2);
                    }
                    return null;
                }else{
                    player.getInfo().messageInfractions = 0;
                }

                //prevent players from sending the same message twice in the span of 10 seconds
                if(message.equals(player.getInfo().lastSentMessage) && Time.timeSinceMillis(player.getInfo().lastMessageTime) < 1000 * 10){
                    player.sendMessage("[scarlet]You may not send the same message twice.");
                    return null;
                }

                player.getInfo().lastSentMessage = message;
                player.getInfo().lastMessageTime = Time.millis();
            }

            return message;
        });

        //block interaction rate limit
        addActionFilter(action -> {
            if(action.type != ActionType.breakBlock &&
                action.type != ActionType.placeBlock &&
                action.type != ActionType.commandUnits &&
                Config.antiSpam.bool()){

                Ratekeeper rate = action.player.getInfo().rate;
                if(rate.allow(Config.interactRateWindow.num() * 1000L, Config.interactRateLimit.num())){
                    return true;
                }else{
                    if(rate.occurences > Config.interactRateKick.num()){
                        action.player.kick("You are interacting with too many blocks.", 1000 * 30);
                    }else if(action.player.getInfo().messageTimer.get(60f * 2f)){
                        action.player.sendMessage("[scarlet]You are interacting with blocks too quickly.");
                    }

                    return false;
                }
            }
            return true;
        });
    }

    public synchronized void blacklistDos(String address){
        dosBlacklist.add(address);
    }

    public synchronized boolean isDosBlacklisted(String address){
        return dosBlacklist.contains(address);
    }

    /** @return time at which a player would be pardoned for a kick (0 means they were never kicked) */
    public long getKickTime(String uuid, String ip){
        return Math.max(getInfo(uuid).lastKicked, kickedIPs.get(ip, 0L));
    }

    /** Sets up kick duration for a player. */
    public void handleKicked(String uuid, String ip, long duration){
        kickedIPs.put(ip, Math.max(kickedIPs.get(ip, 0L), Time.millis() + duration));

        PlayerInfo info = getInfo(uuid);
        info.timesKicked++;
        info.lastKicked = Math.max(Time.millis() + duration, info.lastKicked);
    }

    public Seq<String> getSubnetBans(){
        return subnetBans;
    }

    public void removeSubnetBan(String ip){
        subnetBans.remove(ip);
        save();
    }

    public void addSubnetBan(String ip){
        subnetBans.add(ip);
        save();
    }

    public boolean isSubnetBanned(String ip){
        return subnetBans.contains(ip::startsWith);
    }

    /** Adds a chat filter. This will transform the chat messages of every player.
     * This functionality can be used to implement things like swear filters and special commands.
     * Note that commands (starting with /) are not filtered.*/
    public void addChatFilter(ChatFilter filter){
        chatFilters.add(filter);
    }

    /** Filters out a chat message. */
    public @Nullable String filterMessage(Player player, String message){
        String current = message;
        for(ChatFilter f : chatFilters){
            current = f.filter(player, current);
            if(current == null) return null;
        }
        return current;
    }

    /** Add a filter to actions, preventing things such as breaking or configuring blocks. */
    public void addActionFilter(ActionFilter filter){
        actionFilters.add(filter);
    }

    /** @return whether this action is allowed by the action filters. */
    public boolean allowAction(Player player, ActionType type, Tile tile, Cons<PlayerAction> setter){
        return allowAction(player, type, action -> setter.get(action.set(player, type, tile)));
    }

    /** @return whether this action is allowed by the action filters. */
    public boolean allowAction(Player player, ActionType type, Cons<PlayerAction> setter){
        //some actions are done by the server (null player) and thus are always allowed
        if(player == null) return true;

        PlayerAction act = Pools.obtain(PlayerAction.class, PlayerAction::new);
        act.player = player;
        act.type = type;
        setter.get(act);
        for(ActionFilter filter : actionFilters){
            if(!filter.allow(act)){
                Pools.free(act);
                return false;
            }
        }
        Pools.free(act);
        return true;
    }

    public int getPlayerLimit(){
        return Core.settings.getInt("playerlimit", headless ? 30 : 0);
    }

    public void setPlayerLimit(int limit){
        Core.settings.put("playerlimit", limit);
    }

    public boolean isStrict(){
        return Config.strict.bool();
    }

    public boolean allowsCustomClients(){
        return Config.allowCustomClients.bool();
    }

    /** Call when a player joins to update their information here. */
    public void updatePlayerJoined(String id, String ip, String name){
        PlayerInfo info = getCreateInfo(id);
        info.lastName = name;
        info.lastIP = ip;
        info.timesJoined++;
        if(!info.names.contains(name, false)) info.names.add(name);
        if(!info.ips.contains(ip, false)) info.ips.add(ip);
    }

    public boolean banPlayer(String uuid){
        return banPlayerID(uuid) || banPlayerIP(getInfo(uuid).lastIP);
    }

    /**
     * Bans a player by IP; returns whether this player was already banned.
     * If there are players who at any point had this IP, they will be UUID banned as well.
     */
    public boolean banPlayerIP(String ip){
        if(bannedIPs.contains(ip, false))
            return false;

        for(PlayerInfo info : playerInfo.values()){
            if(info.ips.contains(ip, false)){
                info.banned = true;
            }
        }

        bannedIPs.add(ip);
        save();
        Events.fire(new PlayerIpBanEvent(ip));
        return true;
    }

    /** Bans a player by UUID; returns whether this player was already banned. */
    public boolean banPlayerID(String id){
        if(playerInfo.containsKey(id) && playerInfo.get(id).banned)
            return false;

        getCreateInfo(id).banned = true;

        save();
        Events.fire(new PlayerBanEvent(Groups.player.find(p -> id.equals(p.uuid())), id));
        return true;
    }

    /**
     * Unbans a player by IP; returns whether this player was banned in the first place.
     * This method also unbans any player that was banned and had this IP.
     */
    public boolean unbanPlayerIP(String ip){
        boolean found = bannedIPs.contains(ip, false);

        for(PlayerInfo info : playerInfo.values()){
            if(info.ips.contains(ip, false)){
                info.banned = false;
                found = true;
            }
        }

        bannedIPs.remove(ip, false);

        if(found){
            save();
            Events.fire(new PlayerIpUnbanEvent(ip));
        }
        return found;
    }

    /**
     * Unbans a player by ID; returns whether this player was banned in the first place.
     * This also unbans all IPs the player used.
     */
    public boolean unbanPlayerID(String id){
        PlayerInfo info = getCreateInfo(id);

        if(!info.banned) return false;

        info.banned = false;
        bannedIPs.removeAll(info.ips, false);
        save();
        Events.fire(new PlayerUnbanEvent(Groups.player.find(p -> id.equals(p.uuid())), id));
        return true;
    }

    /**
     * Returns list of all players with admin status
     */
    public Seq<PlayerInfo> getAdmins(){
        Seq<PlayerInfo> result = new Seq<>();
        for(PlayerInfo info : playerInfo.values()){
            if(info.admin){
                result.add(info);
            }
        }
        return result;
    }

    /**
     * Returns list of all players which are banned
     */
    public Seq<PlayerInfo> getBanned(){
        Seq<PlayerInfo> result = new Seq<>();
        for(PlayerInfo info : playerInfo.values()){
            if(info.banned){
                result.add(info);
            }
        }
        return result;
    }

    /**
     * Returns all banned IPs. This does not include the IPs of ID-banned players.
     */
    public Seq<String> getBannedIPs(){
        return bannedIPs;
    }

    /**
     * Makes a player an admin.
     * @return whether this player was already an admin.
     */
    public boolean adminPlayer(String id, String usid){
        PlayerInfo info = getCreateInfo(id);

        var wasAdmin = info.admin;

        info.adminUsid = usid;
        info.admin = true;
        save();

        return wasAdmin;
    }

    /**
     * Makes a player no longer an admin.
     * @return whether this player was an admin in the first place.
     */
    public boolean unAdminPlayer(String id){
        PlayerInfo info = getCreateInfo(id);

        if(!info.admin) return false;

        info.admin = false;
        save();

        return true;
    }

    public boolean isWhitelistEnabled(){
        return Config.whitelist.bool();
    }

    public boolean isWhitelisted(String id, String usid){
        return !isWhitelistEnabled() || whitelist.contains(usid + id);
    }

    public boolean whitelist(String id){
        PlayerInfo info = getCreateInfo(id);
        if(whitelist.contains(info.adminUsid + id)) return false;
        whitelist.add(info.adminUsid + id);
        save();
        return true;
    }

    public boolean unwhitelist(String id){
        PlayerInfo info = getCreateInfo(id);
        if(whitelist.contains(info.adminUsid + id)){
            whitelist.remove(info.adminUsid + id);
            save();
            return true;
        }
        return false;
    }

    public boolean isIPBanned(String ip){
        return bannedIPs.contains(ip, false) || (findByIP(ip) != null && findByIP(ip).banned);
    }

    public boolean isIDBanned(String uuid){
        return getCreateInfo(uuid).banned;
    }

    public boolean isAdmin(String id, String usid){
        PlayerInfo info = getCreateInfo(id);
        return info.admin && usid.equals(info.adminUsid);
    }

    /** Finds player info by IP, UUID and name. */
    public ObjectSet<PlayerInfo> findByName(String name){
        ObjectSet<PlayerInfo> result = new ObjectSet<>();

        for(PlayerInfo info : playerInfo.values()){
            if(info.lastName.equalsIgnoreCase(name) || info.names.contains(name, false)
            || Strings.stripColors(Strings.stripColors(info.lastName)).equals(name)
            || info.ips.contains(name, false) || info.id.equals(name)){
                result.add(info);
            }
        }

        return result;
    }

    /** Finds by name, using contains(). */
    public ObjectSet<PlayerInfo> searchNames(String name){
        ObjectSet<PlayerInfo> result = new ObjectSet<>();

        for(PlayerInfo info : playerInfo.values()){
            if(info.names.contains(n -> n.toLowerCase().contains(name.toLowerCase()) || Strings.stripColors(n).trim().toLowerCase().contains(name))){
                result.add(info);
            }
        }

        return result;
    }

    public Seq<PlayerInfo> findByIPs(String ip){
        Seq<PlayerInfo> result = new Seq<>();

        for(PlayerInfo info : playerInfo.values()){
            if(info.ips.contains(ip, false)){
                result.add(info);
            }
        }

        return result;
    }

    public PlayerInfo getInfo(String id){
        return getCreateInfo(id);
    }

    public PlayerInfo getInfoOptional(String id){
        return playerInfo.get(id);
    }

    public PlayerInfo findByIP(String ip){
        for(PlayerInfo info : playerInfo.values()){
            if(info.ips.contains(ip, false)){
                return info;
            }
        }
        return null;
    }

    public Seq<PlayerInfo> getWhitelisted(){
        return playerInfo.values().toSeq().select(p -> isWhitelisted(p.id, p.adminUsid));
    }

    private PlayerInfo getCreateInfo(String id){
        if(playerInfo.containsKey(id)){
            return playerInfo.get(id);
        }else{
            PlayerInfo info = new PlayerInfo(id);
            playerInfo.put(id, info);
            save();
            return info;
        }
    }

    public void save(){
        Core.settings.putJson("player-data", playerInfo);
        Core.settings.putJson("ip-kicks", kickedIPs);
        Core.settings.putJson("ip-bans", String.class, bannedIPs);
        Core.settings.putJson("whitelist-ids", String.class, whitelist);
        Core.settings.putJson("banned-subnets", String.class, subnetBans);
    }

    @SuppressWarnings("unchecked")
    private void load(){
        //load default data
        playerInfo = Core.settings.getJson("player-data", ObjectMap.class, ObjectMap::new);
        kickedIPs = Core.settings.getJson("ip-kicks", ObjectMap.class, ObjectMap::new);
        bannedIPs = Core.settings.getJson("ip-bans", Seq.class, Seq::new);
        whitelist = Core.settings.getJson("whitelist-ids", Seq.class, Seq::new);
        subnetBans = Core.settings.getJson("banned-subnets", Seq.class, Seq::new);
    }

    /**
     * Server configuration definition. Each config value can be a string, boolean or number.
     * Creating a new Config instance implicitly adds it to the list of server configs. This can be used for custom plugin configuration.
     * */
    public static class Config{
        public static final Seq<Config> all = new Seq<>();

        public static final Config

        serverName = new Config("name", "The server name as displayed on clients.", "Server", "servername"),
        desc = new Config("desc", "The server description, displayed under the name. Max 100 characters.", "off"),
        port = new Config("port", "The port to host on.", Vars.port),
        autoUpdate = new Config("autoUpdate", "Whether to auto-update and exit when a new bleeding-edge update arrives.", false),
        showConnectMessages = new Config("showConnectMessages", "Whether to display connect/disconnect messages.", true),
        enableVotekick = new Config("enableVotekick", "Whether votekick is enabled.", true),
        startCommands = new Config("startCommands", "Commands run at startup. This should be a comma-separated list.", ""),
        logging = new Config("logging", "Whether to log everything to files.", true),
        strict = new Config("strict", "Whether strict mode is on - corrects positions and prevents duplicate UUIDs.", true),
        antiSpam = new Config("antiSpam", "Whether spammers are automatically kicked and rate-limited.", headless),
        interactRateWindow = new Config("interactRateWindow", "Block interaction rate limit window, in seconds.", 6),
        interactRateLimit = new Config("interactRateLimit", "Block interaction rate limit.", 25),
        interactRateKick = new Config("interactRateKick", "How many times a player must interact inside the window to get kicked.", 60),
        messageRateLimit = new Config("messageRateLimit", "Message rate limit in seconds. 0 to disable.", 0),
        messageSpamKick = new Config("messageSpamKick", "How many times a player must send a message before the cooldown to get kicked. 0 to disable.", 3),
        packetSpamLimit = new Config("packetSpamLimit", "Limit for packet count sent within 3sec that will lead to a blacklist + kick.", 300),
        chatSpamLimit = new Config("chatSpamLimit", "Limit for chat packet count sent within 2sec that will lead to a blacklist + kick. Not the same as a rate limit.", 20),
        socketInput = new Config("socketInput", "Allows a local application to control this server through a local TCP socket.", false, "socket", () -> Events.fire(Trigger.socketConfigChanged)),
        socketInputPort = new Config("socketInputPort", "The port for socket input.", 6859, () -> Events.fire(Trigger.socketConfigChanged)),
        socketInputAddress = new Config("socketInputAddress", "The bind address for socket input.", "localhost", () -> Events.fire(Trigger.socketConfigChanged)),
        allowCustomClients = new Config("allowCustomClients", "Whether custom clients are allowed to connect.", !headless, "allow-custom"),
        whitelist = new Config("whitelist", "Whether the whitelist is used.", false),
        motd = new Config("motd", "The message displayed to people on connection.", "off"),
        autosave = new Config("autosave", "Whether the periodically save the map when playing.", false),
        autosaveAmount = new Config("autosaveAmount", "The maximum amount of autosaves. Older ones get replaced.", 10),
        autosaveSpacing = new Config("autosaveSpacing", "Spacing between autosaves in seconds.", 60 * 5),
        debug = new Config("debug", "Enable debug logging.", false, () -> Log.level = debug() ? LogLevel.debug : LogLevel.info),
        snapshotInterval = new Config("snapshotInterval", "Client entity snapshot interval in ms.", 200),
        autoPause = new Config("autoPause", "Whether the game should pause when nobody is online.", false);

        public final Object defaultValue;
        public final String name, key, description;

        final Runnable changed;

        public Config(String name, String description, Object def){
            this(name, description, def, null, null);
        }

        public Config(String name, String description, Object def, String key){
            this(name, description, def, key, null);
        }

        public Config(String name, String description, Object def, Runnable changed){
            this(name, description, def, null, changed);
        }

        public Config(String name, String description, Object def, String key, Runnable changed){
            this.name = name;
            this.description = description;
            this.key = key == null ? name : key;
            this.defaultValue = def;
            this.changed = changed == null ? () -> {} : changed;

            all.add(this);
        }

        public boolean isNum(){
            return defaultValue instanceof Integer;
        }

        public boolean isBool(){
            return defaultValue instanceof Boolean;
        }

        public boolean isString(){
            return defaultValue instanceof String;
        }

        public Object get(){
            return Core.settings.get(key, defaultValue);
        }

        public boolean bool(){
            return Core.settings.getBool(key, (Boolean)defaultValue);
        }

        public int num(){
            return Core.settings.getInt(key, (Integer)defaultValue);
        }

        public String string(){
            return Core.settings.getString(key, (String)defaultValue);
        }

        public void set(Object value){
            Core.settings.put(key, value);
            changed.run();
        }

        private static boolean debug(){
            return Config.debug.bool();
        }
    }

    public static class PlayerInfo{
        public String id;
        public String lastName = "<unknown>", lastIP = "<unknown>";
        public Seq<String> ips = new Seq<>();
        public Seq<String> names = new Seq<>();
        public String adminUsid;
        public int timesKicked;
        public int timesJoined;
        public boolean banned, admin;
        public long lastKicked; //last kicked time to expiration

        public transient long lastMessageTime, lastSyncTime;
        public transient String lastSentMessage;
        public transient int messageInfractions;
        public transient Ratekeeper rate = new Ratekeeper();
        public transient Interval messageTimer = new Interval();

        PlayerInfo(String id){
            this.id = id;
        }

        public PlayerInfo(){
        }

        public String plainLastName(){
            return Strings.stripColors(lastName);
        }
    }

    /** Handles chat messages from players and changes their contents. */
    public interface ChatFilter{
        /** @return the filtered message; a null string signals that the message should not be sent. */
        @Nullable String filter(Player player, String message);
    }

    /** Allows or disallows player actions. */
    public interface ActionFilter{
        /** @return whether this action should be permitted. if applicable, make sure to send this player a message specify why the action was prohibited. */
        boolean allow(PlayerAction action);
    }

    public static class TraceInfo{
        public String ip, uuid;
        public boolean modded, mobile;
        public int timesJoined, timesKicked;

        public TraceInfo(String ip, String uuid, boolean modded, boolean mobile, int timesJoined, int timesKicked){
            this.ip = ip;
            this.uuid = uuid;
            this.modded = modded;
            this.mobile = mobile;
            this.timesJoined = timesJoined;
            this.timesKicked = timesKicked;
        }
    }

    /** Defines a (potentially dangerous) action that a player has done in the world.
     * These objects are pooled; do not cache them! */
    public static class PlayerAction implements Poolable{
        public Player player;
        public ActionType type;
        public @Nullable Tile tile;

        /** valid for block placement events only */
        public @Nullable Block block;
        public int rotation;

        /** valid for configure and rotation-type events only. */
        public Object config;

        /** valid for item-type events only. */
        public @Nullable Item item;
        public int itemAmount;

        /** valid for unit-type events only, and even in that case may be null. */
        public @Nullable Unit unit;

        /** valid only for removePlanned events only; contains packed positions. */
        public @Nullable int[] plans;

        /** valid only for command unit events */
        public @Nullable int[] unitIDs;

        /** valid only for command building events */
        public @Nullable int[] buildingPositions;

        public PlayerAction set(Player player, ActionType type, Tile tile){
            this.player = player;
            this.type = type;
            this.tile = tile;
            return this;
        }

        public PlayerAction set(Player player, ActionType type, Unit unit){
            this.player = player;
            this.type = type;
            this.unit = unit;
            return this;
        }

        @Override
        public void reset(){
            item = null;
            itemAmount = 0;
            config = null;
            player = null;
            type = null;
            tile = null;
            block = null;
            unit = null;
            plans = null;
        }
    }

    public enum ActionType{
        breakBlock, placeBlock, rotate, configure, withdrawItem, depositItem, control, buildSelect, command, removePlanned, commandUnits, commandBuilding, respawn
    }

}
