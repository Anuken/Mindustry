package mindustry.server;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.struct.Array.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.Timer;
import arc.util.CommandHandler.*;
import arc.util.Timer.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonValue.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.maps.Maps.*;
import mindustry.mod.Mods.*;
import mindustry.net.Administration.*;
import mindustry.net.Packets.*;
import mindustry.type.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;

public class ServerControl implements ApplicationListener{
    private static final int roundExtraTime = 12;
    private static final int maxLogLength = 1024 * 512;

    protected static String[] tags = {"&lc&fb[D]", "&lg&fb[I]", "&ly&fb[W]", "&lr&fb[E]", ""};
    protected static DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MM-dd-yyyy | HH:mm:ss");

    private final CommandHandler handler = new CommandHandler("");
    private final Fi logFolder = Core.settings.getDataDirectory().child("logs/");

    private Fi currentLogFile;
    private boolean inExtraRound;
    private Task lastTask;
    private Gamemode lastMode = Gamemode.survival;
    private @Nullable Map nextMapOverride;

    private Thread socketThread;
    private ServerSocket serverSocket;
    private PrintWriter socketOutput;

    public ServerControl(String[] args){
        Core.settings.defaults(
            "shufflemode", "normal",
            "bans", "",
            "admins", "",
            "shufflemode", "custom",
            "globalrules", "{reactorExplosions: false}"
        );

        Log.setLogger((level, text) -> {
            String result = "[" + dateTime.format(LocalDateTime.now()) + "] " + format(tags[level.ordinal()] + " " + text + "&fr");
            System.out.println(result);

            if(Config.logging.bool()){
                logToFile("[" + dateTime.format(LocalDateTime.now()) + "] " + formatColors(tags[level.ordinal()] + " " + text + "&fr", false));
            }

            if(socketOutput != null){
                try{
                    socketOutput.println(formatColors(text + "&fr", false));
                }catch(Throwable e){
                    err("Error occurred logging to socket: {0}", e.getClass().getSimpleName());
                }
            }
        });

        Time.setDeltaProvider(() -> Core.graphics.getDeltaTime() * 60f);
        Effects.setScreenShakeProvider((a, b) -> {});
        Effects.setEffectProvider((a, b, c, d, e, f) -> {});

        registerCommands();

        Core.app.post(() -> {
            Array<String> commands = new Array<>();

            if(args.length > 0){
                commands.addAll(Strings.join(" ", args).split(","));
                info("&lmFound {0} command-line arguments to parse.", commands.size);
            }

            if(!Config.startCommands.string().isEmpty()){
                String[] startup = Strings.join(" ", Config.startCommands.string()).split(",");
                info("&lmFound {0} startup commands.", startup.length);
                commands.addAll(startup);
            }

            for(String s : commands){
                CommandResponse response = handler.handleMessage(s);
                if(response.type != ResponseType.valid){
                    err("Invalid command argument sent: '{0}': {1}", s, response.type.name());
                    err("Argument usage: &lc<command-1> <command1-args...>,<command-2> <command-2-args2...>");
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

        //set up default shuffle mode
        try{
            maps.setShuffleMode(ShuffleMode.valueOf(Core.settings.getString("shufflemode")));
        }catch(Exception e){
            maps.setShuffleMode(ShuffleMode.all);
        }

        Events.on(GameOverEvent.class, event -> {
            if(inExtraRound) return;
            if(state.rules.waves){
                info("&lcGame over! Reached wave &ly{0}&lc with &ly{1}&lc players online on map &ly{2}&lc.", state.wave, playerGroup.size(), Strings.capitalize(world.getMap().name()));
            }else{
                info("&lcGame over! Team &ly{0}&lc is victorious with &ly{1}&lc players online on map &ly{2}&lc.", event.winner.name, playerGroup.size(), Strings.capitalize(world.getMap().name()));
            }

            //set next map to be played
            Map map = nextMapOverride != null ? nextMapOverride : maps.getNextMap(world.getMap());
            nextMapOverride = null;
            if(map != null){
                Call.onInfoMessage((state.rules.pvp
                ? "[YELLOW]The " + event.winner.name + " team is victorious![]" : "[SCARLET]Game over![]")
                + "\nNext selected map:[accent] " + map.name() + "[]"
                + (map.tags.containsKey("author") && !map.tags.get("author").trim().isEmpty() ? " by[accent] " + map.author() + "[]" : "") + "." +
                "\nNew game begins in " + roundExtraTime + "[] seconds.");

                info("Selected next map to be {0}.", map.name());

                play(true, () -> world.loadMap(map, map.applyRules(lastMode)));
            }else{
                netServer.kickAll(KickReason.gameover);
                state.set(State.menu);
                net.closeServer();
            }
        });

        Events.on(Trigger.socketConfigChanged, () -> {
            toggleSocket(false);
            toggleSocket(Config.socketInput.bool());
        });

        Events.on(PlayEvent.class, e -> {
            try{
                JsonValue value = JsonIO.json().fromJson(null, Core.settings.getString("globalrules"));
                JsonIO.json().readFields(state.rules, value);
            }catch(Throwable t){
                Log.err("Error applying custom rules, proceeding without them.", t);
            }
        });

        if(!mods.list().isEmpty()){
            info("&lc{0} mods loaded.", mods.list().size);
        }

        toggleSocket(Config.socketInput.bool());

        info("&lcServer loaded. Type &ly'help'&lc for help.");
    }

    private void registerCommands(){
        handler.register("help", "Displays this command list.", arg -> {
            info("Commands:");
            for(Command command : handler.getCommandList()){
                info("   &y" + command.text + (command.paramText.isEmpty() ? "" : " ") + command.paramText + " - &lm" + command.description);
            }
        });

        handler.register("version", "Displays server version info.", arg -> {
            info("&lmVersion: &lyMindustry {0}-{1} {2} / build {3}", Version.number, Version.modifier, Version.type, Version.build + (Version.revision == 0 ? "" : "." + Version.revision));
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

        handler.register("host", "[mapname] [mode]", "Open the server. Will default to survival and a random map if not specified.", arg -> {
            if(state.is(State.playing)){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            if(lastTask != null) lastTask.cancel();
            
            Map result;
            if(arg.length > 0){
                result = maps.all().find(map -> map.name().equalsIgnoreCase(arg[0].replace('_', ' ')) || map.name().equalsIgnoreCase(arg[0]));

                if(result == null){
                    err("No map with name &y'{0}'&lr found.", arg[0]);
                    return;
                }
            }else{
                Array<Map> maps = Vars.maps.customMaps().size == 0 ? Vars.maps.defaultMaps() : Vars.maps.customMaps();
                result = maps.random();
                info("Randomized next map to be {0}.", result.name());
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
                world.loadMap(result, result.applyRules(lastMode));
                state.rules = result.applyRules(preset);
                logic.play();

                info("Map loaded.");

                netServer.openServer();
            }catch(MapException e){
                Log.err(e.map.name() + ": " + e.getMessage());
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
                    info("&ly  {0} enemies.", state.enemies);
                }else{
                    info("&ly  {0} seconds until next wave.", (int)(state.wavetime / 60));
                }

                info("  &ly{0} FPS, {1} MB used.", Core.graphics.getFramesPerSecond(), Core.app.getJavaHeap() / 1024 / 1024);

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
            if(!mods.list().isEmpty()){
                info("Mods:");
                for(LoadedMod mod : mods.list()){
                    info("  &ly{0} &lcv{1}", mod.meta.displayName(), mod.meta.version);
                }
            }else{
                info("No mods found.");
            }
            info("&lyMod directory: &lb&fi{0}", modDirectory.file().getAbsoluteFile().toString());
        });

        handler.register("mod", "<name...>", "Display information about a loaded plugin.", arg -> {
            LoadedMod mod = mods.list().find(p -> p.meta.name.equalsIgnoreCase(arg[0]));
            if(mod != null){
                info("Name: &ly{0}", mod.meta.displayName());
                info("Internal Name: &ly{0}", mod.name);
                info("Version: &ly{0}", mod.meta.version);
                info("Author: &ly{0}", mod.meta.author);
                info("Path: &ly{0}", mod.file.path());
                info("Description: &ly{0}", mod.meta.description);
            }else{
                info("No mod with name &ly'{0}'&lg found.");
            }
        });

        handler.register("js", "<script...>", "Run arbitrary Javascript.", arg -> {
            info("&lc" + mods.getScripts().runConsole(arg[0]));
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

        handler.register("rules", "[remove/add] [name] [value...]", "List, remove or add global rules. These will apply regardless of map.", arg -> {
            String rules = Core.settings.getString("globalrules");
            JsonValue base = JsonIO.json().fromJson(null, rules);

            if(arg.length == 0){
                Log.info("&lyRules:\n{0}", JsonIO.print(rules));
            }else if(arg.length == 1){
                Log.err("Invalid usage. Specify which rule to remove or add.");
            }else{
                if(!(arg[0].equals("remove") || arg[0].equals("add"))){
                    Log.err("Invalid usage. Either add or remove rules.");
                    return;
                }

                boolean remove = arg[0].equals("remove");
                if(remove){
                    if(base.has(arg[1])){
                        Log.info("Rule &lc'{0}'&lg removed.", arg[1]);
                        base.remove(arg[1]);
                    }else{
                        Log.err("Rule not defined, so not removed.");
                        return;
                    }
                }else{
                    if(arg.length < 3){
                        Log.err("Missing last argument. Specify which value to set the rule to.");
                        return;
                    }

                    try{
                        JsonValue value = new JsonReader().parse(arg[2]);
                        value.name = arg[1];

                        JsonValue parent = new JsonValue(ValueType.object);
                        parent.addChild(value);

                        JsonIO.json().readField(state.rules, value.name, parent);
                        if(base.has(value.name)){
                            base.remove(value.name);
                        }
                        base.addChild(arg[1], value);
                        Log.info("Changed rule: &ly{0}", value.toString().replace("\n", " "));
                    }catch(Throwable e){
                        Log.err("Error parsing rule JSON: {0}", e.getMessage());
                    }
                }

                Core.settings.putSave("globalrules", base.toString());
                Call.onSetRules(state.rules);
            }
        });

        handler.register("fillitems", "[team]", "Fill the core with items.", arg -> {
            if(!state.is(State.playing)){
                err("Not playing. Host first.");
                return;
            }

            Team team = arg.length == 0 ? Team.sharded : Structs.find(Team.all(), t -> t.name.equals(arg[0]));

            if(team == null){
                err("No team with that name found.");
                return;
            }

            if(state.teams.cores(team).isEmpty()){
                err("That team has no cores.");
                return;
            }

            for(Item item : content.items()){
                if(item.type == ItemType.material){
                    state.teams.cores(team).first().items.set(item, state.teams.cores(team).first().block.itemCapacity);
                }
            }

            info("Core filled.");

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

        handler.register("config", "[name] [value...]", "Configure server settings.", arg -> {
            if(arg.length == 0){
                info("&lyAll config values:");
                for(Config c : Config.all){
                    Log.info("&ly| &lc{0}:&lm {1}", c.name(), c.get());
                    Log.info("&ly| | {0}", c.description);
                    Log.info("&ly|");
                }
                return;
            }

            try{
                Config c = Config.valueOf(arg[0]);
                if(arg.length == 1){
                    Log.info("&lc'{0}'&lg is currently &lc{1}.", c.name(), c.get());
                }else{
                    if(c.isBool()){
                        c.set(arg[1].equals("on") || arg[1].equals("true"));
                    }else if(c.isNum()){
                        try{
                            c.set(Integer.parseInt(arg[1]));
                        }catch(NumberFormatException e){
                            Log.err("Not a valid number: {0}", arg[1]);
                            return;
                        }
                    }else if(c.isString()){
                        c.set(arg[1]);
                    }

                    Log.info("&lc{0}&lg set to &lc{1}.", c.name(), c.get());
                }
            }catch(IllegalArgumentException e){
                err("Unknown config: '{0}'. Run the command with no arguments to get a list of valid configs.", arg[0]);
            }
        });

        handler.register("subnet-ban", "[add/remove] [address]", "Ban a subnet. This simply rejects all connections with IPs starting with some string.", arg -> {
            if(arg.length == 0){
                Log.info("Subnets banned: &lc{0}", netServer.admins.getSubnetBans().isEmpty() ? "<none>" : "");
                for(String subnet : netServer.admins.getSubnetBans()){
                    Log.info("&ly  " + subnet + "");
                }
            }else if(arg.length == 1){
                err("You must provide a subnet to add or remove.");
            }else{
                if(arg[0].equals("add")){
                    if(netServer.admins.getSubnetBans().contains(arg[1])){
                        err("That subnet is already banned.");
                        return;
                    }

                    netServer.admins.addSubnetBan(arg[1]);
                    Log.info("Banned &ly{0}&lc**", arg[1]);
                }else if(arg[0].equals("remove")){
                    if(!netServer.admins.getSubnetBans().contains(arg[1])){
                        err("That subnet isn't banned.");
                        return;
                    }

                    netServer.admins.removeSubnetBan(arg[1]);
                    Log.info("Unbanned &ly{0}&lc**", arg[1]);
                }else{
                    err("Incorrect usage. You must provide add/remove as the second argument.");
                }
            }
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

        handler.register("shuffle", "[none/all/custom/builtin]", "Set map shuffling mode.", arg -> {
            if(arg.length == 0){
                info("Shuffle mode current set to &ly'{0}'&lg.", maps.getShuffleMode());
            }else{
                try{
                    ShuffleMode mode = ShuffleMode.valueOf(arg[0]);
                    Core.settings.putSave("shufflemode", mode.name());
                    maps.setShuffleMode(mode);
                    info("Shuffle mode set to &ly'{0}'&lg.", arg[0]);
                }catch(Exception e){
                    err("Invalid shuffle mode.");
                }
            }
        });

        handler.register("nextmap", "<mapname...>", "Set the next map to be played after a game-over. Overrides shuffling.", arg -> {
            Map res = maps.all().find(map -> map.name().equalsIgnoreCase(arg[0].replace('_', ' ')) || map.name().equalsIgnoreCase(arg[0]));
            if(res != null){
                nextMapOverride = res;
                Log.info("Next map set to &ly'{0}'.", res.name());
            }else{
                Log.err("No map '{0}' found.", arg[0]);
            }
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
            if(netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])){
                info("Unbanned player.", arg[0]);
            }else{
                err("That IP/ID is not banned!");
            }
        });
        
        handler.register("pardon", "<ID>", "Pardons a votekicked player by ID and allows them to join again.", arg -> {
            PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);
            
            if(info != null){
                info.lastKicked = 0;
                info("Pardoned player: {0}", info.lastName);
            }else{
                err("That ID can't be found.");
            }
        });

        handler.register("admin", "<add/remove> <username/ID...>", "Make an online user admin", arg -> {
            if(!state.is(State.playing)){
                err("Open the server first.");
                return;
            }

            if(!(arg[0].equals("add") || arg[0].equals("remove"))){
                err("Second parameter must be either 'add' or 'remove'.");
                return;
            }

            boolean add = arg[0].equals("add");

            PlayerInfo target;
            Player playert = playerGroup.find(p -> p.name.equals(arg[1]));
            if(playert != null){
                target = playert.getInfo();
            }else{
                target = netServer.admins.getInfoOptional(arg[1]);
                playert = playerGroup.find(p -> p.getInfo() == target);
            }

            if(target != null){
                if(add){
                    netServer.admins.adminPlayer(target.id, target.adminUsid);
                }else{
                    netServer.admins.unAdminPlayer(target.id);
                }
                if(playert != null) playert.isAdmin = add;
                info("Changed admin status of player: &ly{0}", target.lastName);
            }else{
                err("Nobody with that name or ID could be found. If adding an admin by name, make sure they're online; otherwise, use their UUID.");
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

        handler.register("players", "List all players currently in game.", arg -> {
            if(playerGroup.size() == 0){
                info("No players are currently in the server.");
            }else{
                info("&lyPlayers: {0}", playerGroup.size());
                for(Player user : playerGroup){
                    PlayerInfo userInfo = user.getInfo();
                    info(" &lm {0} /  ID: '{1}' / IP: '{2}' / Admin: '{3}'", userInfo.lastName, userInfo.id, userInfo.lastIP, userInfo.admin);
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
            }

            Fi file = saveDirectory.child(arg[0] + "." + saveExtension);

            if(!SaveIO.isSaveValid(file)){
                err("No (valid) save data found for slot.");
                return;
            }

            Core.app.post(() -> {
                try{
                    SaveIO.load(file);
                    state.rules.zone = null;
                    info("Save loaded.");
                    state.set(State.playing);
                    netServer.openServer();
                }catch(Throwable t){
                    err("Failed to load save. Outdated or corrupt file.");
                }
            });
        });

        handler.register("save", "<slot>", "Save game state to a slot.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting. Host a game first.");
                return;
            }

            Fi file = saveDirectory.child(arg[0] + "." + saveExtension);

            Core.app.post(() -> {
                SaveIO.save(file);
                info("Saved to {0}.", file);
            });
        });

        handler.register("saves", "List all saves in the save directory.", arg -> {
            info("Save files: ");
            for(Fi file : saveDirectory.list()){
                if(file.extension().equals(saveExtension)){
                    info("| &ly{0}", file.nameWithoutExtension());
                }
            }
        });

        handler.register("gameover", "Force a game over.", arg -> {
            if(state.is(State.menu)){
                err("Not playing a map.");
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

        handler.register("search", "<name...>", "Search players who have used part of a name.", arg -> {

            ObjectSet<PlayerInfo> infos = netServer.admins.searchNames(arg[0]);

            if(infos.size > 0){
                info("&lgPlayers found: {0}", infos.size);

                int i = 0;
                for(PlayerInfo info : infos){
                    info("- &lc[{0}] &ly'{1}'&lc / &lm{2}", i++, info.lastName, info.id);
                }
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("gc", "Trigger a grabage struct. Testing only.", arg -> {
            int pre = (int)(Core.app.getJavaHeap() / 1024 / 1024);
            System.gc();
            int post = (int)(Core.app.getJavaHeap() / 1024 / 1024);
            info("&ly{0}&lg MB collected. Memory usage now at &ly{1}&lg MB.", pre - post, post);
        });

        mods.eachClass(p -> p.registerServerCommands(handler));
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
            state.rules = world.getMap().applyRules(lastMode);
            logic.play();

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
                    serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(Config.socketInputAddress.string(), Config.socketInputPort.num()));
                    while(true){
                        Socket client = serverSocket.accept();
                        info("&lmRecieved command socket connection: &lb{0}", serverSocket.getLocalSocketAddress());
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        socketOutput = new PrintWriter(client.getOutputStream(), true);
                        String line;
                        while(client.isConnected() && (line = in.readLine()) != null){
                            String result = line;
                            Core.app.post(() -> handleCommandString(result));
                        }
                        info("&lmLost command socket connection: &lb{0}", serverSocket.getLocalSocketAddress());
                        socketOutput = null;
                    }
                }catch(BindException b){
                    err("Command input socket already in use. Is another instance of the server running?");
                }catch(IOException e){
                    if(!e.getMessage().equals("Socket closed")){
                        err("Terminating socket server.");
                        e.printStackTrace();
                    }
                }
            });
            socketThread.setDaemon(true);
            socketThread.start();
        }else if(socketThread != null){
            socketThread.interrupt();
            try{
                serverSocket.close();
            }catch(IOException e){
                e.printStackTrace();
            }
            socketThread = null;
            socketOutput = null;
        }
    }
}
