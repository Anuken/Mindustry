package mindustry.core;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.net.*;
import mindustry.net.Administration.*;
import mindustry.net.Packets.*;
import mindustry.world.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.zip.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;

public class NetServer implements ApplicationListener{
    /** note that snapshots are compressed, so the max snapshot size here is above the typical UDP safe limit */
    private static final int maxSnapshotSize = 800;
    private static final int timerBlockSync = 0, timerHealthSync = 1;
    private static final float blockSyncTime = 60 * 6, healthSyncTime = 30;
    private static final FloatBuffer fbuffer = FloatBuffer.allocate(20);
    private static final Writes dataWrites = new Writes(null);
    private static final IntSeq hiddenIds = new IntSeq();
    private static final IntSeq healthSeq = new IntSeq(maxSnapshotSize / 4 + 1);
    private static final Vec2 vector = new Vec2();
    /** If a player goes away of their server-side coordinates by this distance, they get teleported back. */
    private static final float correctDist = tilesize * 14f;

    public Administration admins = new Administration();
    public CommandHandler clientCommands = new CommandHandler("/");
    public TeamAssigner assigner = (player, players) -> {
        if(state.rules.pvp){
            //find team with minimum amount of players and auto-assign player to that.
            TeamData re = state.teams.getActive().min(data -> {
                if((state.rules.waveTeam == data.team && state.rules.waves) || !data.team.active() || data.team == Team.derelict) return Integer.MAX_VALUE;

                int count = 0;
                for(Player other : players){
                    if(other.team() == data.team && other != player){
                        count++;
                    }
                }
                return count;
            });
            return re == null ? null : re.team;
        }

        return state.rules.defaultTeam;
    };
    /** Converts a message + NULLABLE player sender into a single string. Override for custom prefixes/suffixes. */
    public ChatFormatter chatFormatter = (player, message) -> player == null ? message : "[coral][[" + player.coloredName() + "[coral]]:[white] " + message;

    /** Handles an incorrect command response. Returns text that will be sent to player. Override for customisation. */
    public InvalidCommandHandler invalidHandler = (player, response) -> {
        if(response.type == ResponseType.manyArguments){
            return "[scarlet]Too many arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
        }else if(response.type == ResponseType.fewArguments){
            return "[scarlet]Too few arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
        }else{ //unknown command
            int minDst = 0;
            Command closest = null;

            for(Command command : netServer.clientCommands.getCommandList()){
                int dst = Strings.levenshtein(command.text, response.runCommand);
                if(dst < 3 && (closest == null || dst < minDst)){
                    minDst = dst;
                    closest = command;
                }
            }

            if(closest != null){
                return "[scarlet]Unknown command. Did you mean \"[lightgray]" + closest.text + "[]\"?";
            }else{
                return "[scarlet]Unknown command. Check [lightgray]/help[scarlet].";
            }
        }
    };

    private boolean closing = false, pvpAutoPaused = true;
    private Interval timer = new Interval(10);
    private IntSet buildHealthChanged = new IntSet();

    private ReusableByteOutStream writeBuffer = new ReusableByteOutStream(127);
    private Writes outputBuffer = new Writes(new DataOutputStream(writeBuffer));

    /** Stream for writing player sync data to. */
    private ReusableByteOutStream syncStream = new ReusableByteOutStream();
    /** Data stream for writing player sync data to. */
    private DataOutputStream dataStream = new DataOutputStream(syncStream);
    /** Packet handlers for custom types of messages. */
    private ObjectMap<String, Seq<Cons2<Player, String>>> customPacketHandlers = new ObjectMap<>();

    public NetServer(){

        net.handleServer(Connect.class, (con, connect) -> {
            Events.fire(new ConnectionEvent(con));

            if(admins.isIPBanned(connect.addressTCP) || admins.isSubnetBanned(connect.addressTCP)){
                con.kick(KickReason.banned);
            }
        });

        net.handleServer(Disconnect.class, (con, packet) -> {
            if(con.player != null){
                onDisconnect(con.player, packet.reason);
            }
        });

        net.handleServer(ConnectPacket.class, (con, packet) -> {
            if(con.kicked) return;

            if(con.address.startsWith("steam:")){
                packet.uuid = con.address.substring("steam:".length());
            }

            Events.fire(new ConnectPacketEvent(con, packet));

            con.connectTime = Time.millis();

            String uuid = packet.uuid;

            if(admins.isIPBanned(con.address) || admins.isSubnetBanned(con.address)) return;

            if(con.hasBegunConnecting){
                con.kick(KickReason.idInUse);
                return;
            }

            PlayerInfo info = admins.getInfo(uuid);

            con.hasBegunConnecting = true;
            con.mobile = packet.mobile;

            if(packet.uuid == null || packet.usid == null){
                con.kick(KickReason.idInUse);
                return;
            }

            if(admins.isIDBanned(uuid)){
                con.kick(KickReason.banned);
                return;
            }

            if(Time.millis() < admins.getKickTime(uuid, con.address)){
                con.kick(KickReason.recentKick);
                return;
            }

            if(admins.getPlayerLimit() > 0 && Groups.player.size() >= admins.getPlayerLimit() && !netServer.admins.isAdmin(uuid, packet.usid)){
                con.kick(KickReason.playerLimit);
                return;
            }

            Seq<String> extraMods = packet.mods.copy();
            Seq<String> missingMods = mods.getIncompatibility(extraMods);

            if(!extraMods.isEmpty() || !missingMods.isEmpty()){
                //can't easily be localized since kick reasons can't have formatted text with them
                StringBuilder result = new StringBuilder("[accent]Incompatible mods![]\n\n");
                if(!missingMods.isEmpty()){
                    result.append("Missing:[lightgray]\n").append("> ").append(missingMods.toString("\n> "));
                    result.append("[]\n");
                }

                if(!extraMods.isEmpty()){
                    result.append("Unnecessary mods:[lightgray]\n").append("> ").append(extraMods.toString("\n> "));
                }
                con.kick(result.toString(), 0);
                return;
            }

            if(!admins.isWhitelisted(packet.uuid, packet.usid)){
                info.adminUsid = packet.usid;
                info.lastName = packet.name;
                info.id = packet.uuid;
                admins.save();
                Call.infoMessage(con, "You are not whitelisted here.");
                info("&lcDo &lywhitelist-add @&lc to whitelist the player &lb'@'", packet.uuid, packet.name);
                con.kick(KickReason.whitelist);
                return;
            }

            if(packet.versionType == null || ((packet.version == -1 || !packet.versionType.equals(Version.type)) && Version.build != -1 && !admins.allowsCustomClients())){
                con.kick(!Version.type.equals(packet.versionType) ? KickReason.typeMismatch : KickReason.customClient);
                return;
            }

            boolean preventDuplicates = headless && netServer.admins.isStrict();

            if(preventDuplicates){
                if(Groups.player.contains(p -> Strings.stripColors(p.name).trim().equalsIgnoreCase(Strings.stripColors(packet.name).trim()))){
                    con.kick(KickReason.nameInUse);
                    return;
                }

                if(Groups.player.contains(player -> player.uuid().equals(packet.uuid) || player.usid().equals(packet.usid))){
                    con.uuid = packet.uuid;
                    con.kick(KickReason.idInUse);
                    return;
                }

                for(var otherCon : net.getConnections()){
                    if(otherCon != con && uuid.equals(otherCon.uuid)){
                        con.uuid = packet.uuid;
                        con.kick(KickReason.idInUse);
                        return;
                    }
                }
            }

            packet.name = fixName(packet.name);

            if(packet.name.trim().length() <= 0){
                con.kick(KickReason.nameEmpty);
                return;
            }

            if(packet.locale == null){
                packet.locale = "en";
            }

            String ip = con.address;

            admins.updatePlayerJoined(uuid, ip, packet.name);

            if(packet.version != Version.build && Version.build != -1 && packet.version != -1){
                con.kick(packet.version > Version.build ? KickReason.serverOutdated : KickReason.clientOutdated);
                return;
            }

            if(packet.version == -1){
                con.modclient = true;
            }

            Player player = Player.create();
            player.admin = admins.isAdmin(uuid, packet.usid);
            player.con = con;
            player.con.usid = packet.usid;
            player.con.uuid = uuid;
            player.con.mobile = packet.mobile;
            player.name = packet.name;
            player.locale = packet.locale;
            player.color.set(packet.color).a(1f);

            //save admin ID but don't overwrite it
            if(!player.admin && !info.admin){
                info.adminUsid = packet.usid;
            }

            try{
                writeBuffer.reset();
                player.write(outputBuffer);
            }catch(Throwable t){
                con.kick(KickReason.nameEmpty);
                err(t);
                return;
            }

            con.player = player;

            //playing in pvp mode automatically assigns players to teams
            player.team(assignTeam(player));

            sendWorldData(player);

            platform.updateRPC();

            Events.fire(new PlayerConnect(player));
        });

        registerCommands();
    }

    @Override
    public void init(){
        mods.eachClass(mod -> mod.registerClientCommands(clientCommands));
    }

    private void registerCommands(){
        clientCommands.<Player>register("help", "[page]", "Lists all commands.", (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
            int commandsPerPage = 6;
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float)clientCommands.getCommandList().size / commandsPerPage);

            page--;

            if(page >= pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Commands Page[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));

            for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), clientCommands.getCommandList().size); i++){
                Command command = clientCommands.getCommandList().get(i);
                result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description).append("\n");
            }
            player.sendMessage(result.toString());
        });

        clientCommands.<Player>register("t", "<message...>", "Send a message only to your teammates.", (args, player) -> {
            String message = admins.filterMessage(player, args[0]);
            if(message != null){
                String raw = "[#" + player.team().color.toString() + "]<T> " + chatFormatter.format(player, message);
                Groups.player.each(p -> p.team() == player.team(), o -> o.sendMessage(raw, player, message));
            }
        });

        clientCommands.<Player>register("a", "<message...>", "Send a message only to admins.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]You must be an admin to use this command.");
                return;
            }

            String raw = "[#" + Pal.adminChat.toString() + "]<A> " + chatFormatter.format(player, args[0]);
            Groups.player.each(Player::admin, a -> a.sendMessage(raw, player, args[0]));
        });

        //duration of a kick in seconds
        int kickDuration = 60 * 60;
        //voting round duration in seconds
        float voteDuration = 0.5f * 60;
        //cooldown between votes in seconds
        int voteCooldown = 60 * 5;

        class VoteSession{
            Player target;
            ObjectSet<String> voted = new ObjectSet<>();
            VoteSession[] map;
            Timer.Task task;
            int votes;

            public VoteSession(VoteSession[] map, Player target){
                this.target = target;
                this.map = map;
                this.task = Timer.schedule(() -> {
                    if(!checkPass()){
                        Call.sendMessage(Strings.format("[lightgray]Vote failed. Not enough votes to kick[orange] @[lightgray].", target.name));
                        map[0] = null;
                        task.cancel();
                    }
                }, voteDuration);
            }

            void vote(Player player, int d){
                votes += d;
                voted.addAll(player.uuid(), admins.getInfo(player.uuid()).lastIP);

                Call.sendMessage(Strings.format("[lightgray]@[lightgray] has voted on kicking[orange] @[lightgray].[accent] (@/@)\n[lightgray]Type[orange] /vote <y/n>[] to agree.",
                    player.name, target.name, votes, votesRequired()));

                checkPass();
            }

            boolean checkPass(){
                if(votes >= votesRequired()){
                    Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] @[orange] will be banned from the server for @ minutes.", target.name, (kickDuration / 60)));
                    Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> p.kick(KickReason.vote, kickDuration * 1000));
                    map[0] = null;
                    task.cancel();
                    return true;
                }
                return false;
            }
        }

        //cooldowns per player
        ObjectMap<String, Timekeeper> cooldowns = new ObjectMap<>();
        //current kick sessions
        VoteSession[] currentlyKicking = {null};

        clientCommands.<Player>register("votekick", "[player...]", "Vote to kick a player.", (args, player) -> {
            if(!Config.enableVotekick.bool()){
                player.sendMessage("[scarlet]Vote-kick is disabled on this server.");
                return;
            }

            if(Groups.player.size() < 3){
                player.sendMessage("[scarlet]At least 3 players are needed to start a votekick.");
                return;
            }

            if(player.isLocal()){
                player.sendMessage("[scarlet]Just kick them yourself if you're the host.");
                return;
            }

            if(currentlyKicking[0] != null){
                player.sendMessage("[scarlet]A vote is already in progress.");
                return;
            }

            if(args.length == 0){
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]Players to kick: \n");

                Groups.player.each(p -> !p.admin && p.con != null && p != player, p -> {
                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(")\n");
                });
                player.sendMessage(builder.toString());
            }else{
                Player found;
                if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
                    int id = Strings.parseInt(args[0].substring(1));
                    found = Groups.player.find(p -> p.id() == id);
                }else{
                    found = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
                }

                if(found != null){
                    if(found == player){
                        player.sendMessage("[scarlet]You can't vote to kick yourself.");
                    }else if(found.admin){
                        player.sendMessage("[scarlet]Did you really expect to be able to kick an admin?");
                    }else if(found.isLocal()){
                        player.sendMessage("[scarlet]Local players cannot be kicked.");
                    }else if(found.team() != player.team()){
                        player.sendMessage("[scarlet]Only players on your team can be kicked.");
                    }else{
                        Timekeeper vtime = cooldowns.get(player.uuid(), () -> new Timekeeper(voteCooldown));

                        if(!vtime.get()){
                            player.sendMessage("[scarlet]You must wait " + voteCooldown/60 + " minutes between votekicks.");
                            return;
                        }

                        VoteSession session = new VoteSession(currentlyKicking, found);
                        session.vote(player, 1);
                        vtime.reset();
                        currentlyKicking[0] = session;
                    }
                }else{
                    player.sendMessage("[scarlet]No player [orange]'" + args[0] + "'[scarlet] found.");
                }
            }
        });

        clientCommands.<Player>register("vote", "<y/n>", "Vote to kick the current player.", (arg, player) -> {
            if(currentlyKicking[0] == null){
                player.sendMessage("[scarlet]Nobody is being voted on.");
            }else{
                if(player.isLocal()){
                    player.sendMessage("[scarlet]Local players can't vote. Kick the player yourself instead.");
                    return;
                }

                //hosts can vote all they want
                if((currentlyKicking[0].voted.contains(player.uuid()) || currentlyKicking[0].voted.contains(admins.getInfo(player.uuid()).lastIP))){
                    player.sendMessage("[scarlet]You've already voted. Sit down.");
                    return;
                }

                if(currentlyKicking[0].target == player){
                    player.sendMessage("[scarlet]You can't vote on your own trial.");
                    return;
                }

                if(currentlyKicking[0].target.team() != player.team()){
                    player.sendMessage("[scarlet]You can't vote for other teams.");
                    return;
                }

                int sign = switch(arg[0].toLowerCase()){
                    case "y", "yes" -> 1;
                    case "n", "no" -> -1;
                    default -> 0;
                };

                if(sign == 0){
                    player.sendMessage("[scarlet]Vote either 'y' (yes) or 'n' (no).");
                    return;
                }

                currentlyKicking[0].vote(player, sign);
            }
        });

        clientCommands.<Player>register("sync", "Re-synchronize world state.", (args, player) -> {
            if(player.isLocal()){
                player.sendMessage("[scarlet]Re-synchronizing as the host is pointless.");
            }else{
                if(Time.timeSinceMillis(player.getInfo().lastSyncTime) < 1000 * 5){
                    player.sendMessage("[scarlet]You may only /sync every 5 seconds.");
                    return;
                }

                player.getInfo().lastSyncTime = Time.millis();
                Call.worldDataBegin(player.con);
                netServer.sendWorldData(player);
            }
        });
    }

    public int votesRequired(){
        return 2 + (Groups.player.size() > 4 ? 1 : 0);
    }

    public Team assignTeam(Player current){
        return assigner.assign(current, Groups.player);
    }

    public Team assignTeam(Player current, Iterable<Player> players){
        return assigner.assign(current, players);
    }

    public void sendWorldData(Player player){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DeflaterOutputStream def = new FastDeflaterOutputStream(stream);
        NetworkIO.writeWorld(player, def);
        WorldStream data = new WorldStream();
        data.stream = new ByteArrayInputStream(stream.toByteArray());
        player.con.sendStream(data);

        debug("Packed @ bytes of world data.", stream.size());
    }

    public void addPacketHandler(String type, Cons2<Player, String> handler){
        customPacketHandlers.get(type, Seq::new).add(handler);
    }

    public Seq<Cons2<Player, String>> getPacketHandlers(String type){
        return customPacketHandlers.get(type, Seq::new);
    }

    public static void onDisconnect(Player player, String reason){
        //singleplayer multiplayer weirdness
        if(player.con == null){
            player.remove();
            return;
        }

        if(!player.con.hasDisconnected){
            if(player.con.hasConnected){
                Events.fire(new PlayerLeave(player));
                if(Config.showConnectMessages.bool()) Call.sendMessage("[accent]" + player.name + "[accent] has disconnected.");
                Call.playerDisconnect(player.id());
            }

            String message = Strings.format("&lb@&fi&lk has disconnected. [&lb@&fi&lk] (@)", player.plainName(), player.uuid(), reason);
            if(Config.showConnectMessages.bool()) info(message);
        }

        player.remove();
        player.con.hasDisconnected = true;
    }

    //these functions are for debugging only, and will be removed!

    @Remote(targets = Loc.client, variants = Variant.one)
    public static void requestDebugStatus(Player player){
        int flags =
            (player.con.hasDisconnected ? 1 : 0) |
            (player.con.hasConnected ? 2 : 0) |
            (player.isAdded() ? 4 : 0) |
            (player.con.hasBegunConnecting ? 8 : 0);

        Call.debugStatusClient(player.con, flags, player.con.lastReceivedClientSnapshot, player.con.snapshotsSent);
        Call.debugStatusClientUnreliable(player.con, flags, player.con.lastReceivedClientSnapshot, player.con.snapshotsSent);
    }

    @Remote(variants = Variant.both, priority = PacketPriority.high)
    public static void debugStatusClient(int value, int lastClientSnapshot, int snapshotsSent){
        logClientStatus(true, value, lastClientSnapshot, snapshotsSent);
    }

    @Remote(variants = Variant.both, priority = PacketPriority.high, unreliable = true)
    public static void debugStatusClientUnreliable(int value, int lastClientSnapshot, int snapshotsSent){
        logClientStatus(false, value, lastClientSnapshot, snapshotsSent);
    }

    static void logClientStatus(boolean reliable, int value, int lastClientSnapshot, int snapshotsSent){
        Log.info("@ Debug status received. disconnected = @, connected = @, added = @, begunConnecting = @ lastClientSnapshot = @, snapshotsSent = @",
        reliable ? "[RELIABLE]" : "[UNRELIABLE]",
        (value & 1) != 0, (value & 2) != 0, (value & 4) != 0, (value & 8) != 0,
        lastClientSnapshot, snapshotsSent
        );
    }

    @Remote(targets = Loc.client)
    public static void serverPacketReliable(Player player, String type, String contents){
        if(netServer.customPacketHandlers.containsKey(type)){
            for(Cons2<Player, String> c : netServer.customPacketHandlers.get(type)){
                c.get(player, contents);
            }
        }
    }

    @Remote(targets = Loc.client, unreliable = true)
    public static void serverPacketUnreliable(Player player, String type, String contents){
        serverPacketReliable(player, type, contents);
    }

    private static boolean invalid(float f){
        return Float.isInfinite(f) || Float.isNaN(f);
    }

    @Remote(targets = Loc.client, unreliable = true)
    public static void clientSnapshot(
        Player player,
        int snapshotID,
        int unitID,
        boolean dead,
        float x, float y,
        float pointerX, float pointerY,
        float rotation, float baseRotation,
        float xVelocity, float yVelocity,
        Tile mining,
        boolean boosting, boolean shooting, boolean chatting, boolean building,
        @Nullable Queue<BuildPlan> plans,
        float viewX, float viewY, float viewWidth, float viewHeight
    ){
        NetConnection con = player.con;
        if(con == null || snapshotID < con.lastReceivedClientSnapshot) return;

        //validate coordinates just in case
        if(invalid(x)) x = 0f;
        if(invalid(y)) y = 0f;
        if(invalid(xVelocity)) xVelocity = 0f;
        if(invalid(yVelocity)) yVelocity = 0f;
        if(invalid(pointerX)) pointerX = 0f;
        if(invalid(pointerY)) pointerY = 0f;
        if(invalid(rotation)) rotation = 0f;
        if(invalid(baseRotation)) baseRotation = 0f;

        boolean verifyPosition = netServer.admins.isStrict() && headless;

        if(con.lastReceivedClientTime == 0) con.lastReceivedClientTime = Time.millis() - 16;

        con.viewX = viewX;
        con.viewY = viewY;
        con.viewWidth = viewWidth;
        con.viewHeight = viewHeight;

        //disable shooting when a mech flies
        if(!player.dead() && player.unit().isFlying() && player.unit() instanceof Mechc){
            shooting = false;
        }

        if(!player.dead() && (player.unit().type.flying || !player.unit().type.canBoost)){
            boosting = false;
        }

        player.mouseX = pointerX;
        player.mouseY = pointerY;
        player.typing = chatting;
        player.shooting = shooting;
        player.boosting = boosting;

        player.unit().controlWeapons(shooting, shooting);
        player.unit().aim(pointerX, pointerY);

        if(player.isBuilder()){
            player.unit().clearBuilding();
            player.unit().updateBuilding(building);

            if(plans != null){
                for(BuildPlan req : plans){
                    if(req == null) continue;
                    Tile tile = world.tile(req.x, req.y);
                    if(tile == null || (!req.breaking && req.block == null)) continue;
                    //auto-skip done requests
                    if(req.breaking && tile.block() == Blocks.air){
                        continue;
                    }else if(!req.breaking && tile.block() == req.block && (!req.block.rotate || (tile.build != null && tile.build.rotation == req.rotation))){
                        continue;
                    }else if(con.rejectedRequests.contains(r -> r.breaking == req.breaking && r.x == req.x && r.y == req.y)){ //check if request was recently rejected, and skip it if so
                        continue;
                    }else if(!netServer.admins.allowAction(player, req.breaking ? ActionType.breakBlock : ActionType.placeBlock, tile, action -> { //make sure request is allowed by the server
                        action.block = req.block;
                        action.rotation = req.rotation;
                        action.config = req.config;
                    })){
                        //force the player to remove this request if that's not the case
                        Call.removeQueueBlock(player.con, req.x, req.y, req.breaking);
                        con.rejectedRequests.add(req);
                        continue;
                    }
                    player.unit().plans().addLast(req);
                }
            }
        }

        player.unit().mineTile = mining;

        con.rejectedRequests.clear();

        if(!player.dead()){
            Unit unit = player.unit();

            long elapsed = Math.min(Time.timeSinceMillis(con.lastReceivedClientTime), 1500);
            float maxSpeed = unit.speed();

            float maxMove = elapsed / 1000f * 60f * maxSpeed * 1.2f;

            //ignore the position if the player thinks they're dead, or the unit is wrong
            boolean ignorePosition = dead || unit.id != unitID;
            float newx = unit.x, newy = unit.y;

            if(!ignorePosition){
                unit.vel.set(xVelocity, yVelocity).limit(maxSpeed);

                vector.set(x, y).sub(unit);
                vector.limit(maxMove);

                float prevx = unit.x, prevy = unit.y;
                //unit.set(con.lastPosition);
                if(!unit.isFlying()){
                    unit.move(vector.x, vector.y);
                }else{
                    unit.trns(vector.x, vector.y);
                }

                newx = unit.x;
                newy = unit.y;

                if(!verifyPosition){
                    unit.set(prevx, prevy);
                    newx = x;
                    newy = y;
                }else if(!Mathf.within(x, y, newx, newy, correctDist)){
                    Call.setPosition(player.con, newx, newy); //teleport and correct position when necessary
                }
            }

            //write sync data to the buffer
            fbuffer.limit(20);
            fbuffer.position(0);

            //now, put the new position, rotation and baserotation into the buffer so it can be read
            //TODO this is terrible
            if(unit instanceof Mechc) fbuffer.put(baseRotation); //base rotation is optional
            fbuffer.put(rotation); //rotation is always there
            fbuffer.put(newx);
            fbuffer.put(newy);
            fbuffer.flip();

            //read sync data so it can be used for interpolation for the server
            unit.readSyncManual(fbuffer);
        }else{
            player.x = x;
            player.y = y;
        }

        con.lastReceivedClientSnapshot = snapshotID;
        con.lastReceivedClientTime = Time.millis();
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void adminRequest(Player player, Player other, AdminAction action){
        if(!player.admin && !player.isLocal()){
            warn("ACCESS DENIED: Player @ / @ attempted to perform admin action '@' on '@' without proper security access.",
            player.plainName(), player.con == null ? "null" : player.con.address, action.name(), other == null ? null : other.plainName());
            return;
        }

        if(other == null || ((other.admin && !player.isLocal()) && other != player)){
            warn("@ &fi&lk[&lb@&fi&lk]&fb attempted to perform admin action on nonexistant or admin player.", player.plainName(), player.uuid());
            return;
        }

        Events.fire(new EventType.AdminRequestEvent(player, other, action));

        if(action == AdminAction.wave){
            //no verification is done, so admins can hypothetically spam waves
            //not a real issue, because server owners may want to do just that
            logic.skipWave();
            info("&lc@ &fi&lk[&lb@&fi&lk]&fb has skipped the wave.", player.plainName(), player.uuid());
        }else if(action == AdminAction.ban){
            netServer.admins.banPlayerID(other.con.uuid);
            netServer.admins.banPlayerIP(other.con.address);
            other.kick(KickReason.banned);
            info("&lc@ &fi&lk[&lb@&fi&lk]&fb has banned @ &fi&lk[&lb@&fi&lk]&fb.", player.plainName(), player.uuid(), other.plainName(), other.uuid());
        }else if(action == AdminAction.kick){
            other.kick(KickReason.kick);
            info("&lc@ &fi&lk[&lb@&fi&lk]&fb has kicked @ &fi&lk[&lb@&fi&lk]&fb.", player.plainName(), player.uuid(), other.plainName(), other.uuid());
        }else if(action == AdminAction.trace){
            PlayerInfo stats = netServer.admins.getInfo(other.uuid());
            TraceInfo info = new TraceInfo(other.con.address, other.uuid(), other.con.modclient, other.con.mobile, stats.timesJoined, stats.timesKicked);
            if(player.con != null){
                Call.traceInfo(player.con, other, info);
            }else{
                NetClient.traceInfo(other, info);
            }
            info("&lc@ &fi&lk[&lb@&fi&lk]&fb has requested trace info of @ &fi&lk[&lb@&fi&lk]&fb.", player.plainName(), player.uuid(), other.plainName(), other.uuid());
        }
    }

    @Remote(targets = Loc.client)
    public static void connectConfirm(Player player){
        if(player.con.kicked) return;

        player.add();

        Events.fire(new PlayerConnectionConfirmed(player));

        if(player.con == null || player.con.hasConnected) return;

        player.con.hasConnected = true;

        if(Config.showConnectMessages.bool()){
            Call.sendMessage("[accent]" + player.name + "[accent] has connected.");
            String message = Strings.format("&lb@&fi&lk has connected. &fi&lk[&lb@&fi&lk]", player.plainName(), player.uuid());
            info(message);
        }

        if(!Config.motd.string().equalsIgnoreCase("off")){
            player.sendMessage(Config.motd.string());
        }

        Events.fire(new PlayerJoin(player));
    }

    public boolean isWaitingForPlayers(){
        if(state.rules.pvp && !state.gameOver){
            int used = 0;
            for(TeamData t : state.teams.getActive()){
                if(Groups.player.count(p -> p.team() == t.team) > 0){
                    used++;
                }
            }
            return used < 2;
        }
        return false;
    }

    @Override
    public void update(){
        if(!headless && !closing && net.server() && state.isMenu()){
            closing = true;
            ui.loadfrag.show("@server.closing");
            Time.runTask(5f, () -> {
                net.closeServer();
                ui.loadfrag.hide();
                closing = false;
            });
        }

        if(state.isGame() && net.server()){
            if(state.rules.pvp && state.rules.pvpAutoPause){
                boolean waiting = isWaitingForPlayers(), paused = state.isPaused();
                if(waiting != paused){
                    if(waiting){
                        //is now waiting, enable pausing, flag it correctly
                        pvpAutoPaused = true;
                        state.set(State.paused);
                    }else if(pvpAutoPaused){
                        //no longer waiting, stop pausing
                        state.set(State.playing);
                        pvpAutoPaused = false;
                    }
                }
            }

            sync();
        }
    }

    //TODO I don't like where this is, move somewhere else?
    /** Queues a building health update. This will be sent in a Call.buildHealthUpdate packet later. */
    public void buildHealthUpdate(Building build){
        buildHealthChanged.add(build.pos());
    }

    /** Should only be used on the headless backend. */
    public void openServer(){
        try{
            net.host(Config.port.num());
            info("Opened a server on port @.", Config.port.num());
        }catch(BindException e){
            err("Unable to host: Port " + Config.port.num() + " already in use! Make sure no other servers are running on the same port in your network.");
            state.set(State.menu);
        }catch(IOException e){
            err(e);
            state.set(State.menu);
        }
    }

    public void kickAll(KickReason reason){
        for(NetConnection con : net.getConnections()){
            con.kick(reason);
        }
    }

    /** Sends a block snapshot to all players. */
    public void writeBlockSnapshots() throws IOException{
        syncStream.reset();

        short sent = 0;
        for(Building entity : Groups.build){
            if(!entity.block.sync) continue;
            sent++;

            dataStream.writeInt(entity.pos());
            dataStream.writeShort(entity.block.id);
            entity.writeAll(Writes.get(dataStream));

            if(syncStream.size() > maxSnapshotSize){
                dataStream.close();
                Call.blockSnapshot(sent, syncStream.toByteArray());
                sent = 0;
                syncStream.reset();
            }
        }

        if(sent > 0){
            dataStream.close();
            Call.blockSnapshot(sent, syncStream.toByteArray());
        }
    }

    public void writeEntitySnapshot(Player player) throws IOException{
        byte tps = (byte)Math.min(Core.graphics.getFramesPerSecond(), 255);
        syncStream.reset();
        int activeTeams = (byte)state.teams.present.count(t -> t.cores.size > 0);

        dataStream.writeByte(activeTeams);
        dataWrites.output = dataStream;

        //block data isn't important, just send the items for each team, they're synced across cores
        for(TeamData data : state.teams.present){
            if(data.cores.size > 0){
                dataStream.writeByte(data.team.id);
                data.cores.first().items.write(dataWrites);
            }
        }

        dataStream.close();

        //write basic state data.
        Call.stateSnapshot(player.con, state.wavetime, state.wave, state.enemies, state.isPaused(), state.gameOver,
        universe.seconds(), tps, GlobalVars.rand.seed0, GlobalVars.rand.seed1, syncStream.toByteArray());

        syncStream.reset();

        hiddenIds.clear();
        int sent = 0;

        for(Syncc entity : Groups.sync){
            //TODO write to special list
            if(entity.isSyncHidden(player)){
                hiddenIds.add(entity.id());
                continue;
            }

            //write all entities now
            dataStream.writeInt(entity.id()); //write id
            dataStream.writeByte(entity.classId()); //write type ID
            entity.writeSync(Writes.get(dataStream)); //write entity

            sent++;

            if(syncStream.size() > maxSnapshotSize){
                dataStream.close();
                Call.entitySnapshot(player.con, (short)sent, syncStream.toByteArray());
                sent = 0;
                syncStream.reset();
            }
        }

        if(sent > 0){
            dataStream.close();

            Call.entitySnapshot(player.con, (short)sent, syncStream.toByteArray());
        }

        if(hiddenIds.size > 0){
            Call.hiddenSnapshot(player.con, hiddenIds);
        }

        player.con.snapshotsSent++;
    }

    public String fixName(String name){
        name = name.trim().replace("\n", "").replace("\t", "");
        if(name.equals("[") || name.equals("]")){
            return "";
        }

        for(int i = 0; i < name.length(); i++){
            if(name.charAt(i) == '[' && i != name.length() - 1 && name.charAt(i + 1) != '[' && (i == 0 || name.charAt(i - 1) != '[')){
                String prev = name.substring(0, i);
                String next = name.substring(i);
                String result = checkColor(next);

                name = prev + result;
            }
        }

        StringBuilder result = new StringBuilder();
        int curChar = 0;
        while(curChar < name.length() && result.toString().getBytes(Strings.utf8).length < maxNameLength){
            result.append(name.charAt(curChar++));
        }
        return result.toString();
    }

    public String checkColor(String str){
        for(int i = 1; i < str.length(); i++){
            if(str.charAt(i) == ']'){
                String color = str.substring(1, i);

                if(Colors.get(color.toUpperCase()) != null || Colors.get(color.toLowerCase()) != null){
                    Color result = (Colors.get(color.toLowerCase()) == null ? Colors.get(color.toUpperCase()) : Colors.get(color.toLowerCase()));
                    if(result.a < 1f){
                        return str.substring(i + 1);
                    }
                }else{
                    try{
                        Color result = Color.valueOf(color);
                        if(result.a < 1f){
                            return str.substring(i + 1);
                        }
                    }catch(Exception e){
                        return str;
                    }
                }
            }
        }
        return str;
    }

    void sync(){
        try{
            int interval = Config.snapshotInterval.num();
            Groups.player.each(p -> !p.isLocal(), player -> {
                if(player.con == null || !player.con.isConnected()){
                    onDisconnect(player, "disappeared");
                    return;
                }

                var connection = player.con;

                if(Time.timeSinceMillis(connection.syncTime) < interval || !connection.hasConnected) return;

                connection.syncTime = Time.millis();

                try{
                    writeEntitySnapshot(player);
                }catch(IOException e){
                    e.printStackTrace();
                }
            });

            if(Groups.player.size() > 0 && Core.settings.getBool("blocksync") && timer.get(timerBlockSync, blockSyncTime)){
                writeBlockSnapshots();
            }

            if(Groups.player.size() > 0 && buildHealthChanged.size > 0 && timer.get(timerHealthSync, healthSyncTime)){
                healthSeq.clear();

                var iter = buildHealthChanged.iterator();
                while(iter.hasNext){
                    int next = iter.next();
                    var build = world.build(next);

                    //pack pos + health into update list
                    if(build != null){
                        healthSeq.add(next, Float.floatToRawIntBits(build.health));
                    }

                    //if size exceeds snapshot limit, send it out and begin building it up again
                    if(healthSeq.size * 4 >= maxSnapshotSize){
                        Call.buildHealthUpdate(healthSeq);
                        healthSeq.clear();
                    }
                }

                //send any residual health updates
                if(healthSeq.size > 0){
                    Call.buildHealthUpdate(healthSeq);
                }

                buildHealthChanged.clear();
            }
        }catch(IOException e){
            Log.err(e);
        }
    }

    public interface TeamAssigner{
        Team assign(Player player, Iterable<Player> players);
    }

    public interface ChatFormatter{
        /** @return text to be placed before player name */
        String format(@Nullable Player player, String message);
    }

    public interface InvalidCommandHandler{
        String handle(Player player, CommandResponse response);
    }
}
