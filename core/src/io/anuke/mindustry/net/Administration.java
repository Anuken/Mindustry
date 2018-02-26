package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.ucore.core.Settings;

public class Administration {
    private Json json = new Json();
    private Array<String> bannedIPS = new Array<>();
    private Array<String> admins = new Array<>();
    private ObjectMap<String, String> known = new ObjectMap<>();

    public Administration(){
        Settings.defaultList(
            "bans", "{}",
            "admins", "{}",
            "knownIPs", "{}"
        );

        load();
    }

    /**Sets last known name for an IP.*/
    public void setKnownName(String ip, String name){
        known.put(ip, name);
        saveKnown();
    }

    /**Returns the last known name for an IP. Returns 'unknown' if this IP has an unknown username.*/
    public String getLastName(String ip){
        return known.get(ip, "unknown");
    }

    /**Returns list of banned IPs.*/
    public Array<String> getBanned(){
        return bannedIPS;
    }

    /**Bans a player by IP; returns whether this player was already banned.*/
    public boolean banPlayer(String ip){
        if(bannedIPS.contains(ip, false))
            return false;
        bannedIPS.add(ip);
        saveBans();

        return true;
    }

    /**Unbans a player by IP; returns whether this player was banned in the first place..*/
    public boolean unbanPlayer(String ip){
        if(!bannedIPS.contains(ip, false))
            return false;
        bannedIPS.removeValue(ip, false);
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

    public boolean isBanned(String ip){
        return bannedIPS.contains(ip, false);
    }

    public boolean isAdmin(String ip){
        return admins.contains(ip, false);
    }

    private void saveKnown(){
        Settings.putString("knownIPs", json.toJson(known));
        Settings.save();
    }

    private void saveBans(){
        Settings.putString("bans", json.toJson(bannedIPS));
        Settings.save();
    }

    private void saveAdmins(){
        Settings.putString("admins", json.toJson(admins));
        Settings.save();
    }

    private void load(){
        bannedIPS = json.fromJson(Array.class, Settings.getString("bans"));
        admins = json.fromJson(Array.class, Settings.getString("admins"));
        known = json.fromJson(ObjectMap.class, Settings.getString("knownIPs"));
    }

}
