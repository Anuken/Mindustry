package io.anuke.mindustry.server;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.collection.Array.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.Timer;
import io.anuke.arc.util.CommandHandler.*;
import io.anuke.arc.util.Timer.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.mod.Mods.*;
import io.anuke.mindustry.net.Administration.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.type.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import static io.anuke.arc.util.Log.*;
import static io.anuke.mindustry.Vars.*;

public class ServerControl implements ApplicationListener{
    private static final int roundExtraTime = 12;
    //in bytes: 512 kb is max
    private static final int maxLogLength = 1024 * 512;
    private static final int commandSocketPort = 6859;

    private final CommandHandler handler = new CommandHandler("");
    private final FileHandle logFolder = Core.settings.getDataDirectory().child("logs/");

    private FileHandle currentLogFile;
    private boolean inExtraRound;
    private Task lastTask;
    private Gamemode lastMode = Gamemode.survival;

    private Thread socketThread;
    private PrintWriter socketOutput;

    public ServerControl(String[] args){
        Core.settings.defaults(
            "shufflemode", "normal",
            "bans", "",
            "admins", "",
            "shuffle", true,
            "crashreport", false,
            "port", port,
            "logging", true,
            "socket", false
        );

        Log.setLogger(new LogHandler(){
            DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MM-dd-yyyy | HH:mm:ss");

            @Override
            public void debug(String text, Object... args){
                print("&lc&fb" + "[DEBG] " + text, args);
            }

            @Override
            public void info(String text, Object... args){
                print("&lg&fb" + "[INFO] " + text, args);
            }

            @Override
            public void err(String text, Object... args){
                print("&lr&fb" + "[ERR!] " + text, args);
            }

            @Override
            public void warn(String text, Object... args){
                print("&ly&fb" + "[WARN] " + text, args);
            }

            @Override
            public void print(String text, Object... args){
                String result = "[" + dateTime.format(LocalDateTime.now()) + "] " + format(text + "&fr", args);
                System.out.println(result);

                if(Core.settings.getBool("logging")){
                    logToFile("[" + dateTime.format(LocalDateTime.now()) + "] " + format(text + "&fr", false, args));
                }

                if(socketOutput != null){
                    try{
                        socketOutput.println(format(text + "&fr", false, args).replace("[DEBG] ", "").replace("[WARN] ", "").replace("[INFO] ", "").replace("[ERR!] ", ""));
                    }catch(Throwable e){
                        err("Error occurred logging to socket: {0}", e.getClass().getSimpleName());
                    }
                }
            }
        });

        Time.setDeltaProvider(() -> Core.graphics.getDeltaTime() * 60f);
        Effects.setScreenShakeProvider((a, b) -> {});
        Effects.setEffectProvider((a, b, c, d, e, f) -> {});

        registerCommands();

        Core.app.post(() -> {
            String[] commands = {};

            if(args.length > 0){
                commands = Strings.join(" ", args).split(",");
                info("&lmFound {0} command-line arguments to parse.", commands.length);
            }

            for(String s : commands){
                CommandResponse response = handler.handleMessage(s);
                if(response.type != ResponseType.valid){
                    err("Invalid command argument sent: '{0}': {1}", s, response.type.name());
                    err("Argument usage: &lc<command-1> <command1-args...>,<command-2> <command-2-args2...>");
                    System.exit(1);
                }
            }
        });

        customMapDirectory.mkdirs();

        Thread thread = new Thread(this::readCommands, "Server Controls");
        thread.setDaemon(true);
        thread.start();

        if(Version.build == -1){
            warn("&lyYour server is running a custom build, which means that client checking is disabled.");
            warn("&lyIt is highly advised to specify which version you're using by building with gradle args &lc-Pbuildversion=&lm<build>&ly.");
        }

        Events.on(GameOverEvent.class, event -> {
            if(inExtraRound) return;
            info("Game over!");

            if(Core.settings.getBool("shuffle")){
                if(maps.all().size > 0){
                    Array<Map> maps = Array.with(Vars.maps.customMaps().size == 0 ? Vars.maps.defaultMaps() : Vars.maps.customMaps());
                    maps.shuffle();

                    Map previous = world.getMap();
                    Map map = maps.find(m -> m != previous || maps.size == 1);

                    if(map != null){

                        Call.onInfoMessage((state.rules.pvp
                        ? "[YELLOW]The " + event.winner.name() + " team is victorious![]" : "[SCARLET]Game over![]")
                        + "\nNext selected map:[accent] " + map.name() + "[]"
                        + (map.tags.containsKey("author") && !map.tags.get("author").trim().isEmpty() ? " by[accent] " + map.author() + "[]" : "") + "." +
                        "\nNew game begins in " + roundExtraTime + "[] seconds.");

                        info("Selected next map to be {0}.", map.name());

                        play(true, () -> world.loadMap(map, map.applyRules(lastMode)));
                    }else{
                        Log.err("No suitable map found.");
                    }
                }
            }else{
                netServer.kickAll(KickReason.gameover);
                state.set(State.menu);
                net.closeServer();
            }
        });

        if(!mods.all().isEmpty()){
            info("&lc{0} mods loaded.", mods.all().size);
        }

        info("&lcServer loaded. Type &ly'help'&lc for help.");
        System.out.print("> ");

        if(Core.settings.getBool("socket")){
            toggleSocket(true);
        }
    }

    private void registerCommands(){
        handler.register("help", "Displays this command list.", arg -> {
            info("Commands:");
            for(Command command : handler.getCommandList()){
                info("   &y" + command.text + (command.paramText.isEmpty() ? "" : " ") + command.paramText + " - &lm" + command.description);
            }
        });

        handler.register("version", "Displays server version info.", arg -> {
            info("&lmVersion: &lyMindustry {0}-{1} {2} / build {3}", Version.number, Version.modifier, Version.type, Version.build);
            info("&lmJava Version: &ly{0}", System.getProperty("java.version"));
        });

        handler.register("exit", "Exit the server application.", arg -> {
            info("Shutting down server.");
            net.dispose();
            Core.app.exit();
        });

        handler.register("stop", "Stop hosting the server.", arg -> {
            net.closeServer();
            if(lastTask != null) lastTask.cancel();
            state.set(State.menu);
            info("Stopped server.");
        });

        handler.register("host", "<mapname> [mode]", "Open the server with a specific map.", arg -> {
            if(state.is(State.playing)){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            if(lastTask != null) lastTask.cancel();

            Map result = maps.all().find(map -> map.name().equalsIgnoreCase(arg[0].replace('_', ' ')) || map.name().equalsIgnoreCase(arg[0]));

            if(result == null){
                err("No map with name &y'{0}'&lr found.", arg[0]);
                return;
            }

            Gamemode preset = Gamemode.survival;

            if(arg.length > 1){
                try{
                    preset = Gamemode.valueOf(arg[1]);
                }catch(IllegalArgumentException e){
                    err("No gamemode '{0}' found.", arg[1]);
                    return;
                }
            }

            info("Loading map...");

            logic.reset();
            lastMode = preset;
            try{
                world.loadMap(result,  result.applyRules(lastMode));
                state.rules = result.applyRules(preset);
                logic.play();

                info("Map loaded.");

                host();
            }catch(MapException e){
                Log.err(e.map.name() + ": " + e.getMessage());
            }
        });

        handler.register("port", "[port]", "Sets or displays the port for hosting the server.", arg -> {
            if(arg.length == 0){
                info("&lyPort: &lc{0}", Core.settings.getInt("port"));
            }else{
                int port = Strings.parseInt(arg[0]);
                if(port < 0 || port > 65535){
                    err("Port must be a number between 0 and 65535.");
                    return;
                }
                info("&lyPort set to {0}.", port);
                Core.settings.put("port", port);
                Core.settings.save();
            }
        });

        handler.register("maps", "Display all available maps.", arg -> {
            if(!maps.all().isEmpty()){
                info("Maps:");
                for(Map map : maps.all()){
                    info("  &ly{0}: &lb&fi{1} / {2}x{3}", map.name(), map.custom ? "Custom" : "Default", map.width, map.height);
                }
            }else{
                info("No maps found.");
            }
            info("&lyMap directory: &lb&fi{0}", customMapDirectory.file().getAbsoluteFile().toString());
        });

        handler.register("reloadmaps", "Reload all maps from disk.", arg -> {
            int beforeMaps = maps.all().size;
            maps.reload();
            if(maps.all().size > beforeMaps){
                info("&lc{0}&ly new map(s) found and reloaded.", maps.all().size - beforeMaps);
            }else{
                info("&lyMaps reloaded.");
            }
        });

        handler.register("status", "Display server status.", arg -> {
            if(state.is(State.menu)){
                info("Status: &rserver closed");
            }else{
                info("Status:");
                info("  &lyPlaying on map &fi{0}&fb &lb/&ly Wave {1}", Strings.capitalize(world.getMap().name()), state.wave);

                if(state.rules.waves){
                    info("&ly  {0} enemies.", unitGroups[Team.crux.ordinal()].size());
                }else{
                    info("&ly  {0} seconds until next wave.", (int)(state.wavetime / 60));
                }

                info("  &ly{0} FPS, {1} MB used.", (int)(60f / Time.delta()), Core.app.getJavaHeap() / 1024 / 1024);

                if(playerGroup.size() > 0){
                    info("  &lyPlayers: {0}", playerGroup.size());
                    for(Player p : playerGroup.all()){
                        info("    &y{0} / {1}", p.name, p.uuid);
                    }
                }else{
                    info("  &lyNo players connected.");
                }
            }
        });

        handler.register("mods", "Display all loaded mods.", arg -> {
            if(!mods.all().isEmpty()){
                info("Mods:");
                for(LoadedMod mod : mods.all()){
                    info("  &ly{0} &lcv{1}", mod.meta.name, mod.meta.version);
                }
            }else{
                info("No mods found.");
            }
            info("&lyMod directory: &lb&fi{0}", modDirectory.file().getAbsoluteFile().toString());
        });

        handler.register("mod", "<name...>", "Display information about a loaded plugin.", arg -> {
            LoadedMod mod = mods.all().find(p -> p.meta.name.equalsIgnoreCase(arg[0]));
            if(mod != null){
                info("Name: &ly{0}", mod.meta.name);
                info("Version: &ly{0}", mod.meta.version);
                info("Author: &ly{0}", mod.meta.author);
                info("Path: &ly{0}", mod.file.path());
                info("Description: &ly{0}", mod.meta.description);
            }else{
                info("No mod with name &ly'{0}'&lg found.");
            }
        });

        handler.register("say", "<message...>", "Send a message to all players.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting. Host a game first.");
                return;
            }

            Call.sendMessage("[scarlet][[Server]:[] " + arg[0]);

            info("&lyServer: &lb{0}", arg[0]);
        });

        handler.register("difficulty", "<difficulty>", "Set game difficulty.", arg -> {
            try{
                state.rules.waveSpacing = Difficulty.valueOf(arg[0]).waveTime * 60 * 60 * 2;
                info("Difficulty set to '{0}'.", arg[0]);
            }catch(IllegalArgumentException e){
                err("No difficulty with name '{0}' found.", arg[0]);
            }
        });

        handler.register("fillitems", "[team]", "Fill the core with items.", arg -> {
            if(!state.is(State.playing)){
                err("Not playing. Host first.");
                return;
            }

            try{
                Team team = arg.length == 0 ? Team.sharded : Team.valueOf(arg[0]);

                if(state.teams.get(team).cores.isEmpty()){
                    err("That team has no cores.");
                    return;
                }

                for(Item item : content.items()){
                    if(item.type == ItemType.material){
                        state.teams.get(team).cores.first().entity.items.set(item, state.teams.get(team).cores.first().block().itemCapacity);
                    }
                }

                info("Core filled.");
            }catch(IllegalArgumentException ignored){
                err("No such team exists.");
            }
        });

        handler.register("name", "[name...]", "Change the server display name.", arg -> {
            if(arg.length == 0){
                info("Server name is currently &lc'{0}'.", Core.settings.getString("servername"));
                return;
            }
            Core.settings.put("servername", arg[0]);
            Core.settings.save();
            info("Server name is now &lc'{0}'.", arg[0]);
        });

        handler.register("playerlimit", "[off/somenumber]", "Set the server player limit.", arg -> {
            if(arg.length == 0){
                info("Player limit is currently &lc{0}.", netServer.admins.getPlayerLimit() == 0 ? "off" : netServer.admins.getPlayerLimit());
                return;
            }
            if(arg[0].equals("off")){
                netServer.admins.setPlayerLimit(0);
                info("Player limit disabled.");
                return;
            }

            if(Strings.canParsePostiveInt(arg[0]) && Strings.parseInt(arg[0]) > 0){
                int lim = Strings.parseInt(arg[0]);
                netServer.admins.setPlayerLimit(lim);
                info("Player limit is now &lc{0}.", lim);
            }else{
                err("Limit must be a number above 0.");
            }
        });

        handler.register("whitelist", "[on/off...]", "Enable/disable whitelisting.", arg -> {
            if(arg.length == 0){
                info("Whitelist is currently &lc{0}.", netServer.admins.isWhitelistEnabled() ? "on" : "off");
                return;
            }
            boolean on = arg[0].equalsIgnoreCase("on");
            netServer.admins.setWhitelist(on);
            info("Whitelist is now &lc{0}.", on ? "on" : "off");
        });

        handler.register("whitelisted", "List the entire whitelist.", arg -> {
            if(netServer.admins.getWhitelisted().isEmpty()){
                info("&lyNo whitelisted players found.");
                return;
            }

            info("&lyWhitelist:");
            netServer.admins.getWhitelisted().each(p -> Log.info("- &ly{0}", p.lastName));
        });

        handler.register("whitelist-add", "<ID>", "Add a player to the whitelist by ID.", arg -> {
            PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);
            if(info == null){
                err("Player ID not found. You must use the ID displayed when a player joins a server.");
                return;
            }

            netServer.admins.whitelist(arg[0]);
            info("Player &ly'{0}'&lg has been whitelisted.", info.lastName);
        });

        handler.register("whitelist-remove", "<ID>", "Remove a player to the whitelist by ID.", arg -> {
            PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);
            if(info == null){
                err("Player ID not found. You must use the ID displayed when a player joins a server.");
                return;
            }

            netServer.admins.unwhitelist(arg[0]);
            info("Player &ly'{0}'&lg has been un-whitelisted.", info.lastName);
        });

        handler.register("crashreport", "<on/off>", "Disables or enables automatic crash reporting", arg -> {
            boolean value = arg[0].equalsIgnoreCase("on");
            Core.settings.put("crashreport", value);
            Core.settings.save();
            info("Crash reporting is now {0}.", value ? "on" : "off");
        });

        handler.register("logging", "<on/off>", "Disables or enables server logs", arg -> {
            boolean value = arg[0].equalsIgnoreCase("on");
            Core.settings.put("logging", value);
            Core.settings.save();
            info("Logging is now {0}.", value ? "on" : "off");
        });

        handler.register("strict", "<on/off>", "Disables or enables strict mode", arg -> {
            boolean value = arg[0].equalsIgnoreCase("on");
            netServer.admins.setStrict(value);
            info("Strict mode is now {0}.", netServer.admins.getStrict() ? "on" : "off");
        });

        handler.register("socketinput", "[on/off]", "Disables or enables a local TCP socket at port "+commandSocketPort+" to recieve commands from other applications", arg -> {
            if(arg.length == 0){
                info("Socket input is currently &lc{0}.", Core.settings.getBool("socket") ? "on" : "off");
                return;
            }

            boolean value = arg[0].equalsIgnoreCase("on");
            toggleSocket(value);
            Core.settings.put("socket", value);
            Core.settings.save();
            info("Socket input is now &lc{0}.", value ? "on" : "off");
        });

        handler.register("allow-custom-clients", "[on/off]", "Allow or disallow custom clients.", arg -> {
            if(arg.length == 0){
                info("Custom clients are currently &lc{0}.", netServer.admins.allowsCustomClients() ? "allowed" : "disallowed");
                return;
            }

            String s = arg[0];
            if(s.equalsIgnoreCase("on")){
                netServer.admins.setCustomClients(true);
                info("Custom clients enabled.");
            }else if(s.equalsIgnoreCase("off")){
                netServer.admins.setCustomClients(false);
                info("Custom clients disabled.");
            }else{
                err("Incorrect command usage.");
            }
        });

        handler.register("shuffle", "<on/off>", "Set map shuffling.", arg -> {
            if(!arg[0].equals("on") && !arg[0].equals("off")){
                err("Invalid shuffle mode.");
                return;
            }
            Core.settings.put("shuffle", arg[0].equals("on"));
            Core.settings.save();
            info("Shuffle mode set to '{0}'.", arg[0]);
        });

        handler.register("kick", "<username...>", "Kick a person by name.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting a game yet. Calm down.");
                return;
            }

            Player target = playerGroup.find(p -> p.name.equals(arg[0]));

            if(target != null){
                Call.sendMessage("[scarlet] " + target.name + "[scarlet] has been kicked by the server.");
                target.con.kick(KickReason.kick);
                info("It is done.");
            }else{
                info("Nobody with that name could be found...");
            }
        });

        handler.register("ban", "<type-id/name/ip> <username/IP/ID...>", "Ban a person.", arg -> {
            if(arg[0].equals("id")){
                netServer.admins.banPlayerID(arg[1]);
                info("Banned.");
            }else if(arg[0].equals("name")){
                Player target = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[1]));
                if(target != null){
                    netServer.admins.banPlayer(target.uuid);
                    info("Banned.");
                }else{
                    err("No matches found.");
                }
            }else if(arg[0].equals("ip")){
                netServer.admins.banPlayerIP(arg[1]);
                info("Banned.");
            }else{
                err("Invalid type.");
            }

            for(Player player : playerGroup.all()){
                if(netServer.admins.isIDBanned(player.uuid)){
                    Call.sendMessage("[scarlet] " + player.name + " has been banned.");
                    player.con.kick(KickReason.banned);
                }
            }
        });

        handler.register("bans", "List all banned IPs and IDs.", arg -> {
            Array<PlayerInfo> bans = netServer.admins.getBanned();

            if(bans.size == 0){
                info("No ID-banned players have been found.");
            }else{
                info("&lyBanned players [ID]:");
                for(PlayerInfo info : bans){
                    info(" &ly {0} / Last known name: '{1}'", info.id, info.lastName);
                }
            }

            Array<String> ipbans = netServer.admins.getBannedIPs();

            if(ipbans.size == 0){
                info("No IP-banned players have been found.");
            }else{
                info("&lmBanned players [IP]:");
                for(String string : ipbans){
                    PlayerInfo info = netServer.admins.findByIP(string);
                    if(info != null){
                        info(" &lm '{0}' / Last known name: '{1}' / ID: '{2}'", string, info.lastName, info.id);
                    }else{
                        info(" &lm '{0}' (No known name or info)", string);
                    }
                }
            }
        });

        handler.register("unban", "<ip/ID>", "Completely unban a person by IP or ID.", arg -> {
            if(arg[0].contains(".")){
                if(netServer.admins.unbanPlayerIP(arg[0])){
                    info("Unbanned player by IP: {0}.", arg[0]);
                }else{
                    err("That IP is not banned!");
                }
            }else{
                if(netServer.admins.unbanPlayerID(arg[0])){
                    info("Unbanned player by ID: {0}.", arg[0]);
                }else{
                    err("That ID is not banned!");
                }
            }
        });

        handler.register("admin", "<username...>", "Make an online user admin", arg -> {
            if(!state.is(State.playing)){
                err("Open the server first.");
                return;
            }

            Player target = playerGroup.find(p -> p.name.equals(arg[0]));

            if(target != null){
                netServer.admins.adminPlayer(target.uuid, target.usid);
                target.isAdmin = true;
                info("Admin-ed player: {0}", arg[0]);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("unadmin", "<username...>", "Removes admin status from an online player", arg -> {
            if(!state.is(State.playing)){
                err("Open the server first.");
                return;
            }

            Player target = playerGroup.find(p -> p.name.equals(arg[0]));

            if(target != null){
                netServer.admins.unAdminPlayer(target.uuid);
                target.isAdmin = false;
                info("Un-admin-ed player: {0}", arg[0]);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("admins", "List all admins.", arg -> {
            Array<PlayerInfo> admins = netServer.admins.getAdmins();

            if(admins.size == 0){
                info("No admins have been found.");
            }else{
                info("&lyAdmins:");
                for(PlayerInfo info : admins){
                    info(" &lm {0} /  ID: '{1}' / IP: '{2}'", info.lastName, info.id, info.lastIP);
                }
            }
        });

        handler.register("runwave", "Trigger the next wave.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting. Host a game first.");
            }else{
                logic.runWave();
                info("Wave spawned.");
            }
        });

        handler.register("load", "<slot>", "Load a save from a slot.", arg -> {
            if(state.is(State.playing)){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }else if(!Strings.canParseInt(arg[0])){
                err("Invalid save slot '{0}'.", arg[0]);
                return;
            }

            int slot = Strings.parseInt(arg[0]);

            if(!SaveIO.isSaveValid(slot)){
                err("No (valid) save data found for slot.");
                return;
            }

            Core.app.post(() -> {
                try{
                    SaveIO.loadFromSlot(slot);
                    state.rules.zone = null;
                }catch(Throwable t){
                    err("Failed to load save. Outdated or corrupt file.");
                }
                info("Save loaded.");
                host();
                state.set(State.playing);
            });
        });

        handler.register("save", "<slot>", "Save game state to a slot.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting. Host a game first.");
                return;
            }else if(!Strings.canParseInt(arg[0])){
                err("Invalid save slot '{0}'.", arg[0]);
                return;
            }

            Core.app.post(() -> {
                int slot = Strings.parseInt(arg[0]);
                SaveIO.saveToSlot(slot);
                info("Saved to slot {0}.", slot);
            });
        });

        handler.register("gameover", "Force a game over.", arg -> {
            if(state.is(State.menu)){
                info("Not playing a map.");
                return;
            }

            info("&lyCore destroyed.");
            inExtraRound = false;
            Events.fire(new GameOverEvent(Team.crux));
        });

        handler.register("info", "<IP/UUID/name...>", "Find player info(s). Can optionally check for all names or IPs a player has had.", arg -> {

            ObjectSet<PlayerInfo> infos = netServer.admins.findByName(arg[0]);

            if(infos.size > 0){
                info("&lgPlayers found: {0}", infos.size);

                int i = 0;
                for(PlayerInfo info : infos){
                    info("&lc[{0}] Trace info for player '{1}' / UUID {2}", i++, info.lastName, info.id);
                    info("  &lyall names used: {0}", info.names);
                    info("  &lyIP: {0}", info.lastIP);
                    info("  &lyall IPs used: {0}", info.ips);
                    info("  &lytimes joined: {0}", info.timesJoined);
                    info("  &lytimes kicked: {0}", info.timesKicked);
                }
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("gc", "Trigger a grabage collection. Testing only.", arg -> {
            int pre = (int)(Core.app.getJavaHeap() / 1024 / 1024);
            System.gc();
            int post = (int)(Core.app.getJavaHeap() / 1024 / 1024);
            info("&ly{0}&lg MB collected. Memory usage now at &ly{1}&lg MB.", pre - post, post);
        });

        mods.each(p -> p.registerServerCommands(handler));
        //TODO
        //plugins.each(p -> p.registerClientCommands(netServer.clientCommands));
    }

    private void readCommands(){

        Scanner scan = new Scanner(System.in);
        while(scan.hasNext()){
            String line = scan.nextLine();
            Core.app.post(() -> handleCommandString(line));
        }
    }

    private void handleCommandString(String line){
        CommandResponse response = handler.handleMessage(line);

        if(response.type == ResponseType.unknownCommand){

            int minDst = 0;
            Command closest = null;

            for(Command command : handler.getCommandList()){
                int dst = Strings.levenshtein(command.text, response.runCommand);
                if(dst < 3 && (closest == null || dst < minDst)){
                    minDst = dst;
                    closest = command;
                }
            }

            if(closest != null){
                err("Command not found. Did you mean \"" + closest.text + "\"?");
            }else{
                err("Invalid command. Type 'help' for help.");
            }
        }else if(response.type == ResponseType.fewArguments){
            err("Too few command arguments. Usage: " + response.command.text + " " + response.command.paramText);
        }else if(response.type == ResponseType.manyArguments){
            err("Too many command arguments. Usage: " + response.command.text + " " + response.command.paramText);
        }

        System.out.print("> ");
    }

    private void play(boolean wait, Runnable run){
        inExtraRound = true;
        Runnable r = () -> {
            Array<Player> players = new Array<>();
            for(Player p : playerGroup.all()){
                players.add(p);
                p.setDead(true);
            }
            
            logic.reset();

            Call.onWorldDataBegin();
            run.run();
            logic.play();
            state.rules = world.getMap().applyRules(lastMode);

            for(Player p : players){
                if(p.con == null) continue;

                p.reset();
                if(state.rules.pvp){
                    p.setTeam(netServer.assignTeam(p, new ArrayIterable<>(players)));
                }
                netServer.sendWorldData(p);
            }
            inExtraRound = false;
        };

        if(wait){
            lastTask = new Task(){
                @Override
                public void run(){
                    try{
                        r.run();
                    }catch(MapException e){
                        Log.err(e.map.name() + ": " + e.getMessage());
                        net.closeServer();
                    }
                }
            };

            Timer.schedule(lastTask, roundExtraTime);
        }else{
            r.run();
        }
    }

    private void host(){
        try{
            net.host(Core.settings.getInt("port"));
            info("&lcOpened a server on port {0}.", Core.settings.getInt("port"));
        }catch(BindException e){
            Log.err("Unable to host: Port already in use! Make sure no other servers are running on the same port in your network.");
            state.set(State.menu);
        }catch(IOException e){
            err(e);
            state.set(State.menu);
        }
    }

    private void logToFile(String text){
        if(currentLogFile != null && currentLogFile.length() > maxLogLength){
            String date = DateTimeFormatter.ofPattern("MM-dd-yyyy | HH:mm:ss").format(LocalDateTime.now());
            currentLogFile.writeString("[End of log file. Date: " + date + "]\n", true);
            currentLogFile = null;
        }

        if(currentLogFile == null){
            int i = 0;
            while(logFolder.child("log-" + i + ".txt").length() >= maxLogLength){
                i++;
            }

            currentLogFile = logFolder.child("log-" + i + ".txt");
        }

        currentLogFile.writeString(text + "\n", true);
    }

    private void toggleSocket(boolean on){
        if(on && socketThread == null){
            socketThread = new Thread(() -> {
                try{
                    try(ServerSocket socket = new ServerSocket()){
                        socket.bind(new InetSocketAddress("localhost", commandSocketPort));
                        while(true){
                            Socket client = socket.accept();
                            info("&lmRecieved command socket connection: &lb{0}", socket.getLocalSocketAddress());
                            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            socketOutput = new PrintWriter(client.getOutputStream(), true);
                            String line;
                            while(client.isConnected() && (line = in.readLine()) != null){
                                String result = line;
                                Core.app.post(() -> handleCommandString(result));
                            }
                            info("&lmLost command socket connection: &lb{0}", socket.getLocalSocketAddress());
                            socketOutput = null;
                        }
                    }
                }catch(BindException b){
                    err("Command input socket already in use. Is another instance of the server running?");
                }catch(IOException e){
                    err("Terminating socket server.");
                    e.printStackTrace();
                }
            });
            socketThread.setDaemon(true);
            socketThread.start();
        }else if(socketThread != null){
            socketThread.interrupt();
            socketThread = null;
            socketOutput = null;
        }
    }
}
