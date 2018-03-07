package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.core.Settings;

public class Administration {
    private Json json = new Json();
    private Array<String> bannedIPs = new Array<>();
    private Array<String> bannedIDs = new Array<>();
    private Array<String> admins = new Array<>();
    private ObjectMap<String, String> ipNames = new ObjectMap<>();
    private ObjectMap<String, String> idIPs = new ObjectMap<>();
    private ObjectMap<String, TraceInfo> traces = new ObjectMap<>();

    public Administration(){
        Settings.defaultList(
            "bans", "{}",
            "bannedIDs", "{}",
            "admins", "{}",
            "knownIPs", "{}",
            "knownIDs", "{}"
        );

        load();
    }

    public TraceInfo getTrace(String ip){
        if(!traces.containsKey(ip)) traces.put(ip, new TraceInfo(ip));

        return traces.get(ip);
    }

    public void clearTraces(){
        traces.clear();
    }

    /**Sets last known name for an IP.*/
    public void setKnownName(String ip, String name){
        ipNames.put(ip, name);
        saveKnown();
    }

    /**Sets last known UUID for an IP.*/
    public void setKnownIP(String id, String ip){
        idIPs.put(id, ip);
        saveKnown();
    }

    /**Returns the last known name for an IP. Returns 'unknown' if this IP has an unknown username.*/
    public String getLastName(String ip){
        return ipNames.get(ip, "unknown");
    }

    /**Returns the last known IP for a UUID. Returns 'unknown' if this IP has an unknown IP.*/
    public String getLastIP(String id){
        return idIPs.get(id, "unknown");
    }

    /**Return the last known device ID associated with an IP.  Returns 'unknown' if this IP has an unknown device.*/
    public String getLastID(String ip){
        for(String id : idIPs.keys()){
            if(idIPs.get(id).equals(ip)){
                return id;
            }
        }
        return "unknown";
    }

    /**Returns list of banned IPs.*/
    public Array<String> getBanned(){
        return bannedIPs;
    }

    /**Returns list of banned IDs.*/
    public Array<String> getBannedIDs(){
        return bannedIDs;
    }

    /**Bans a player by IP; returns whether this player was already banned.*/
    public boolean banPlayerIP(String ip){
        if(bannedIPs.contains(ip, false))
            return false;
        bannedIPs.add(ip);
        saveBans();

        return true;
    }

    /**Bans a player by UUID.*/
    public boolean banPlayerID(String id){
        if(bannedIDs.contains(id, false))
            return false;
        bannedIDs.add(id);
        saveBans();

        return true;
    }

    /**Unbans a player by IP; returns whether this player was banned in the first place..*/
    public boolean unbanPlayerIP(String ip){
        if(!bannedIPs.contains(ip, false))
            return false;
        bannedIPs.removeValue(ip, false);
        saveBans();

        return true;
    }

    /**Unbans a player by IP; returns whether this player was banned in the first place..*/
    public boolean unbanPlayerID(String ip){
        if(!bannedIDs.contains(ip, false))
            return false;
        bannedIDs.removeValue(ip, false);
        saveBans();

        return true;
    }

    /**Returns list of banned IPs.*/
    public Array<String> getAdmins(){
        return admins;
    }

    /**Makes a player an admin. Returns whether this player was already an admin.*/
    public boolean adminPlayer(String ip){
        if(admins.contains(ip, false))
            return false;
        admins.add(ip);
        saveAdmins();

        return true;
    }

    /**Makes a player no longer an admin. Returns whether this player was an admin in the first place.*/
    public boolean unAdminPlayer(String ip){
        if(!admins.contains(ip, false))
            return false;
        admins.removeValue(ip, false);
        saveAdmins();

        return true;
    }

    public boolean isIPBanned(String ip){
        return bannedIPs.contains(ip, false);
    }

    public boolean isIDBanned(String uuid){
        return bannedIDs.contains(uuid, false);
    }

    public boolean isAdmin(String ip){
        return admins.contains(ip, false);
    }

    private void saveKnown(){
        Settings.putString("knownIPs", json.toJson(ipNames));
        Settings.putString("knownIDs", json.toJson(idIPs));
        Settings.save();
    }

    private void saveBans(){
        Settings.putString("bans", json.toJson(bannedIPs));
        Settings.putString("bannedIDs", json.toJson(bannedIDs));
        Settings.save();
    }

    private void saveAdmins(){
        Settings.putString("admins", json.toJson(admins));
        Settings.save();
    }

    private void load(){
        bannedIPs = json.fromJson(Array.class, Settings.getString("bans"));
        bannedIDs = json.fromJson(Array.class, Settings.getString("bannedIDs"));
        admins = json.fromJson(Array.class, Settings.getString("admins"));
        ipNames = json.fromJson(ObjectMap.class, Settings.getString("knownIPs"));
        idIPs = json.fromJson(ObjectMap.class, Settings.getString("knownIDs"));
    }

}
