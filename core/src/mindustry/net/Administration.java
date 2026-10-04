package mindustry.net;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import arc.util.pooling.Pool.*;
import arc.util.pooling.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.game.Interval;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;

import java.io.*;
import java.util.regex.*;
import java.util.zip.*;

import static mindustry.Vars.*;
import static mindustry.game.EventType.*;

public class Administration{
    /** All player info. Maps UUIDs to info. This persists throughout restarts. Do not modify directly. */
    public ObjectMap<String, PlayerInfo> playerInfo = new ObjectMap<>();
    public Seq<String> bannedIPs = new Seq<>();
    public Seq<String> whitelist = new Seq<>();
    public Seq<ChatFilter> chatFilters = new Seq<>();
    public Seq<ActionFilter> actionFilters = new Seq<>();
    public Seq<String> subnetBans = new Seq<>();
    public ObjectSet<String> dosBlacklist = new ObjectSet<>();
    public ObjectMap<String, Long> kickedIPs = new ObjectMap<>();
    public Seq<Pattern> bannedNames = new Seq<>();

    private final Fi bansDirectory = dataDirectory.child("bans");
    private final Fi ipsFile = bansDirectory.child("ips.txt"), subnetsFile = bansDirectory.child("subnet.txt"), namesFile = bansDirectory.child("names.txt");
    private final Fi whitelistFile = dataDirectory.child("whitelist.txt"), playersFile = dataDirectory.child("players.dat");

    private boolean playersModified, ipsModified, subnetsModified, namesModified, whitelistModified;
    private ObjectMap<String, IdEncounterInfo> encounteredIDsForIp = new ObjectMap<>();

    public Administration(){
        load();

        //anti-spam
        addChatFilter((player, message) -> {
            long resetTime = netServer.config.messageRateLimit * 1000L;
            if(netServer.config.antiSpam && !player.isLocal() && !player.admin){
                //prevent people from spamming messages quickly
                if(resetTime > 0 && Time.timeSinceMillis(player.getInfo().lastMessageTime) < resetTime){
                    //supress message
                    player.sendMessage("[scarlet]You may only send messages every " + netServer.config.messageRateLimit + " seconds.");
                    player.getInfo().messageInfractions ++;
                    //kick player for spamming and prevent connection if they've done this several times
                    if(player.getInfo().messageInfractions >= netServer.config.messageSpamKick && netServer.config.messageSpamKick != 0){
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
                netServer.config.antiSpam && !action.player.isLocal()){

                Ratekeeper rate = action.player.getInfo().rate;
                if(rate.allow(netServer.config.interactRateWindow * 1000L, netServer.config.interactRateLimit)){
                    return true;
                }else{
                    if(rate.occurences > netServer.config.interactRateKick){
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

    public synchronized void unBlacklistDos(String address){
        dosBlacklist.remove(address);
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
        saveSubnetBans();
    }

    public void addSubnetBan(String ip){
        subnetBans.add(ip);
        saveSubnetBans();
    }

    public boolean isSubnetBanned(String ip){
        return subnetBans.contains(ip::startsWith);
    }

    public void addNameBan(String regex) throws PatternSyntaxException{
        bannedNames.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        saveNameBans();
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

    //TODO: remove when Core.settings is refactored
    public int getPlayerLimit(){
        return headless ? netServer.config.playerLimit : Core.settings.getInt("playerlimit", 0);
    }

    public boolean isStrict(){
        return netServer.config.strict;
    }

    public boolean allowsCustomClients(){
        return netServer.config.allowCustomClients;
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
        saveBannedIPs();
        Events.fire(new PlayerIpBanEvent(ip));
        return true;
    }

    /** Bans a player by UUID; returns whether this player was already banned. */
    public boolean banPlayerID(String id){
        if(playerInfo.containsKey(id) && playerInfo.get(id).banned)
            return false;

        getCreateInfo(id).banned = true;

        save();
        Events.fire(new PlayerBanEvent(state.entities.player.find(p -> id.equals(p.uuid())), id));
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
            saveBannedIPs();
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
        saveBannedIPs();
        Events.fire(new PlayerUnbanEvent(state.entities.player.find(p -> id.equals(p.uuid())), id));
        return true;
    }

    public boolean checkUuidChanges(String address, String uuid){
        if(netServer.config.uuidChangeLimit <= 1) return false;

        var set = encounteredIDsForIp.get(address, IdEncounterInfo::new);
        //clear encountered list every hour
        if(Time.timeSinceMillis(set.initialTime) > 1000L * 60 * 60 * netServer.config.uuidChangeTimePeriod){
            set.ids.clear();
            set.initialTime = Time.millis();
        }
        set.ids.add(uuid);

        if(set.ids.size > netServer.config.uuidChangeLimit){
            banPlayerIP(address);
            return true;
        }
        return false;
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
        return netServer.config.whitelist;
    }

    public boolean isWhitelisted(String id, String usid){
        return !isWhitelistEnabled() || whitelist.contains(usid + id);
    }

    public boolean whitelist(String id){
        PlayerInfo info = getCreateInfo(id);
        if(whitelist.contains(info.adminUsid + id)) return false;
        whitelist.add(info.adminUsid + id);
        saveWhitelist();
        return true;
    }

    public boolean unwhitelist(String id){
        PlayerInfo info = getCreateInfo(id);
        if(whitelist.contains(info.adminUsid + id)){
            whitelist.remove(info.adminUsid + id);
            saveWhitelist();
            return true;
        }
        return false;
    }

    public boolean isIPBanned(String ip){
        return bannedIPs.contains(ip, false) || (findByIP(ip) != null && findByIP(ip).banned) || (steam && ip.startsWith("steam") && SteamAdmin.isBanned(ip));
    }

    public boolean isIDBanned(String uuid){
        return getCreateInfo(uuid).banned;
    }

    public boolean isNameBanned(String name){
        return bannedNames.size > 0 && bannedNames.contains(p -> p.matcher(name).find());
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

    /** Marks player data as modified. */
    public void save(){
        playersModified = true;
    }

    public void saveBannedIPs(){
        ipsModified = true;
    }

    public void saveSubnetBans(){
        subnetsModified = true;
    }

    public void saveNameBans(){
        namesModified = true;
    }

    public void saveWhitelist(){
        whitelistModified = true;
    }

    /** Writes every data set that has been modified since it was last written. */
    public void forceSave(){
        if(playersModified) writePlayers();
        if(ipsModified) ipsModified = !writeLines(ipsFile, bannedIPs);
        if(subnetsModified) subnetsModified = !writeLines(subnetsFile, subnetBans);
        if(namesModified) namesModified = !writeLines(namesFile, bannedNames.map(Pattern::pattern));
        if(whitelistModified) whitelistModified = !writeLines(whitelistFile, whitelist);
    }

    private void writePlayers(){
        try{
            FileIO.atomicWrite(playersFile, tmp -> {
                try(DataOutputStream out = new DataOutputStream(new FastDeflaterOutputStream(tmp.write(false, 8192)))){
                    JsonIO.writeBytes(playerInfo, PlayerInfo.class, out);
                }
            });
            playersModified = false;
        }catch(Throwable t){
            Log.err("Failed to write " + playersFile.name() + "!", t);
        }
    }

    private boolean writeLines(Fi file, Seq<String> lines){
        try{
            FileIO.atomicWrite(file, tmp -> FileIO.writeLines(lines, tmp));
            return true;
        }catch(Throwable t){
            Log.err("Failed to write " + file.name() + "!", t);
            return false;
        }
    }

    private Seq<String> loadLines(Fi file){
        return file.exists() ? FileIO.readLines(file) : new Seq<>();
    }

    @SuppressWarnings("unchecked")
    private void load(){
        bannedIPs = loadLines(ipsFile);
        subnetBans = loadLines(subnetsFile);
        whitelist = loadLines(whitelistFile);

        for(String regex : loadLines(namesFile)){
            try{
                bannedNames.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            }catch(Exception ignored){
            }
        }

        if(playersFile.exists()){
            try(DataInputStream in = new DataInputStream(new InflaterInputStream(playersFile.read(8192)))){
                playerInfo = JsonIO.readBytes(ObjectMap.class, PlayerInfo.class, in);
            }catch(Throwable t){
                if(headless){
                    throw new RuntimeException("Failed to read " + playersFile.name() + "! This is non-recoverable, fix or remove the file before starting again.", t);
                }else{
                    //crashing isn't acceptable on the client
                    Log.err("Failed to read player data file. It will be wiped.", t);
                }
            }
        }
    }

    public static class PlayerInfo implements AllowSerialization{
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
        public transient mindustry.game.Interval messageTimer = new Interval();

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
        public String ip, uuid, locale;
        public boolean modded, mobile;
        public int timesJoined, timesKicked;
        public String[] ips, names;

        public TraceInfo(String ip, String uuid, String locale, boolean modded, boolean mobile, int timesJoined, int timesKicked, String[] ips, String[] names){
            this.ip = ip;
            this.uuid = uuid;
            this.locale = locale;
            this.modded = modded;
            this.mobile = mobile;
            this.timesJoined = timesJoined;
            this.timesKicked = timesKicked;
            this.names = names;
            this.ips = ips;
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

        /** valid only for payload events */
        public @Nullable Payload payload;

        /** valid only for removePlanned events only; contains packed positions. */
        public @Nullable int[] plans;

        /** valid only for command unit events */
        public @Nullable int[] unitIDs;
        public @Nullable UnitCommand unitCommand;

        /** valid only for command building events */
        public @Nullable int[] buildingPositions;

        /** valid only for location pings */
        public @Nullable String pingText;
        public float pingX, pingY;

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
        breakBlock, placeBlock, rotate, configure, withdrawItem, depositItem, control, buildSelect, command, removePlanned, commandUnits, commandBuilding, respawn, pickupBlock, dropPayload, pingLocation
    }

    static class IdEncounterInfo{
        long initialTime;
        ObjectSet<String> ids = new ObjectSet<>();
    }
}
