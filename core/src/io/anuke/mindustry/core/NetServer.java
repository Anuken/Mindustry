package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.CommandHandler.*;
import io.anuke.arc.util.io.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Administration.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.world.*;

import java.io.*;
import java.nio.*;
import java.util.zip.*;

import static io.anuke.mindustry.Vars.*;

public class NetServer implements ApplicationListener{
    public final static int maxSnapshotSize = 430;
    private final static float serverSyncTime = 12, kickDuration = 30 * 1000;
    private final static Vector2 vector = new Vector2();
    private final static Rectangle viewport = new Rectangle();
    /** If a player goes away of their server-side coordinates by this distance, they get teleported back. */
    private final static float correctDist = 16f;

    public final Administration admins = new Administration();
    public final CommandHandler clientCommands = new CommandHandler("/");

    private boolean closing = false;

    private ByteBuffer writeBuffer = ByteBuffer.allocate(127);
    private ByteBufferOutput outputBuffer = new ByteBufferOutput(writeBuffer);

    /** Stream for writing player sync data to. */
    private ReusableByteOutStream syncStream = new ReusableByteOutStream();
    /** Data stream for writing player sync data to. */
    private DataOutputStream dataStream = new DataOutputStream(syncStream);

    public NetServer(){

        net.handleServer(Connect.class, (con, connect) -> {
            if(admins.isIPBanned(connect.addressTCP)){
                con.kick(KickReason.banned);
            }
        });

        net.handleServer(Disconnect.class, (con, packet) -> {
            if(con.player != null){
                onDisconnect(con.player, packet.reason);
            }
        });

        net.handleServer(ConnectPacket.class, (con, packet) -> {
            String uuid = packet.uuid;

            if(admins.isIPBanned(con.address)) return;

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

            if(Time.millis() - info.lastKicked < kickDuration){
                con.kick(KickReason.recentKick);
                return;
            }

            if(admins.getPlayerLimit() > 0 && playerGroup.size() >= admins.getPlayerLimit()){
                con.kick(KickReason.playerLimit);
                return;
            }

            Array<String> extraMods = packet.mods.copy();
            Array<String> missingMods = mods.getIncompatibility(extraMods);

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
                con.kick(result.toString());
            }

            if(!admins.isWhitelisted(packet.uuid, packet.usid)){
                info.adminUsid = packet.usid;
                info.lastName = packet.name;
                info.id = packet.uuid;
                admins.save();
                Call.onInfoMessage(con, "You are not whitelisted here.");
                Log.info("&lcDo &lywhitelist-add {0}&lc to whitelist the player &lb'{1}'", packet.uuid, packet.name);
                con.kick(KickReason.whitelist);
                return;
            }

            if(packet.versionType == null || ((packet.version == -1 || !packet.versionType.equals(Version.type)) && Version.build != -1 && !admins.allowsCustomClients())){
                con.kick(!Version.type.equals(packet.versionType) ? KickReason.typeMismatch : KickReason.customClient);
                return;
            }

            boolean preventDuplicates = headless && netServer.admins.getStrict();

            if(preventDuplicates){
                for(Player player : playerGroup.all()){
                    if(player.name.trim().equalsIgnoreCase(packet.name.trim())){
                        con.kick(KickReason.nameInUse);
                        return;
                    }

                    if(player.uuid != null && player.usid != null && (player.uuid.equals(packet.uuid) || player.usid.equals(packet.usid))){
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

            String ip = con.address;

            admins.updatePlayerJoined(uuid, ip, packet.name);

            if(packet.version != Version.build && Version.build != -1 && packet.version != -1){
                con.kick(packet.version > Version.build ? KickReason.serverOutdated : KickReason.clientOutdated);
                return;
            }

            if(packet.version == -1){
                con.modclient = true;
            }

            Player player = new Player();
            player.isAdmin = admins.isAdmin(uuid, packet.usid);
            player.con = con;
            player.usid = packet.usid;
            player.name = packet.name;
            player.uuid = uuid;
            player.isMobile = packet.mobile;
            player.dead = true;
            player.setNet(player.x, player.y);
            player.color.set(packet.color);
            player.color.a = 1f;

            try{
                writeBuffer.position(0);
                player.write(outputBuffer);
            }catch(Throwable t){
                t.printStackTrace();
                con.kick(KickReason.nameEmpty);
                return;
            }

            con.player = player;

            //playing in pvp mode automatically assigns players to teams
            if(state.rules.pvp){
                player.setTeam(assignTeam(player, playerGroup.all()));
                Log.info("Auto-assigned player {0} to team {1}.", player.name, player.getTeam());
            }

            sendWorldData(player);

            platform.updateRPC();

            Events.fire(new PlayerConnect(player));
        });

        net.handleServer(InvokePacket.class, (con, packet) -> {
            if(con.player == null) return;
            RemoteReadServer.readPacket(packet.writeBuffer, packet.type, con.player);
        });

        registerCommands();
    }

    @Override
    public void init(){
        mods.each(mod -> mod.registerClientCommands(clientCommands));
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

            page --;

            if(page > pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Commands Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n\n", (page+1), pages));

            for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), clientCommands.getCommandList().size); i++){
                Command command = clientCommands.getCommandList().get(i);
                result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description).append("\n");
            }
            player.sendMessage(result.toString());
        });

        clientCommands.<Player>register("t", "<message...>", "Send a message only to your teammates.", (args, player) -> {
            playerGroup.all().each(p -> p.getTeam() == player.getTeam(), o -> o.sendMessage(args[0], player, "[#" + player.getTeam().color.toString() + "]<T>" + NetClient.colorizeName(player.id, player.name)));
        });

        //duration of a a kick in seconds
        int kickDuration = 15 * 60;

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
                        Call.sendMessage(Strings.format("[lightgray]Vote failed. Not enough votes to kick[orange] {0}[lightgray].", target.name));
                        map[0] = null;
                        task.cancel();
                    }
                }, 60 * 1);
            }

            void vote(Player player, int d){
                votes += d;
                voted.addAll(player.uuid, admins.getInfo(player.uuid).lastIP);
                        
                Call.sendMessage(Strings.format("[orange]{0}[lightgray] has voted to kick[orange] {1}[].[accent] ({2}/{3})\n[lightgray]Type[orange] /vote <y/n>[] to agree.",
                            player.name, target.name, votes, votesRequired()));
            }

            boolean checkPass(){
                if(votes >= votesRequired()){
                    Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] {0}[orange] will be banned from the server for {1} minutes.", target.name, (kickDuration/60)));
                    target.getInfo().lastKicked = Time.millis() + kickDuration*1000;
                    playerGroup.all().each(p -> p.uuid != null && p.uuid.equals(target.uuid), p -> p.con.kick(KickReason.vote));
                    map[0] = null;
                    task.cancel();
                    return true;
                }
                return false;
            }
        }

        //cooldown between votes
        int voteTime = 60 * 5;
        Timekeeper vtime = new Timekeeper(voteTime);
        //current kick sessions
        VoteSession[] currentlyKicking = {null};

        clientCommands.<Player>register("votekick", "[player...]", "Vote to kick a player, with a cooldown.", (args, player) -> {
            if(playerGroup.size() < 3){
                player.sendMessage("[scarlet]At least 3 players are needed to start a votekick.");
                return;
            }

            if(player.isLocal){
                player.sendMessage("[scarlet]Just kick them yourself if you're the host.");
                return;
            }

            if(args.length == 0){
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]Players to kick: \n");
                for(Player p : playerGroup.all()){
                    if(p.isAdmin || p.con == null || p == player) continue;

                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id).append(")\n");
                }
                player.sendMessage(builder.toString());
            }else{
                Player found;
                if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
                    int id = Strings.parseInt(args[0].substring(1));
                    found = playerGroup.find(p -> p.id == id);
                }else{
                    found = playerGroup.find(p -> p.name.equalsIgnoreCase(args[0]));
                }

                if(found != null){
                    if(found.isAdmin){
                        player.sendMessage("[scarlet]Did you really expect to be able to kick an admin?");
                    }else if(found.isLocal){
                        player.sendMessage("[scarlet]Local players cannot be kicked.");
                    }else{
                        if(!vtime.get()){
                            player.sendMessage("[scarlet]You must wait " + voteTime/60 + " minutes between votekicks.");
                            return;
                        }

                        VoteSession session = new VoteSession(currentlyKicking, found);
                        session.vote(player, 1);
                        vtime.reset();                  
                        currentlyKicking[0] = session;
                    }
                }else{
                    player.sendMessage("[scarlet]No player[orange]'" + args[0] + "'[scarlet] found.");
                }
            }
        });

        clientCommands.<Player>register("vote", "<y/n>", "Vote to kick the current player.", (arg, player) -> {
            if(currentlyKicking[0] == null){
                player.sendMessage("[scarlet]Nobody is being voted on.");
            }else{
                //hosts can vote all they want
                if(player.uuid != null && (currentlyKicking[0].voted.contains(player.uuid) || currentlyKicking[0].voted.contains(admins.getInfo(player.uuid).lastIP))){
                    player.sendMessage("[scarlet]You've already voted. Sit down.");
                    return;
                }

                if(currentlyKicking[0].target == player){
                    player.sendMessage("[scarlet]You can't vote on your own trial.");
                    return;
                }

                if(!arg[0].toLowerCase().equals("y") && !arg[0].toLowerCase().equals("n")){
                    player.sendMessage("[scarlet]Vote either 'y' (yes) or 'n' (no).");
                    return;
                }

                int sign = arg[0].toLowerCase().equals("y") ? 1 : -1;
                currentlyKicking[0].vote(player, sign);
            }
        });


        clientCommands.<Player>register("sync", "Re-synchronize world state.", (args, player) -> {
            if(player.isLocal){
                player.sendMessage("[scarlet]Re-synchronizing as the host is pointless.");
            }else{
                Call.onWorldDataBegin(player.con);
                netServer.sendWorldData(player);
            }
        });
    }

    public int votesRequired(){
        return 2 + (playerGroup.size() > 4 ? 1 : 0);
    }

    public Team assignTeam(Player current, Iterable<Player> players){
        //find team with minimum amount of players and auto-assign player to that.
        return Structs.findMin(Team.all, team -> {
            if(state.teams.isActive(team) && !state.teams.get(team).cores.isEmpty()){
                int count = 0;
                for(Player other : players){
                    if(other.getTeam() == team && other != current){
                        count++;
                    }
                }
                return count;
            }
            return Integer.MAX_VALUE;
        });
    }

    public void sendWorldData(Player player){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DeflaterOutputStream def = new FastDeflaterOutputStream(stream);
        NetworkIO.writeWorld(player, def);
        WorldStream data = new WorldStream();
        data.stream = new ByteArrayInputStream(stream.toByteArray());
        player.con.sendStream(data);

        Log.debug("Packed {0} compressed bytes of world data.", stream.size());
    }

    public static void onDisconnect(Player player, String reason){
        //singleplayer multiplayer wierdness
        if(player.con == null){
            player.remove();
            return;
        }

        if(!player.con.hasDisconnected){
            if(player.con.hasConnected){
                Events.fire(new PlayerLeave(player));
                Call.sendMessage("[accent]" + player.name + "[accent] has disconnected.");
                Call.onPlayerDisconnect(player.id);
            }

            Log.info("&lm[{1}] &lc{0} has disconnected. &lg&fi({2})", player.name, player.uuid, reason);
        }

        player.remove();
        player.con.hasDisconnected = true;
    }

    @Remote(targets = Loc.client, unreliable = true)
    public static void onClientShapshot(
        Player player,
        int snapshotID,
        float x, float y,
        float pointerX, float pointerY,
        float rotation, float baseRotation,
        float xVelocity, float yVelocity,
        Tile mining,
        boolean boosting, boolean shooting, boolean chatting,
        BuildRequest[] requests,
        float viewX, float viewY, float viewWidth, float viewHeight
    ){
        NetConnection connection = player.con;
        if(connection == null || snapshotID < connection.lastRecievedClientSnapshot) return;

        boolean verifyPosition = !player.isDead() && netServer.admins.getStrict() && headless;

        if(connection.lastRecievedClientTime == 0) connection.lastRecievedClientTime = Time.millis() - 16;

        connection.viewX = viewX;
        connection.viewY = viewY;
        connection.viewWidth = viewWidth;
        connection.viewHeight = viewHeight;

        long elapsed = Time.timeSinceMillis(connection.lastRecievedClientTime);

        float maxSpeed = boosting && !player.mech.flying ? player.mech.compoundSpeedBoost : player.mech.compoundSpeed;
        float maxMove = elapsed / 1000f * 60f * Math.min(maxSpeed, player.mech.maxSpeed) * 1.2f;

        player.pointerX = pointerX;
        player.pointerY = pointerY;
        player.setMineTile(mining);
        player.isTyping = chatting;
        player.isBoosting = boosting;
        player.isShooting = shooting;
        player.buildQueue().clear();
        for(BuildRequest req : requests){
            if(req == null) continue;
            Tile tile = world.tile(req.x, req.y);
            if(tile == null) continue;
            //auto-skip done requests
            if(req.breaking && tile.block() == Blocks.air){
                continue;
            }else if(!req.breaking && tile.block() == req.block && (!req.block.rotate || tile.rotation() == req.rotation)){
                continue;
            }
            player.buildQueue().addLast(req);
        }

        vector.set(x - player.getInterpolator().target.x, y - player.getInterpolator().target.y);
        vector.limit(maxMove);

        float prevx = player.x, prevy = player.y;
        player.set(player.getInterpolator().target.x, player.getInterpolator().target.y);
        if(!player.mech.flying && player.boostHeat < 0.01f){
            player.move(vector.x, vector.y);
        }else{
            player.x += vector.x;
            player.y += vector.y;
        }
        float newx = player.x, newy = player.y;

        if(!verifyPosition){
            player.x = prevx;
            player.y = prevy;
            newx = x;
            newy = y;
        }else if(Mathf.dst(x, y, newx, newy) > correctDist){
            Call.onPositionSet(player.con, newx, newy); //teleport and correct position when necessary
        }

        //reset player to previous synced position so it gets interpolated
        player.x = prevx;
        player.y = prevy;

        //set interpolator target to *new* position so it moves toward it
        player.getInterpolator().read(player.x, player.y, newx, newy, rotation, baseRotation);
        player.velocity().set(xVelocity, yVelocity); //only for visual calculation purposes, doesn't actually update the player

        connection.lastRecievedClientSnapshot = snapshotID;
        connection.lastRecievedClientTime = Time.millis();
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void onAdminRequest(Player player, Player other, AdminAction action){

        if(!player.isAdmin){
            Log.warn("ACCESS DENIED: Player {0} / {1} attempted to perform admin action without proper security access.",
            player.name, player.con.address);
            return;
        }

        if(other == null || ((other.isAdmin && !player.isLocal) && other != player)){
            Log.warn("{0} attempted to perform admin action on nonexistant or admin player.", player.name);
            return;
        }

        if(action == AdminAction.wave){
            //no verification is done, so admins can hypothetically spam waves
            //not a real issue, because server owners may want to do just that
            state.wavetime = 0f;
        }else if(action == AdminAction.ban){
            netServer.admins.banPlayerIP(other.con.address);
            other.con.kick(KickReason.banned);
            Log.info("&lc{0} has banned {1}.", player.name, other.name);
        }else if(action == AdminAction.kick){
            other.con.kick(KickReason.kick);
            Log.info("&lc{0} has kicked {1}.", player.name, other.name);
        }else if(action == AdminAction.trace){
            TraceInfo info = new TraceInfo(other.con.address, other.uuid, other.con.modclient, other.con.mobile);
            if(player.con != null){
                Call.onTraceInfo(player.con, other, info);
            }else{
                NetClient.onTraceInfo(other, info);
            }
            Log.info("&lc{0} has requested trace info of {1}.", player.name, other.name);
        }
    }

    @Remote(targets = Loc.client)
    public static void connectConfirm(Player player){
        if(player.con == null || player.con.hasConnected) return;

        player.add();
        player.con.hasConnected = true;
        Call.sendMessage("[accent]" + player.name + "[accent] has connected.");
        Log.info("&lm[{1}] &y{0} has connected. ", player.name, player.uuid);

        Events.fire(new PlayerJoin(player));
    }

    public boolean isWaitingForPlayers(){
        if(state.rules.pvp){
            int used = 0;
            for(Team t : Team.all){
                if(playerGroup.count(p -> p.getTeam() == t) > 0){
                    used++;
                }
            }
            return used < 2;
        }
        return false;
    }

    public void update(){

        if(!headless && !closing && net.server() && state.is(State.menu)){
            closing = true;
            ui.loadfrag.show("$server.closing");
            Time.runTask(5f, () -> {
                net.closeServer();
                ui.loadfrag.hide();
                closing = false;
            });
        }

        if(!state.is(State.menu) && net.server()){
            sync();
        }
    }

    public void kickAll(KickReason reason){
        for(NetConnection con : net.getConnections()){
            con.kick(reason);
        }
    }

    public void writeSnapshot(Player player) throws IOException{
        syncStream.reset();
        ObjectSet<Tile> cores = state.teams.get(player.getTeam()).cores;

        dataStream.writeByte(cores.size);

        for(Tile tile : cores){
            dataStream.writeInt(tile.pos());
            tile.entity.items.write(dataStream);
        }

        dataStream.close();
        byte[] stateBytes = syncStream.toByteArray();

        //write basic state data.
        Call.onStateSnapshot(player.con, state.wavetime, state.wave, state.enemies(), (short)stateBytes.length, net.compressSnapshot(stateBytes));

        viewport.setSize(player.con.viewWidth, player.con.viewHeight).setCenter(player.con.viewX, player.con.viewY);

        //check for syncable groups
        for(EntityGroup<?> group : entities.all()){
            if(group.isEmpty() || !(group.all().get(0) instanceof SyncTrait)) continue;

            //make sure mapping is enabled for this group
            if(!group.mappingEnabled()){
                throw new RuntimeException("Entity group '" + group.getType() + "' contains SyncTrait entities, yet mapping is not enabled. In order for syncing to work, you must enable mapping for this group.");
            }

            syncStream.reset();

            int sent = 0;

            for(Entity entity :  group.all()){
                SyncTrait sync = (SyncTrait)entity;
                if(!sync.isSyncing()) continue;

                //write all entities now
                dataStream.writeInt(entity.getID()); //write id
                dataStream.writeByte(sync.getTypeID().id); //write type ID
                sync.write(dataStream); //write entity

                sent++;

                if(syncStream.size() > maxSnapshotSize){
                    dataStream.close();
                    byte[] syncBytes = syncStream.toByteArray();
                    Call.onEntitySnapshot(player.con, (byte)group.getID(), (short)sent, (short)syncBytes.length, net.compressSnapshot(syncBytes));
                    sent = 0;
                    syncStream.reset();
                }
            }

            if(sent > 0){
                dataStream.close();

                byte[] syncBytes = syncStream.toByteArray();
                Call.onEntitySnapshot(player.con, (byte)group.getID(), (short)sent, (short)syncBytes.length, net.compressSnapshot(syncBytes));
            }
        }
    }

    String fixName(String name){
        name = name.trim();
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
        while(curChar < name.length() && result.toString().getBytes().length < maxNameLength){
            result.append(name.charAt(curChar++));
        }
        return result.toString();
    }

    String checkColor(String str){

        for(int i = 1; i < str.length(); i++){
            if(str.charAt(i) == ']'){
                String color = str.substring(1, i);

                if(Colors.get(color.toUpperCase()) != null || Colors.get(color.toLowerCase()) != null){
                    Color result = (Colors.get(color.toLowerCase()) == null ? Colors.get(color.toUpperCase()) : Colors.get(color.toLowerCase()));
                    if(result.a <= 0.8f){
                        return str.substring(i + 1);
                    }
                }else{
                    try{
                        Color result = Color.valueOf(color);
                        if(result.a <= 0.8f){
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

            //iterate through each player
            for(int i = 0; i < playerGroup.size(); i++){
                Player player = playerGroup.all().get(i);
                if(player.isLocal) continue;

                if(player.con == null || !player.con.isConnected()){
                    onDisconnect(player, "disappeared");
                    continue;
                }

                NetConnection connection = player.con;

                if(!player.timer.get(Player.timerSync, serverSyncTime) || !connection.hasConnected) continue;

                writeSnapshot(player);
            }

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
