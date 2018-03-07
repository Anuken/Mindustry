package io.anuke.mindustry.server;

import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.net.Packets.ChatPacket;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.net.TraceInfo;
import io.anuke.mindustry.ui.fragments.DebugFragment;
import io.anuke.mindustry.world.Map;
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

;

public class ServerControl extends Module {
    private final CommandHandler handler = new CommandHandler("");
    private ShuffleMode mode;

    public ServerControl(){
        Settings.defaultList(
            "shufflemode", "normal",
            "bans", "",
            "admins", ""
        );

        mode = ShuffleMode.valueOf(Settings.getString("shufflemode"));

        Effects.setScreenShakeProvider((a, b) -> {});
        Effects.setEffectProvider((a, b, c, d, e) -> {});
        Sounds.setHeadless(true);

        //override default handling of chat packets
        Net.handle(ChatPacket.class, (packet) -> {
            info("&y" + (packet.name == null ? "" : packet.name) +  ": &lb{0}", packet.text);
        });

        //don't do anything at all for GDX logging: don't want controller info and such
        Gdx.app.setApplicationLogger(new ApplicationLogger() {
            @Override public void log(String tag, String message) { }
            @Override public void log(String tag, String message, Throwable exception) { }
            @Override public void error(String tag, String message) { }
            @Override public void error(String tag, String message, Throwable exception) { }
            @Override public void debug(String tag, String message) { }
            @Override public void debug(String tag, String message, Throwable exception) { }
        });

        registerCommands();
        Thread thread = new Thread(this::readCommands, "Server Controls");
        thread.setDaemon(true);
        thread.start();

        Events.on(GameOverEvent.class, () -> {
            info("Game over!");

            Timers.runTask(30f, () -> {
                state.set(State.menu);
                Net.closeServer();

                Timers.runTask(30f, () -> {

                    if (mode != ShuffleMode.off) {
                        Array<Map> maps = mode == ShuffleMode.both ? world.maps().getAllMaps() :
                                mode == ShuffleMode.normal ? world.maps().getDefaultMaps() : world.maps().getCustomMaps();

                        Map previous = world.getMap();
                        Map map = previous;
                        while (map == previous || !map.visible) map = maps.random();

                        info("Selected next map to be {0}.", map.name);
                        state.set(State.playing);
                        logic.reset();
                        world.loadMap(map);
                        host();
                    }
                });
            });
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

        handler.register("exit", "Exit the server application.", arg -> {
            info("Shutting down server.");
            Net.dispose();
            Gdx.app.exit();
        });

        handler.register("stop", "Stop hosting the server.", arg -> {
            Net.closeServer();
            state.set(State.menu);
            netServer.reset();
            Log.info("Stopped server.");
        });

        handler.register("host", "<mapname> <mode>", "Open the server with a specific map.", arg -> {
            if(state.is(State.playing)){
                err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            String search = arg[0];
            Map result = null;
            for(Map map : world.maps().list()){
                if(map.name.equalsIgnoreCase(search))
                    result = map;
            }

            if(result == null){
                err("No map with name &y'{0}'&lr found.", search);
                return;
            }

            GameMode mode = null;
            try{
                mode = GameMode.valueOf(arg[1]);
            }catch (IllegalArgumentException e){
                err("No gamemode '{0}' found.", arg[1]);
                return;
            }

            info("Loading map...");
            state.mode = mode;

            logic.reset();
            world.loadMap(result);
            state.set(State.playing);
            info("Map loaded.");

            host();
        });

        handler.register("maps", "Display all available maps.", arg -> {
            Log.info("Maps:");
            for(Map map : world.maps().getAllMaps()){
                Log.info("  &ly{0}: &lb&fi{1} / {2}x{3}", map.name, map.custom ? "Custom" : "Default", map.getWidth(), map.getHeight());
            }
        });

        handler.register("status", "Display server status.", arg -> {
            if(state.is(State.menu)){
                info("&lyStatus: &rserver closed");
            }else{
                info("&lyStatus: &lcPlaying on map &fi{0}&fb &lb/&lc Wave {1} &lb/&lc {2}",
                        Strings.capitalize(world.getMap().name), state.wave, Strings.capitalize(state.difficulty.name()));
                if(state.enemies > 0){
                    info("&ly{0} enemies remaining.", state.enemies);
                }else{
                    info("&ly{0} seconds until next wave.", (int)(state.wavetime / 60));
                }

                if(playerGroup.size() > 0) {
                    info("&lyPlayers: {0}", playerGroup.size());
                    for (Player p : playerGroup.all()) {
                        print("   &y" + p.name);
                    }
                }else{
                    info("&lyNo players connected.");
                }
                info("&lbFPS: {0}", (int)(60f/Timers.delta()));
            }
        });

        handler.register("players", "Display player info.", arg -> {
            if(state.is(State.menu)){
                info("&lyServer is closed.");
            }else{
                if(playerGroup.size() > 0) {
                    info("&lyPlayers: {0}", playerGroup.size());
                    for (Player p : playerGroup.all()) {
                        print("   &y{0} / Connection {1} / IP: {2}", p.name, p.clientid, Net.getConnection(p.clientid).address);
                    }
                }else{
                    info("&lyNo players connected.");
                }
            }
        });

        handler.register("say", "<message...>", "Send a message to all players.", arg -> {
            if(!state.is(State.playing)) {
                err("Not hosting. Host a game first.");
                return;
            }

            netCommon.sendMessage("[GRAY][[Server]:[] " + arg[0]);
            info("&lyServer: &lb{0}", arg[0]);
        });

        handler.register("difficulty", "<difficulty>", "Set game difficulty.", arg -> {
            try{
                Difficulty diff = Difficulty.valueOf(arg[0]);
                state.difficulty = diff;
                info("Difficulty set to '{0}'.", arg[0]);
            }catch (IllegalArgumentException e){
                err("No difficulty with name '{0}' found.", arg[0]);
            }
        });

        handler.register("friendlyfire", "<on/off>", "Enable or disable friendly fire", arg -> {
            String s = arg[0];
            if(s.equalsIgnoreCase("on")){
                NetEvents.handleFriendlyFireChange(true);
                state.friendlyFire = true;
                info("Friendly fire enabled.");
            }else if(s.equalsIgnoreCase("off")){
                NetEvents.handleFriendlyFireChange(false);
                state.friendlyFire = false;
                info("Friendly fire disabled.");
            }else{
                err("Incorrect command usage.");
            }
        });

        handler.register("shuffle", "<normal/custom/both/off>", "Set map shuffling.", arg -> {

            try{
                mode = ShuffleMode.valueOf(arg[0]);
                Settings.putString("shufflemode", arg[0]);
                Settings.save();
                info("Shuffle mode set to '{0}'.", arg[0]);
            }catch (Exception e){
                err("Unknown shuffle mode '{0}'.", arg[0]);
            }
        });

        handler.register("kick", "<username...>", "Kick a person by name.", arg -> {
            if(!state.is(State.playing)) {
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
                Net.kickConnection(target.clientid, KickReason.kick);
                info("It is done.");
            }else{
                info("Nobody with that name could be found...");
            }
        });

        handler.register("ban", "<username...>", "Ban a person by name.", arg -> {
            if(!state.is(State.playing)) {
                err("Can't ban people by name with no players.");
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
                String ip = Net.getConnection(target.clientid).address;
                netServer.admins.banPlayerIP(ip);
                netServer.admins.banPlayerID(netServer.admins.getTrace(ip).uuid);
                Net.kickConnection(target.clientid, KickReason.banned);
                info("Banned player by IP and ID: {0} / {1}", ip, netServer.admins.getTrace(ip).uuid);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("bans", "List all banned IPs and IDs.", arg -> {
            Array<String> bans = netServer.admins.getBanned();

            if(bans.size == 0){
                Log.info("No IP-banned players have been found.");
            }else{
                Log.info("&lyBanned players [IP]:");
                for(String string : bans){
                    Log.info(" &ly {0} / Last known name: '{1}'", string, netServer.admins.getLastName(string));
                }
            }

            Array<String> idbans = netServer.admins.getBannedIDs();

            if(idbans.size == 0){
                Log.info("No ID-banned players have been found.");
            }else{
                Log.info("&lmBanned players [ID]:");
                for(String string : idbans){
                    Log.info(" &lm '{0}' / Last known name: '{1}' / Last known IP: '{2}'", string,
                            netServer.admins.getLastName(netServer.admins.getLastIP(string)), netServer.admins.getLastIP(string));
                }
            }
        });

        handler.register("banip", "<ip>", "Ban a person by IP.", arg -> {
            if(netServer.admins.banPlayerIP(arg[0])) {
                info("Banned player by IP: {0}.", arg[0]);

                for(Player player : playerGroup.all()){
                    if(Net.getConnection(player.clientid).address.equals(arg[0])){
                        Net.kickConnection(player.clientid, KickReason.banned);
                        break;
                    }
                }
            }else{
                err("That IP is already banned!");
            }
        });

        handler.register("banid", "<id>", "Ban a person by their unique ID.", arg -> {
            if(netServer.admins.banPlayerID(arg[0])) {
                info("Banned player by ID: {0}.", arg[0]);

                for(Player player : playerGroup.all()){
                    if(netServer.admins.getTrace(Net.getConnection(player.clientid).address).uuid.equals(arg[0])){
                        Net.kickConnection(player.clientid, KickReason.banned);
                        break;
                    }
                }
            }else{
                err("That ID is already banned!");
            }
        });

        handler.register("unbanip", "<ip>", "Completely unban a person by IP.", arg -> {
            if(netServer.admins.unbanPlayerIP(arg[0])) {
                info("Unbanned player by IP: {0}.", arg[0]);
                for(String s : netServer.admins.getBannedIDs()){
                    if(netServer.admins.getLastIP(s).equals(arg[0])){
                         netServer.admins.unbanPlayerID(s);
                         Log.info("Also unbanned UUID '{0}' as it corresponds to this IP.", s);
                    }
                }
            }else{
                err("That IP is not banned!");
            }
        });

        handler.register("unbanid", "<id>", "Completely unban a person by ID.", arg -> {
            if(netServer.admins.unbanPlayerID(arg[0])) {
                info("&lmUnbanned player by ID: {0}.", arg[0]);
                String ip = netServer.admins.getLastIP(arg[0]);
                if(!ip.equals("unknown")) {
                    netServer.admins.unbanPlayerIP(ip);
                    Log.info("Also unbanned IP '{0}' as it corresponds to this ID.", ip);
                }
            }else{
                err("That IP is not banned!");
            }
        });

        handler.register("admin", "<username...>", "Make a user admin", arg -> {
            if(!state.is(State.playing)) {
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
                String ip = Net.getConnection(target.clientid).address;
                netServer.admins.adminPlayer(ip);
                NetEvents.handleAdminSet(target, true);
                info("Admin-ed player by IP: {0} / {1}", ip, arg[0]);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("unadmin", "<username...>", "Removes admin status from a player", arg -> {
            if(!state.is(State.playing)) {
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
                String ip = Net.getConnection(target.clientid).address;
                netServer.admins.unAdminPlayer(ip);
                NetEvents.handleAdminSet(target, false);
                info("Un-admin-ed player by IP: {0} / {1}", ip, arg[0]);
            }else{
                info("Nobody with that name could be found.");
            }
        });

        handler.register("admins", "List all admins.", arg -> {
            Array<String> admins = netServer.admins.getAdmins();

            if(admins.size == 0){
                Log.info("No admins have been found.");
            }else{
                Log.info("&lyAdmins:");
                for(String string : admins){
                    Log.info(" &luy {0} / Name: '{1}'", string, netServer.admins.getLastName(string));
                }
            }
        });

        handler.register("runwave", "Trigger the next wave.", arg -> {
            if(!state.is(State.playing)) {
                err("Not hosting. Host a game first.");
            }else if(state.enemies > 0){
                err("There are still {0} enemies remaining.", state.enemies);
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

            SaveIO.loadFromSlot(slot);
            info("Save loaded.");
            host();
            state.set(State.playing);
        });

        handler.register("save", "<slot>", "Save game state to a slot.", arg -> {
            if(!state.is(State.playing)){
                err("Not hosting. Host a game first.");
                return;
            }else if(!Strings.canParseInt(arg[0])){
                err("Invalid save slot '{0}'.", arg[0]);
                return;
            }

            int slot = Strings.parseInt(arg[0]);

            SaveIO.saveToSlot(slot);

            info("Saved to slot {0}.", slot);
        });

        handler.register("gameover", "Force a game over.", arg -> {
            world.removeBlock(world.getCore());
            info("Core destroyed.");
        });

        handler.register("info", "Print debug info", arg -> {
            info(DebugFragment.debugInfo());
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
                        for(int i = 0; i < arr.size/2; i ++){
                            result.append(arr.get(i*2));
                            result.append(": ");
                            result.append(arr.get(i*2 + 1));
                            result.append("\n");
                        }
                        Log.info("&ly{0}", result);
                    }else{
                        Log.info("No tile entity for that block.");
                    }
                }else{
                    Log.info("No tile at that location.");
                }
            }catch (NumberFormatException e){
                Log.err("Invalid coordinates passed.");
            }
        });

        handler.register("trace", "<username...>", "Trace a player's actions", arg -> {
            if(!state.is(State.playing)) {
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
                TraceInfo info = netServer.admins.getTrace(Net.getConnection(target.clientid).address);
                Log.info("&lcTrace info for player '{0}':", target.name);
                Log.info("  &lyEntity ID: {0}", info. playerid);
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

                if (response.type == ResponseType.unknownCommand) {
                    err("Invalid command. Type 'help' for help.");
                }else if (response.type == ResponseType.fewArguments) {
                    err("Too few command arguments. Usage: " + response.command.text + " " + response.command.paramText);
                }else if (response.type == ResponseType.manyArguments) {
                    err("Too many command arguments. Usage: " + response.command.text + " " + response.command.paramText);
                }
            });
        }
    }

    private void host(){
        try {
            Net.host(port);
        }catch (IOException e){
            Log.err(e);
            state.set(State.menu);
        }
    }

    enum ShuffleMode{
        normal, custom, both, off
    }
}
