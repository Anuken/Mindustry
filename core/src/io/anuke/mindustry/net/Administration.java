package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.Rock;
import io.anuke.mindustry.world.blocks.StaticBlock;
import io.anuke.ucore.core.Settings;

import static io.anuke.mindustry.Vars.headless;
import static io.anuke.mindustry.Vars.world;

public class Administration{
    public static final int defaultMaxBrokenBlocks = 15;
    public static final int defaultBreakCooldown = 1000 * 15;

    /**
     * All player info. Maps UUIDs to info. This persists throughout restarts.
     */
    private ObjectMap<String, PlayerInfo> playerInfo = new ObjectMap<>();
    /**
     * Maps UUIDs to trace infos. This is wiped when a player logs off.
     */
    private ObjectMap<String, TraceInfo> traceInfo = new ObjectMap<>();
    /**
     * Maps packed coordinates to logs for that coordinate
     */
    private IntMap<Array<EditLog>> editLogs = new IntMap<>();

    private Array<String> bannedIPs = new Array<>();

    public Administration(){
        Settings.defaultList(
                "antigrief", false,
                "antigrief-max", defaultMaxBrokenBlocks,
                "antigrief-cooldown", defaultBreakCooldown
        );

        load();
    }

    public boolean isAntiGrief(){
        return Settings.getBool("antigrief");
    }

    public void setAntiGrief(boolean antiGrief){
        Settings.putBool("antigrief", antiGrief);
        Settings.save();
    }

    public boolean allowsCustomClients(){
        return Settings.getBool("allow-custom", !headless);
    }

    public void setCustomClients(boolean allowed){
        Settings.putBool("allow-custom", allowed);
        Settings.save();
    }

    public boolean isValidateReplace(){
        return false;
    }

    public void setAntiGriefParams(int maxBreak, int cooldown){
        Settings.putInt("antigrief-max", maxBreak);
        Settings.putInt("antigrief-cooldown", cooldown);
        Settings.save();
    }

    public IntMap<Array<EditLog>> getEditLogs(){
        return editLogs;
    }

    public void logEdit(int x, int y, Player player, Block block, int rotation, EditLog.EditAction action){
        if(block instanceof BlockPart || block instanceof Rock || block instanceof Floor || block instanceof StaticBlock)
            return;
        if(editLogs.containsKey(x + y * world.width())){
            editLogs.get(x + y * world.width()).add(new EditLog(player.name, block, rotation, action));
        }else{
            Array<EditLog> logs = new Array<>();
            logs.add(new EditLog(player.name, block, rotation, action));
            editLogs.put(x + y * world.width(), logs);
        }
    }

    /*
    public void rollbackWorld(int rollbackTimes) {
        for(IntMap.Entry<Array<EditLog>> editLog : editLogs.entries()) {
            int coords = editLog.key;
            Array<EditLog> logs = editLog.value;

            for(int i = 0; i < rollbackTimes; i++) {

                EditLog log = logs.get(logs.size - 1);

                int x = coords % world.width();
                int y = coords / world.width();
                Block result = log.block;
                int rotation = log.rotation;

                //TODO fix this mess, broken with 4.0

                if(log.action == EditLog.EditAction.PLACE) {
                   // Build.breakBlock(x, y, false, false);

                    Packets.BreakPacket packet = new Packets.BreakPacket();
                    packet.x = (short) x;
                    packet.y = (short) y;
                    packet.playerid = 0;

                    Net.send(packet, Net.SendMode.tcp);
                }
                else if(log.action == EditLog.EditAction.BREAK) {
                    //Build.placeBlock(x, y, result, rotation, false, false);

                    Packets.PlacePacket packet = new Packets.PlacePacket();
                    packet.x = (short) x;
                    packet.y = (short) y;
                    packet.rotation = (byte) rotation;
                    packet.playerid = 0;
                    //packet.block = result.id;

                    Net.send(packet, Net.SendMode.tcp);
                }

                logs.removeIndex(logs.size - 1);
                if(logs.size == 0) {
                    editLogs.remove(coords);
                    break;
                }
            }
        }
    }*/

    public boolean validateBreak(String id, String ip){
        if(!isAntiGrief() || isAdmin(id, ip)) return true;

        PlayerInfo info = getCreateInfo(id);

        if(info.lastBroken == null || info.lastBroken.length != Settings.getInt("antigrief-max")){
            info.lastBroken = new long[Settings.getInt("antigrief-max")];
        }

        long[] breaks = info.lastBroken;

        int shiftBy = 0;
        for(int i = 0; i < breaks.length && breaks[i] != 0; i++){
            if(TimeUtils.timeSinceMillis(breaks[i]) >= Settings.getInt("antigrief-cooldown")){
                shiftBy = i;
            }
        }

        for(int i = 0; i < breaks.length; i++){
            breaks[i] = (i + shiftBy >= breaks.length) ? 0 : breaks[i + shiftBy];
        }

        int remaining = 0;
        for(int i = 0; i < breaks.length; i++){
            if(breaks[i] == 0){
                remaining = breaks.length - i;
                break;
            }
        }

        if(remaining == 0) return false;

        breaks[breaks.length - remaining] = TimeUtils.millis();
        return true;
    }

    /**
     * Call when a player joins to update their information here.
     */
    public void updatePlayerJoined(String id, String ip, String name){
        PlayerInfo info = getCreateInfo(id);
        info.lastName = name;
        info.lastIP = ip;
        info.timesJoined++;
        if(!info.names.contains(name, false)) info.names.add(name);
        if(!info.ips.contains(ip, false)) info.ips.add(ip);
    }

    /**
     * Returns trace info by IP.
     */
    public TraceInfo getTraceByID(String uuid){
        if(!traceInfo.containsKey(uuid)) traceInfo.put(uuid, new TraceInfo(uuid));

        return traceInfo.get(uuid);
    }

    public void clearTraces(){
        traceInfo.clear();
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

        return true;
    }

    /**
     * Bans a player by UUID; returns whether this player was already banned.
     */
    public boolean banPlayerID(String id){
        if(playerInfo.containsKey(id) && playerInfo.get(id).banned)
            return false;

        getCreateInfo(id).banned = true;

        save();

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

        if(found) save();

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

        if(info.admin)
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

    public boolean isIPBanned(String ip){
        return bannedIPs.contains(ip, false) || (findByIP(ip) != null && findByIP(ip).banned);
    }

    public boolean isIDBanned(String uuid){
        return getCreateInfo(uuid).banned;
    }

    public boolean isAdmin(String id, String usip){
        PlayerInfo info = getCreateInfo(id);
        return info.admin && usip.equals(info.adminUsid);
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
        Settings.putJson("player-info", playerInfo);
        Settings.putJson("banned-ips", bannedIPs);
        Settings.save();
    }

    private void load(){
        playerInfo = Settings.getJson("player-info", ObjectMap.class);
        bannedIPs = Settings.getJson("banned-ips", Array.class);
    }

    public static class PlayerInfo{
        public String id;
        public String lastName = "<unknown>", lastIP = "<unknown>";
        public Array<String> ips = new Array<>();
        public Array<String> names = new Array<>();
        public String adminUsid;
        public int timesKicked;
        public int timesJoined;
        public int totalBlockPlaced;
        public int totalBlocksBroken;
        public boolean banned, admin;
        public long lastKicked; //last kicked timestamp

        public long[] lastBroken;

        PlayerInfo(String id){
            this.id = id;
        }

        private PlayerInfo(){
        }
    }

}
