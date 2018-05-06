package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Settings;

public class Administration {
    public static final int defaultMaxBrokenBlocks = 15;
    public static final int defaultBreakCooldown = 1000*15;

    private Json json = new Json();
    /**All player info. Maps UUIDs to info. This persists throughout restarts.*/
    private ObjectMap<String, PlayerInfo> playerInfo = new ObjectMap<>();
    /**Maps UUIDs to trace infos. This is wiped when a player logs off.*/
    private ObjectMap<String, TraceInfo> traceInfo = new ObjectMap<>();
    private Array<String> bannedIPs = new Array<>();

    public Administration(){
        Settings.defaultList(
            "playerInfo", "{}",
            "bannedIPs", "{}",
            "antigrief", false,
            "antigrief-max", defaultMaxBrokenBlocks,
            "antigrief-cooldown", defaultBreakCooldown
        );

        load();
    }

    public boolean isAntiGrief(){
        return Settings.getBool("antigrief");
    }

    public boolean isValidateReplace(){
        return false;
    }

    public void setAntiGrief(boolean antiGrief){
        Settings.putBool("antigrief", antiGrief);
        Settings.save();
    }

    public void setAntiGriefParams(int maxBreak, int cooldown){
        Settings.putInt("antigrief-max", maxBreak);
        Settings.putInt("antigrief-cooldown", cooldown);
        Settings.save();
    }

    public boolean validateBreak(String id, String ip){
        if(!isAntiGrief() || isAdmin(id, ip)) return true;

        PlayerInfo info = getCreateInfo(id);

        if(info.lastBroken == null || info.lastBroken.length != Settings.getInt("antigrief-max")){
            info.lastBroken = new long[Settings.getInt("antigrief-max")];
        }

        long[] breaks = info.lastBroken;

        int shiftBy = 0;
        for(int i = 0; i < breaks.length && breaks[i] != 0; i ++){
            if(TimeUtils.timeSinceMillis(breaks[i]) >= Settings.getInt("antigrief-cooldown")){
                shiftBy = i;
            }
        }

        for (int i = 0; i < breaks.length; i++) {
            breaks[i] = (i + shiftBy >= breaks.length) ? 0 : breaks[i + shiftBy];
        }

        int remaining = 0;
        for(int i = 0; i < breaks.length; i ++){
            if(breaks[i] == 0){
                remaining = breaks.length - i;
                break;
            }
        }

        if(remaining == 0) return false;

        breaks[breaks.length - remaining] = TimeUtils.millis();
        return true;
    }

    /**Call when a player joins to update their information here.*/
    public void updatePlayerJoined(String id, String ip, String name){
        PlayerInfo info = getCreateInfo(id);
        info.lastName = name;
        info.lastIP = ip;
        info.timesJoined ++;
        if(!info.names.contains(name, false)) info.names.add(name);
        if(!info.ips.contains(ip, false)) info.ips.add(ip);
    }

    /**Returns trace info by IP.*/
    public TraceInfo getTrace(String ip){
        if(!traceInfo.containsKey(ip)) traceInfo.put(ip, new TraceInfo(ip));

        return traceInfo.get(ip);
    }

    public void clearTraces(){
        traceInfo.clear();
    }

    /**Bans a player by IP; returns whether this player was already banned.
     * If there are players who at any point had this IP, they will be UUID banned as well.*/
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

        return true;
    }

    /**Bans a player by UUID; returns whether this player was already banned.*/
    public boolean banPlayerID(String id){
        if(playerInfo.containsKey(id) && playerInfo.get(id).banned)
            return false;

        getCreateInfo(id).banned = true;

        save();

        return true;
    }

    /**Unbans a player by IP; returns whether this player was banned in the first place.
     * This method also unbans any player that was banned and had this IP.*/
    public boolean unbanPlayerIP(String ip){
        boolean found = bannedIPs.contains(ip, false);

        for(PlayerInfo info : playerInfo.values()){
            if(info.ips.contains(ip, false)){
                info.banned = false;
                found = true;
            }
        }

        bannedIPs.removeValue(ip, false);

        if(found) save();

        return found;
    }

    /**Unbans a player by ID; returns whether this player was banned in the first place.
     * This also unbans all IPs the player used.*/
    public boolean unbanPlayerID(String id){
        PlayerInfo info = getCreateInfo(id);

        if(!info.banned)
            return false;

        info.banned = false;
        bannedIPs.removeAll(info.ips, false);
        save();

        return true;
    }

    /**Returns list of all players with admin status*/
    public Array<PlayerInfo> getAdmins(){
        Array<PlayerInfo> result = new Array<>();
        for(PlayerInfo info : playerInfo.values()){
            if(info.admin){
                result.add(info);
            }
        }
        return result;
    }

    /**Returns list of all players with admin status*/
    public Array<PlayerInfo> getBanned(){
        Array<PlayerInfo> result = new Array<>();
        for(PlayerInfo info : playerInfo.values()){
            if(info.banned){
                result.add(info);
            }
        }
        return result;
    }

    /**Returns all banned IPs. This does not include the IPs of ID-banned players.*/
    public Array<String> getBannedIPs(){
        return bannedIPs;
    }

    /**Makes a player an admin. Returns whether this player was already an admin.*/
    public boolean adminPlayer(String id, String ip){
        PlayerInfo info = getCreateInfo(id);

        if(info.admin)
            return false;

        info.validAdminIP = ip;
        info.admin = true;
        save();

        return true;
    }

    /**Makes a player no longer an admin. Returns whether this player was an admin in the first place.*/
    public boolean unAdminPlayer(String id){
        PlayerInfo info = getCreateInfo(id);

        if(!info.admin)
            return false;

        info.admin = false;
        save();

        return true;
    }

    public boolean isIPBanned(String ip){
        return bannedIPs.contains(ip, false) || (findByIP(ip) != null && findByIP(ip).banned);
    }

    public boolean isIDBanned(String uuid){
        return getCreateInfo(uuid).banned;
    }

    public boolean isAdmin(String id, String ip){
        PlayerInfo info = getCreateInfo(id);
        return info.admin && ip.equals(info.validAdminIP);
    }

    public Array<PlayerInfo> findByName(String name, boolean last){
        Array<PlayerInfo> result = new Array<>();

        for(PlayerInfo info : playerInfo.values()){
            if(info.lastName.toLowerCase().equals(name.toLowerCase()) || (last && info.names.contains(name, false))){
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
        Settings.putString("playerInfo", json.toJson(playerInfo));
        Settings.putString("bannedIPs", json.toJson(bannedIPs));
        Settings.save();
    }

    private void load(){
        playerInfo = json.fromJson(ObjectMap.class, Settings.getString("playerInfo"));
        bannedIPs = json.fromJson(Array.class, Settings.getString("bannedIPs"));
    }

    public static class PlayerInfo{
        public String id;
        public String lastName = "<unknown>", lastIP = "<unknown>";
        public String validAdminIP;
        public Array<String> ips = new Array<>();
        public Array<String> names = new Array<>();
        public int timesKicked; //TODO not implemented!
        public int timesJoined;
        public int totalBlockPlaced;
        public int totalBlocksBroken;
        public boolean banned, admin;
        public long lastKicked; //last kicked timestamp

        public long[] lastBroken;

        PlayerInfo(String id){
            this.id = id;
        }

        private PlayerInfo(){}
    }

}
