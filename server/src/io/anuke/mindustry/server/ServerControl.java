package io.anuke.mindustry.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.game.EventType.SectorCompleteEvent;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.net.TraceInfo;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.*;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.CommandHandler;
import io.anuke.ucore.util.CommandHandler.Command;
import io.anuke.ucore.util.CommandHandler.Response;
import io.anuke.ucore.util.CommandHandler.ResponseType;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import java.io.IOException;
import java.util.Scanner;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.util.Log.*;

public class ServerControl extends Module{
    private static final int roundExtraTime = 12;

    private final CommandHandler handler = new CommandHandler("");
    private int gameOvers;
    private boolean inExtraRound;
    private Task lastTask;

    public ServerControl(String[] args){
        Settings.defaultList(
            "shufflemode", "normal",
            "bans", "",
            "admins", "",
            "sector_x", 2,
            "sector_y", 1,
            "shuffle", true,
            "crashreport", false,
            "port", port
        );

        Timers.setDeltaProvider(() -> Gdx.graphics.getDeltaTime() * 60f);
        Effects.setScreenShakeProvider((a, b) -> {});
        Effects.setEffectProvider((a, b, c, d, e, f) -> {});
        Sounds.setHeadless(true);

        registerCommands();

        Gdx.app.postRunnable(() -> {
            String[] commands = {};

            if(args.length > 0){
                commands = String.join(" ", args).split(",");
                Log.info("&lmFound {0} command-line arguments to parse. {1}", commands.length);
            }

            for(String s : commands){
                Response response = handler.handleMessage(s);
                if(response.type != ResponseType.valid){
                    Log.err("Invalid command argument sent: '{0}': {1}", s, response.type.name());
                    Log.err("Argument usage: &lc<command-1> <command1-args...>,<command-2> <command-2-args2...>");
                    System.exit(1);
                }
            }
        });

        Thread thread = new Thread(this::readCommands, "Server Controls");
        thread.setDaemon(true);
        thread.start();

        if(Version.build == -1){
            err("WARNING: &lyYour server is running a custom build, which means that client checking is disabled.\n" +
            "&lrWARNING: &lyIt is highly advised to specify which version you're using by building with gradle args &lc-Pbuildversion=&lm<build>&ly so that clients know which version you are using.");
        }

        Events.on(SectorCompleteEvent.class, event -> {
            Log.info("Sector complete.");
            world.sectors.completeSector(world.getSector().x, world.getSector().y);
            world.sectors.save();
            gameOvers = 0;
            inExtraRound = true;
            Settings.putInt("sector_x", world.getSector().x + world.getSector().width);
            Settings.save();

            Call.onInfoMessage("[accent]Sector conquered![]\n" + roundExtraTime + " seconds until deployment in next sector.");

            playSectorMap();
        });

        Events.on(GameOverEvent.class, event -> {
            if(inExtraRound) return;
            info("Game over!");

            if(Settings.getBool("shuffle")){
                if(world.getSector() == null){
                    if(world.maps.all().size > 0){
                        Array<Map> maps = world.maps.customMaps().size == 0 ? world.maps.defaultMaps() : world.maps.customMaps();

                        Map previous = world.getMap();
                        Map map = previous;
                        if(maps.size > 1){
                            while(map == previous) map = maps.random();
                        }

                        Call.onInfoMessage((state.mode.isPvp
                        ? "[YELLOW]The " + event.winner.name() + " team is victorious![]" : "[SCARLET]Game over![]")
                        + "\nNext selected map:[accent] "+map.name+"[]"
                        + (map.meta.author() != null ? " by[accent] " + map.meta.author() + "[]" : "") + "."+
                        "\nNew game begins in " + roundExtraTime + " seconds.");

                        info("Selected next map to be {0}.", map.name);

                        Map fmap = map;

                        play(true, () -> world.loadMap(fmap));
                    }
                }else{
                    Call.onInfoMessage("[SCARLET]Sector has been lost.[]\nRe-deploying in " + roundExtraTime + " seconds.");
                    if(gameOvers >= 2){
                        Settings.putInt("sector_y", Settings.getInt("sector_y") < 0 ? Settings.getInt("sector_y") + 1 : Settings.getInt("sector_y") - 1);
                        Settings.save();
                        gameOvers = 0;
                    }
                    gameOvers ++;
                    playSectorMap();
                    info("Re-trying sector map: {0} {1}",  Settings.getInt("sector_x"), Settings.getInt("sector_y"));
                }
            }else{
                netServer.kickAll(KickReason.gameover);
                state.set(State.menu);
                Net.closeServer();
            }
        });

        info("&lcServer loaded. Type &ly'help'&lc for help.");
    }

    private void registerCommands(){
        handler.register("help", "Displays this command list.", arg -> {
            info("Commands:");
            for(Command command : handler.getCommandList()){
                print("   &y" + command.text + (command.paramText.isEmpty() ? "" : " ") + command.paramText + " - &lm" + command.description);
            }
        });

        handler.register("version", "Displays server version info.", arg -> {
            info("&lmVersion: &lyMindustry {0}-{1} {2} / build {3}", Version.number, Version.modifier, Version.type, Version.build);
            info("&lmJava Version: &ly{0}", System.getProperty("java.version"));
        });

        handler.register("exit", "Exit the server application.", arg -> {
            info("Shutting down server.");
            Net.dispose();
            Gdx.app.exit();
        });

        handler.register("stop", "Stop hosting the server.", arg -> {
            Net.closeServer();
            if(lastTask != null) lastTask.cancel();
            state.set(State.menu);
            netServer.reset();
            Log.info("Stopped server.");
        });

        handler.register("host", "[mapname] [mode]", "Open the server with a specific map.", arg -> {
            if(state.is(State.playing)){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            if(lastTask != null) lastTask.cancel();

            Map result = null;

            if(arg.length > 0){

                String search = arg[0];
                for(Map map : world.maps.all()){
                    if(map.name.equalsIgnoreCase(search)) result = map;
                }

                if(result == null){
                    err("No map with name &y'{0}'&lr found.", search);
                    return;
                }


                info("Loading map...");

                if(arg.length > 1){
                    GameMode mode;
                    try{
                        mode = GameMode.valueOf(arg[1]);
                    }catch(IllegalArgumentException e){
                        err("No gamemode '{0}' found.", arg[1]);
                        return;
                    }

                    state.mode = mode;
                }

                logic.reset();
                world.loadMap(result);
                logic.play();

            }else{
                info("&ly&fiNo map specified. Loading sector {0}, {1}.", Settings.getInt("sector_x"), Settings.getInt("sector_y"));
                playSectorMap(false);
            }

            info("Map loaded.");

            host();
        });

        handler.register("port", "[port]", "Sets or displays the port for hosting the server.", arg -> {
            if(arg.length == 0){
                info("&lyPort: &lc{0}", Settings.getInt("port"));
            }else{
                int port = Strings.parseInt(arg[0]);
                if(port < 0 || port > 65535){
                    err("Port must be a number between 0 and 65535.");
                    return;
                }
                info("&lyPort set to {0}.", port);
                Settings.putInt("port", port);
                Settings.save();
            }
        });

        handler.register("maps", "Display all available maps.", arg -> {
            info("Maps:");
            for(Map map : world.maps.all()){
                info("  &ly{0}: &lb&fi{1} / {2}x{3}", map.name, map.custom ? "Custom" : "Default", map.meta.width, map.meta.height);
            }
        });

        handler.register("status", "Display server status.", arg -> {
            if(state.is(State.menu)){
                info("&lyStatus: &rserver closed");
            }else{
                info("&lyStatus: &lcPlaying on map &fi{0}&fb &lb/&lc Wave {1} &lb/&lc {2} &lb/&lc {3}",
                        Strings.capitalize(world.getMap().name), state.wave, Strings.capitalize(state.difficulty.name()), Strings.capitalize(state.mode.name()));
                if(state.mode.disableWaveTimer){
                    info("&ly{0} enemies.", unitGroups[Team.red.ordinal()].size());
                }else{
                    info("&ly{0} seconds until next wave.", (int) (state.wavetime / 60));
                }

                if(playerGroup.size() > 0){
                    info("&lyPlayers: {0}", playerGroup.size());
                    for(Player p : playerGroup.all()){
                        print("   &y" + p.name);
                    }
                }else{
                    info("&lyNo players connected.");
                }
                info("&lbFPS: {0}", (int) (60f / Timers.delta()));
            }
        });

        handler.register("players", "Display player info.", arg -> {
            if(state.is(State.menu)){
                info("&lyServer is closed.");
            }else{
                if(playerGroup.size() > 0){
                    info("&lyPlayers: {0}", playerGroup.size());
                    for(Player p : playerGroup.all()){
                        print("   &y{0} / Connection {1} / IP: {2}", p.name, p.con.id, p.con.address);
                    }
                }else{
                    info("&lyNo players connected.");
                }
            }
        });

        handler.register("say", "<message...>", "Send a message to all players.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting. Host a game first.");
                return;
            }

            Call.sendMessage("[GRAY][[Server]:[] " + arg[0]);

            info("&lyServer: &lb{0}", arg[0]);
        });

        handler.register("difficulty", "<difficulty>", "Set game difficulty.", arg -> {
            try{
                state.difficulty = Difficulty.valueOf(arg[0]);
                info("Difficulty set to '{0}'.", arg[0]);
            }catch(IllegalArgumentException e){
                err("No difficulty with name '{0}' found.", arg[0]);
            }
        });

        handler.register("setsector", "<x> <y>", "Sets the next sector to be played. Does not affect current game.", arg -> {
            try{
                Settings.putInt("sector_x", Integer.parseInt(arg[0]));
                Settings.putInt("sector_y", Integer.parseInt(arg[1]));
                Settings.save();
                info("Sector position set.");
            }catch(NumberFormatException e){
                err("Invalid coordinates.");
            }
        });

        handler.register("fillitems", "Fill the core with 2000 items.", arg -> {
            if(!state.is(State.playing)){
                err("Not playing. Host first.");
                return;
            }

            for(Item item : content.items()){
                if(item.type == ItemType.material){
                    state.teams.get(Team.blue).cores.first().entity.items.add(item, 2000);
                }
            }
            info("Core filled.");
        });

        handler.register("crashreport", "<on/off>", "Disables or enables automatic crash reporting", arg -> {
            boolean value = arg[0].equalsIgnoreCase("on");
            Settings.putBool("crashreport", value);
            Settings.save();
            info("Crash reporting is now {0}.", value ? "on" : "off");
        });

        handler.register("strict", "<on/off>", "Disables or enables strict mode", arg -> {
           boolean value = arg[0].equalsIgnoreCase("on");
           netServer.admins.setStrict(value);
           info("Strict mode is now {0}.", netServer.admins.getStrict() ? "on" : "off");
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
            Settings.putBool("shuffle", arg[0].equals("on"));
            Settings.save();
            info("Shuffle mode set to '{0}'.", arg[0]);
        });

        handler.register("kick", "<username...>", "Kick a person by name.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting a game yet. Calm down.");
                return;
            }

            Player target = null;

            for(Player player : playerGroup.all()){
                if(player.name.equalsIgnoreCase(arg[0])){
                    target = player;
                    break;
                }
            }

            if(target != null){
                netServer.kick(target.con.id, KickReason.kick);
                info("It is done.");
            }else{
                info("Nobody with that name could be found...");
            }
        });

        handler.register("ban", "<username...>", "Ban a person by name.", arg -> {
            if(!state.is(State.playing)){
                err("Can't ban people by name with no players.");
                return;
            }

            Player target = null;

            for(Player player : playerGroup.all()){
                if(player.name.equalsIgnoreCase(arg[0])){
                    target = player;
                }
            }

            if(target != null){
                String ip = target.con.address;
                netServer.admins.banPlayerIP(ip);
                netServer.admins.banPlayerID(target.uuid);
                netServer.kick(target.con.id, KickReason.banned);
                info("Banned player by IP and ID: {0} / {1}", ip, target.uuid);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("bans", "List all banned IPs and IDs.", arg -> {
            Array<PlayerInfo> bans = netServer.admins.getBanned();

            if(bans.size == 0){
                Log.info("No ID-banned players have been found.");
            }else{
                Log.info("&lyBanned players [ID]:");
                for(PlayerInfo info : bans){
                    Log.info(" &ly {0} / Last known name: '{1}'", info.id, info.lastName);
                }
            }

            Array<String> ipbans = netServer.admins.getBannedIPs();

            if(ipbans.size == 0){
                Log.info("No IP-banned players have been found.");
            }else{
                Log.info("&lmBanned players [IP]:");
                for(String string : ipbans){
                    PlayerInfo info = netServer.admins.findByIP(string);
                    if(info != null){
                        Log.info(" &lm '{0}' / Last known name: '{1}' / ID: '{2}'", string, info.lastName, info.id);
                    }else{
                        Log.info(" &lm '{0}' (No known name or info)", string);
                    }
                }
            }
        });

        handler.register("banip", "<ip>", "Ban a person by IP.", arg -> {
            if(netServer.admins.banPlayerIP(arg[0])){
                info("Banned player by IP: {0}.", arg[0]);

                for(Player player : playerGroup.all()){
                    if(player.con.address != null &&
                            player.con.address.equals(arg[0])){
                        netServer.kick(player.con.id, KickReason.banned);
                    }
                }
            }else{
                err("That IP is already banned!");
            }
        });

        handler.register("banid", "<id>", "Ban a person by their unique ID.", arg -> {
            if(netServer.admins.banPlayerID(arg[0])){
                info("Banned player by ID: {0}.", arg[0]);

                for(Player player : playerGroup.all()){
                    if(player.uuid.equals(arg[0])){
                        netServer.kick(player.con.id, KickReason.banned);
                    }
                }
            }else{
                err("That ID is already banned!");
            }
        });

        handler.register("unbanip", "<ip>", "Completely unban a person by IP.", arg -> {
            if(netServer.admins.unbanPlayerIP(arg[0])){
                info("Unbanned player by IP: {0}.", arg[0]);
            }else{
                err("That IP is not banned!");
            }
        });

        handler.register("unbanid", "<id>", "Completely unban a person by ID.", arg -> {
            if(netServer.admins.unbanPlayerID(arg[0])){
                info("&lmUnbanned player by ID: {0}.", arg[0]);
            }else{
                err("That IP is not banned!");
            }
        });

        handler.register("admin", "<username...>", "Make a user admin", arg -> {
            if(!state.is(State.playing)){
                err("Open the server first.");
                return;
            }

            Player target = null;

            for(Player player : playerGroup.all()){
                if(player.name.equalsIgnoreCase(arg[0])){
                    target = player;
                    break;
                }
            }

            if(target != null){
                netServer.admins.adminPlayer(target.uuid, target.usid);
                target.isAdmin = true;
                info("Admin-ed player by ID: {0} / {1}", target.uuid, arg[0]);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("unadmin", "<username...>", "Removes admin status from a player", arg -> {
            if(!state.is(State.playing)){
                err("Open the server first.");
                return;
            }

            Player target = null;

            for(Player player : playerGroup.all()){
                if(player.name.equalsIgnoreCase(arg[0])){
                    target = player;
                    break;
                }
            }

            if(target != null){
                netServer.admins.unAdminPlayer(target.uuid);
                target.isAdmin = false;
                info("Un-admin-ed player by ID: {0} / {1}", target.uuid, arg[0]);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("admins", "List all admins.", arg -> {
            Array<PlayerInfo> admins = netServer.admins.getAdmins();

            if(admins.size == 0){
                Log.info("No admins have been found.");
            }else{
                Log.info("&lyAdmins:");
                for(PlayerInfo info : admins){
                    Log.info(" &lm {0} /  ID: '{1}' / IP: '{2}'", info.lastName, info.id, info.lastIP);
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
                err("No save data found for slot.");
                return;
            }

            threads.run(() -> {
                SaveIO.loadFromSlot(slot);
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

            threads.run(() -> {
                int slot = Strings.parseInt(arg[0]);
                SaveIO.saveToSlot(slot);
                info("Saved to slot {0}.", slot);
            });
        });

        handler.register("griefers", "[min-break:place-ratio] [min-breakage]", "Find possible griefers currently online.", arg -> {
            if(!state.is(State.playing)){
                err("Open the server first.");
                return;
            }

            try{

                float ratio = arg.length > 0 ? Float.parseFloat(arg[0]) : 0.5f;
                int minbreak = arg.length > 1 ? Integer.parseInt(arg[1]) : 100;

                boolean found = false;

                for(Player player : playerGroup.all()){
                    TraceInfo info = netServer.admins.getTraceByID(player.uuid);
                    if(info.totalBlocksBroken >= minbreak && info.totalBlocksBroken / Math.max(info.totalBlocksPlaced, 1f) >= ratio){
                        info("&ly - Player '{0}' / UUID &lm{1}&ly found: &lc{2}&ly broken and &lc{3}&ly placed.",
                                player.name, info.uuid, info.totalBlocksBroken, info.totalBlocksPlaced);
                        found = true;
                    }
                }

                if(!found){
                    info("No griefers matching the criteria have been found.");
                }

            }catch(NumberFormatException e){
                err("Invalid number format.");
            }
        });

        handler.register("gameover", "Force a game over.", arg -> {
            if(state.is(State.menu)){
                info("Not playing a map.");
                return;
            }

            info("&lyCore destroyed.");
            inExtraRound = false;
            Events.fire(new GameOverEvent(Team.red));
        });

        handler.register("traceblock", "<x> <y>", "Prints debug info about a block", arg -> {
            try{
                int x = Integer.parseInt(arg[0]);
                int y = Integer.parseInt(arg[1]);
                Tile tile = world.tile(x, y);
                if(tile != null){
                    if(tile.entity != null){
                        Array<Object> arr = tile.block().getDebugInfo(tile);
                        StringBuilder result = new StringBuilder();
                        for(int i = 0; i < arr.size / 2; i++){
                            result.append(arr.get(i * 2));
                            result.append(": ");
                            result.append(arr.get(i * 2 + 1));
                            result.append("\n");
                        }
                        Log.info("&ly{0}", result);
                    }else{
                        Log.info("No tile entity for that block.");
                    }
                }else{
                    Log.info("No tile at that location.");
                }
            }catch(NumberFormatException e){
                Log.err("Invalid coordinates passed.");
            }
        });

        handler.register("find", "<name...>", "Find player info(s) by name. Can optionally check for all names a player has had.", arg -> {
            boolean checkAll = true;

            Array<PlayerInfo> infos = netServer.admins.findByName(arg[0], checkAll);

            if(infos.size == 1){
                PlayerInfo info = infos.peek();
                Log.info("&lcTrace info for player '{0}' / UUID {1}:", info.lastName, info.id);
                Log.info("  &lyall names used: {0}", info.names);
                Log.info("  &lyIP: {0}", info.lastIP);
                Log.info("  &lyall IPs used: {0}", info.ips);
                Log.info("  &lytimes joined: {0}", info.timesJoined);
                Log.info("  &lytimes kicked: {0}", info.timesKicked);
                Log.info("");
                Log.info("  &lytotal blocks broken: {0}", info.totalBlocksBroken);
                Log.info("  &lytotal blocks placed: {0}", info.totalBlockPlaced);
            }else if(infos.size > 1){
                Log.info("&lcMultiple people have been found with that name:");
                for(PlayerInfo info : infos){
                    Log.info("  &ly{0}", info.id);
                }
                Log.info("&lcUse the info command to examine each person individually.");
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("findip", "<ip>", "Find player info(s) by IP.", arg -> {

            Array<PlayerInfo> infos = netServer.admins.findByIPs(arg[0]);

            if(infos.size == 1){
                PlayerInfo info = infos.peek();
                Log.info("&lcTrace info for player '{0}' / UUID {1}:", info.lastName, info.id);
                Log.info("  &lyall names used: {0}", info.names);
                Log.info("  &lyIP: {0}", info.lastIP);
                Log.info("  &lyall IPs used: {0}", info.ips);
                Log.info("  &lytimes joined: {0}", info.timesJoined);
                Log.info("  &lytimes kicked: {0}", info.timesKicked);
                Log.info("");
                Log.info("  &lytotal blocks broken: {0}", info.totalBlocksBroken);
                Log.info("  &lytotal blocks placed: {0}", info.totalBlockPlaced);
            }else if(infos.size > 1){
                Log.info("&lcMultiple people have been found with that IP:");
                for(PlayerInfo info : infos){
                    Log.info("  &ly{0}", info.id);
                }
                Log.info("&lcUse the info command to examine each person individually.");
            }else{
                info("Nobody with that IP could be found.");
            }
        });


        handler.register("info", "<UUID>", "Get global info for a player's UUID.", arg -> {
            PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);

            if(info != null){
                Log.info("&lcTrace info for player '{0}':", info.lastName);
                Log.info("  &lyall names used: {0}", info.names);
                Log.info("  &lyIP: {0}", info.lastIP);
                Log.info("  &lyall IPs used: {0}", info.ips);
                Log.info("  &lytimes joined: {0}", info.timesJoined);
                Log.info("  &lytimes kicked: {0}", info.timesKicked);
                Log.info("");
                Log.info("  &lytotal blocks broken: {0}", info.totalBlocksBroken);
                Log.info("  &lytotal blocks placed: {0}", info.totalBlockPlaced);
            }else{
                info("Nobody with that UUID could be found.");
            }
        });

        handler.register("trace", "<username...>", "Trace a player's actions", arg -> {
            if(!state.is(State.playing)){
                err("Open the server first.");
                return;
            }

            Player target = null;

            for(Player player : playerGroup.all()){
                if(player.name.equalsIgnoreCase(arg[0])){
                    target = player;
                    break;
                }
            }

            if(target != null){
                TraceInfo info = netServer.admins.getTraceByID(target.uuid);
                Log.info("&lcTrace info for player '{0}':", target.name);
                Log.info("  &lyEntity ID: {0}", info.playerid);
                Log.info("  &lyIP: {0}", info.ip);
                Log.info("  &lyUUID: {0}", info.uuid);
                Log.info("  &lycustom client: {0}", info.modclient);
                Log.info("  &lyandroid: {0}", info.android);
                Log.info("");
                Log.info("  &lytotal blocks broken: {0}", info.totalBlocksBroken);
                Log.info("  &lystructure blocks broken: {0}", info.structureBlocksBroken);
                Log.info("  &lylast block broken: {0}", info.lastBlockBroken.formalName);
                Log.info("");
                Log.info("  &lytotal blocks placed: {0}", info.totalBlocksPlaced);
                Log.info("  &lylast block placed: {0}", info.lastBlockPlaced.formalName);
            }else{
                info("Nobody with that name could be found.");
            }
        });
    }

    private void readCommands(){
        Scanner scan = new Scanner(System.in);
        while(true){
            String line = scan.nextLine();

            Gdx.app.postRunnable(() -> {
                Response response = handler.handleMessage(line);

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
            });
        }
    }

    private void playSectorMap(){
        playSectorMap(true);
    }

    private void playSectorMap(boolean wait){
        int x = Settings.getInt("sector_x"), y = Settings.getInt("sector_y");
        if(world.sectors.get(x, y) == null){
            world.sectors.createSector(x, y);
        }

        world.sectors.get(x, y).completedMissions = 0;

        play(wait, () -> world.loadSector(world.sectors.get(x, y)));
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
            for(Player p : players){
                p.reset();
                netServer.sendWorldData(p, p.con.id);
            }
            inExtraRound = false;
        };

        if(wait){
            lastTask = new Task(){
                @Override
                public void run(){
                    r.run();
                }
            };

            Timer.schedule(lastTask, roundExtraTime);
        }else{
            r.run();
        }
    }

    private void host(){
        try{
            Net.host(Settings.getInt("port"));
            info("&lcOpened a server on port {0}.", Settings.getInt("port"));
        }catch(IOException e){
            Log.err(e);
            state.set(State.menu);
        }
    }

    @Override
    public void update(){
        if(!inExtraRound && state.mode.isPvp){
        //    checkPvPGameOver();
        }
    }
}
