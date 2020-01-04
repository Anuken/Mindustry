package mindustry.net;

import arc.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.type.*;

import static mindustry.Vars.headless;
import static mindustry.game.EventType.*;

public class Administration{
    /** All player info. Maps UUIDs to info. This persists throughout restarts. */
    private ObjectMap<String, PlayerInfo> playerInfo = new ObjectMap<>();
    private Array<String> bannedIPs = new Array<>();
    private Array<String> whitelist = new Array<>();
    private Array<ChatFilter> chatFilters = new Array<>();

    public Administration(){
        load();
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
            current = f.filter(player, message);
            if(current == null) return null;
        }
        return current;
    }

    public int getPlayerLimit(){
        return Core.settings.getInt("playerlimit", 0);
    }

    public void setPlayerLimit(int limit){
        Core.settings.putSave("playerlimit", limit);
    }

    public boolean getStrict(){
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
        Events.fire(new PlayerBanEvent(Vars.playerGroup.find(p -> id.equals(p.uuid))));
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

        bannedIPs.removeValue(ip, false);

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

        if(!info.banned)
            return false;

        info.banned = false;
        bannedIPs.removeAll(info.ips, false);
        save();
        Events.fire(new PlayerUnbanEvent(Vars.playerGroup.find(p -> id.equals(p.uuid))));
        return true;
    }

    /**
     * Returns list of all players with admin status
     */
    public Array<PlayerInfo> getAdmins(){
        Array<PlayerInfo> result = new Array<>();
        for(PlayerInfo info : playerInfo.values()){
            if(info.admin){
                result.add(info);
            }
        }
        return result;
    }

    /**
     * Returns list of all players with admin status
     */
    public Array<PlayerInfo> getBanned(){
        Array<PlayerInfo> result = new Array<>();
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
    public Array<String> getBannedIPs(){
        return bannedIPs;
    }

    /**
     * Makes a player an admin. Returns whether this player was already an admin.
     */
    public boolean adminPlayer(String id, String usid){
        PlayerInfo info = getCreateInfo(id);

        if(info.admin && info.adminUsid != null && info.adminUsid.equals(usid))
            return false;

        info.adminUsid = usid;
        info.admin = true;
        save();

        return true;
    }

    /**
     * Makes a player no longer an admin. Returns whether this player was an admin in the first place.
     */
    public boolean unAdminPlayer(String id){
        PlayerInfo info = getCreateInfo(id);

        if(!info.admin)
            return false;

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
            if(info.lastName.toLowerCase().equals(name.toLowerCase()) || (info.names.contains(name, false))
            || info.ips.contains(name, false) || info.id.equals(name)){
                result.add(info);
            }
        }

        return result;
    }

    public Array<PlayerInfo> findByIPs(String ip){
        Array<PlayerInfo> result = new Array<>();

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

    public Array<PlayerInfo> getWhitelisted(){
        return playerInfo.values().toArray().select(p -> isWhitelisted(p.id, p.adminUsid));
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
        Core.settings.putObject("player-info", playerInfo);
        Core.settings.putObject("banned-ips", bannedIPs);
        Core.settings.putObject("whitelisted", whitelist);
        Core.settings.save();
    }

    @SuppressWarnings("unchecked")
    private void load(){
        playerInfo = Core.settings.getObject("player-info", ObjectMap.class, ObjectMap::new);
        bannedIPs = Core.settings.getObject("banned-ips", Array.class, Array::new);
        whitelist = Core.settings.getObject("whitelisted", Array.class, Array::new);
    }

    /** Server configuration definition. Each config value can be a string, boolean or number. */
    public enum Config{
        name("The server name as displayed on clients.", "Server", "servername"),
        port("The port to host on.", Vars.port),
        autoUpdate("Whether to auto-update and exit when a new bleeding-edge update arrives.", false),
        showConnectMessages("Whether to display connect/disconnect messages.", true),
        enableVotekick("Whether votekick is enabled.", true),
        startCommands("Commands run at startup. This should be a comma-separated list.", ""),
        crashReport("Whether to send crash reports.", false, "crashreport"),
        logging("Whether to log everything to files.", true),
        strict("Whether strict mode is on - corrects positions and prevents duplicate UUIDs.", true),
        socketInput("Allows a local application to control this server through a local TCP socket.", false, "socket", () -> Events.fire(Trigger.socketConfigChanged)),
        socketInputPort("The port for socket input.", 6859, () -> Events.fire(Trigger.socketConfigChanged)),
        socketInputAddress("The bind address for socket input.", "localhost", () -> Events.fire(Trigger.socketConfigChanged)),
        allowCustomClients("Whether custom clients are allowed to connect.", !headless, "allow-custom"),
        whitelist("Whether the whitelist is used.", false),
        motd("The message displayed to people on connection.", "off");

        public static final Config[] all = values();

        public final Object defaultValue;
        public final String key, description;
        final Runnable changed;

        Config(String description, Object def){
            this(description, def, null, null);
        }

        Config(String description, Object def, String key){
            this(description, def, key, null);
        }

        Config(String description, Object def, Runnable changed){
            this(description, def, null, changed);
        }

        Config(String description, Object def, String key, Runnable changed){
            this.description = description;
            this.key = key == null ? name() : key;
            this.defaultValue = def;
            this.changed = changed == null ? () -> {} : changed;
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
            Core.settings.putSave(key, value);
            changed.run();
        }
    }

    @Serialize
    public static class PlayerInfo{
        public String id;
        public String lastName = "<unknown>", lastIP = "<unknown>";
        public Array<String> ips = new Array<>();
        public Array<String> names = new Array<>();
        public String adminUsid;
        public int timesKicked;
        public int timesJoined;
        public boolean banned, admin;
        public long lastKicked; //last kicked timestamp

        PlayerInfo(String id){
            this.id = id;
        }

        public PlayerInfo(){
        }
    }

    public interface ChatFilter{
        /** @return the filtered message; a null string signals that the message should not be sent. */
        @Nullable String filter(Player player, String message);
    }

    public static class TraceInfo{
        public String ip, uuid;
        public boolean modded, mobile;

        public TraceInfo(String ip, String uuid, boolean modded, boolean mobile){
            this.ip = ip;
            this.uuid = uuid;
            this.modded = modded;
            this.mobile = mobile;
        }
    }

}
