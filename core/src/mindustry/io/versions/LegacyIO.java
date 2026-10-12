package mindustry.io.versions;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.Maps.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.ui.dialogs.JoinDialog.*;

import java.io.*;
import java.util.zip.*;

public class LegacyIO{
    /** Maps old unit names to new ones. */
    public static final StringMap unitMap = StringMap.of(
    "titan", "mace",
    "chaos-array", "scepter",
    "eradicator", "reign",
    "eruptor", "atrax",
    "wraith", "flare",
    "ghoul", "horizon",
    "revenant", "zenith",
    "lich", "antumbra",
    "reaper", "eclipse",
    "draug", "mono",
    "phantom", "poly",
    "spirit", "poly"
    );

    /** Migrates all legacy server data out of settings.bin into standalone files. Does nothing if settings.bin does not exist. */
    @SuppressWarnings("unchecked")
    public static void migrateServerSettings(){
        Fi dataDir = Core.settings.getDataDirectory();
        Fi settingsFile = dataDir.child("settings.bin");
        if(!settingsFile.exists()) return;

        Log.info("Migrating legacy settings.bin...");

        ObjectMap<String, Object> values;
        try{
            values = readSettingsFile(settingsFile);
        }catch(Throwable t){
            throw new RuntimeException("Failed to read settings.bin! This is non-recoverable, restore it from settings_backup.bin or remove it before starting the server again.", t);
        }

        try{
            Fi configFile = dataDir.child("config.hjson");
            if(!configFile.exists()){
                FileIO.atomicWrite(configFile, tmp -> JsonIO.write(readLegacyConfig(values), Jformat.hjson, tmp));
            }

            Fi rulesFile = dataDir.child("rules.hjson");
            if(values.containsKey("globalrules")){
                Jval base = Jval.newObject();
                if(rulesFile.exists()) base.asObject().putAll(Jval.read(rulesFile.readString()).asObject());
                base.asObject().putAll(Jval.read((String)values.get("globalrules")).asObject());
                FileIO.atomicWrite(rulesFile, tmp -> tmp.writeString(base.toString(Jformat.hjson)));
            }

            migrateLines(values, "ip-bans", dataDir.child("bans/ips.txt"));
            migrateLines(values, "banned-subnets", dataDir.child("bans/subnet.txt"));
            migrateLines(values, "banned-names", dataDir.child("bans/names.txt"));
            migrateLines(values, "whitelist-ids", dataDir.child("whitelist.txt"));

            Fi playersFile = dataDir.child("players.dat");
            if(!playersFile.exists() && values.get("player-data") instanceof byte[] bytes){
                ObjectMap<String, PlayerInfo> players = JsonIO.readBytes(ObjectMap.class, null, new DataInputStream(new ByteArrayInputStream(bytes)));
                FileIO.atomicWrite(playersFile, tmp -> {
                    try(DataOutputStream out = new DataOutputStream(new FastDeflaterOutputStream(tmp.write(false, 8192)))){
                        JsonIO.writeBytes(players, PlayerInfo.class, out);
                    }
                });
            }
        }catch(Throwable t){
            throw new RuntimeException("Failed to migrate settings.bin! This is non-recoverable, settings.bin has been left in place.", t);
        }

        Fi migrated = dataDir.child("migrated/settings.bin");
        migrated.parent().mkdirs();
        FileIO.swap(settingsFile, migrated);

        //no reason to pollute main directory with this; settings_backups still exists just in case
        dataDir.child("settings_backup.bin").delete();

        Log.info("Legacy settings migrated; the old file was moved to @.", migrated.path());
    }

    @SuppressWarnings("unchecked")
    private static void migrateLines(ObjectMap<String, Object> values, String key, Fi file) throws IOException{
        if(file.exists() || !(values.get(key) instanceof byte[] bytes)) return;

        Seq<String> lines = JsonIO.readBytes(Seq.class, String.class, new DataInputStream(new ByteArrayInputStream(bytes)));
        FileIO.atomicWrite(file, tmp -> FileIO.writeLines(lines, tmp));
    }

    /** Reads settings.bin directly into a map, without going through Settings. */
    private static ObjectMap<String, Object> readSettingsFile(Fi file) throws IOException{
        byte[] header = new byte[2];
        file.readBytes(header, 0, 2);
        boolean compressed = header[0] == (byte)0x78 && (header[1] == (byte)0x01 || header[1] == (byte)0x5E || header[1] == (byte)0x9c || header[1] == (byte)0xda);

        ObjectMap<String, Object> values = new ObjectMap<>();

        try(DataInputStream stream = new DataInputStream(compressed ? new InflaterInputStream(file.read(8192)) : file.read(8192))){
            int amount = stream.readInt();
            if(amount == 0) return values; //0 values are fine

            for(int i = 0; i < amount; i++){
                String key = stream.readUTF();
                byte type = stream.readByte();

                switch(type){
                    case 0 -> values.put(key, stream.readBoolean());
                    case 1 -> values.put(key, stream.readInt());
                    case 2 -> values.put(key, stream.readLong());
                    case 3 -> values.put(key, stream.readFloat());
                    case 4 -> values.put(key, stream.readUTF());
                    case 5 -> {
                        byte[] bytes = new byte[stream.readInt()];
                        stream.readFully(bytes);
                        values.put(key, bytes);
                    }
                    default -> throw new IOException("Unknown key type: " + type);
                }
            }

            int end = stream.read();
            if(end != -1) throw new IOException("Trailing settings data; expected EOF, but got: " + end);
        }

        return values;
    }

    private static String str(ObjectMap<String, Object> v, String key, String def){
        return v.get(key) instanceof String s ? s : def;
    }

    private static int num(ObjectMap<String, Object> v, String key, int def){
        return v.get(key) instanceof Integer i ? i : def;
    }

    private static boolean bool(ObjectMap<String, Object> v, String key, boolean def){
        return v.get(key) instanceof Boolean b ? b : def;
    }

    /** Builds a ServerConfig from the legacy (pre-config.hjson) settings keys. */
    private static ServerConfig readLegacyConfig(ObjectMap<String, Object> v){
        ServerConfig c = new ServerConfig();

        c.name = str(v, "servername", c.name);
        c.desc = str(v, "desc", c.desc);
        c.port = num(v, "port", c.port);
        c.playerLimit = num(v, "playerlimit", c.playerLimit);
        c.autoUpdate = bool(v, "autoUpdate", c.autoUpdate);
        c.showConnectMessages = bool(v, "showConnectMessages", c.showConnectMessages);
        c.enableVotekick = bool(v, "enableVotekick", c.enableVotekick);
        c.startCommands = str(v, "startCommands", c.startCommands);
        c.logging = bool(v, "logging", c.logging);
        c.strict = bool(v, "strict", c.strict);
        c.antiSpam = bool(v, "antiSpam", c.antiSpam);
        c.interactRateWindow = num(v, "interactRateWindow", c.interactRateWindow);
        c.interactRateLimit = num(v, "interactRateLimit", c.interactRateLimit);
        c.interactRateKick = num(v, "interactRateKick", c.interactRateKick);
        c.messageRateLimit = num(v, "messageRateLimit", c.messageRateLimit);
        c.messageSpamKick = num(v, "messageSpamKick", c.messageSpamKick);
        c.packetSpamLimit = num(v, "packetSpamLimit", c.packetSpamLimit);
        c.uuidChangeLimit = num(v, "uuidChangeLimit", c.uuidChangeLimit);
        c.uuidChangeTimePeriod = num(v, "uuidChangeTimePeriod", c.uuidChangeTimePeriod);
        c.chatSpamLimit = num(v, "chatSpamLimit", c.chatSpamLimit);
        c.socketInput = bool(v, "socket", c.socketInput);
        c.socketInputPort = num(v, "socketInputPort", c.socketInputPort);
        c.socketInputAddress = str(v, "socketInputAddress", c.socketInputAddress);
        c.allowCustomClients = bool(v, "allow-custom", c.allowCustomClients);
        c.whitelist = bool(v, "whitelist", c.whitelist);
        c.motd = str(v, "motd", c.motd);
        c.autosave = bool(v, "autosave", c.autosave);
        c.autosaveAmount = num(v, "autosaveAmount", c.autosaveAmount);
        c.autosaveSpacing = num(v, "autosaveSpacing", c.autosaveSpacing);
        c.debug = bool(v, "debug", c.debug);
        c.snapshotInterval = num(v, "snapshotInterval", c.snapshotInterval);
        c.autoPause = bool(v, "autoPause", c.autoPause);
        c.roundExtraTime = num(v, "roundExtraTime", c.roundExtraTime);
        c.maxLogLength = num(v, "maxLogLength", c.maxLogLength);
        c.logCommands = bool(v, "logCommands", c.logCommands);

        try{
            c.shuffleMode = ShuffleMode.valueOf(str(v, "shufflemode", c.shuffleMode.name()));
            c.lastGamemode = Gamemode.valueOf(str(v, "lastServerMode", c.lastGamemode.name()));
        }catch(Exception e){
            Log.err("Invalid legacy config server value", e);
        }

        return c;
    }

    public static Seq<Server> readServers(){
        Seq<Server> arr = new Seq<>();

        try{
            byte[] bytes = Core.settings.getBytes("server-list");
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes));

            int length = stream.readInt();
            if(length > 0){
                //name of type, irrelevant
                stream.readUTF();

                for(int i = 0; i < length; i++){
                    Server server = new Server();
                    server.ip = stream.readUTF();
                    server.port = stream.readInt();
                    arr.add(server);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return arr;
    }
}
