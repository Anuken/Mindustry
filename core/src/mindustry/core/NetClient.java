package mindustry.core;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.GameState.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;
import mindustry.net.Net.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.world.*;
import mindustry.world.modules.*;

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class NetClient implements ApplicationListener{
    private static final float dataTimeout = 60 * 18;
    private static final float playerSyncTime = 2;
    public final static float viewScale = 2f;

    private long ping;
    private Interval timer = new Interval(5);
    /** Whether the client is currently connecting. */
    private boolean connecting = false;
    /** If true, no message will be shown on disconnect. */
    private boolean quiet = false;
    /** Whether to supress disconnect events completely.*/
    private boolean quietReset = false;
    /** Counter for data timeout. */
    private float timeoutTime = 0f;
    /** Last sent client snapshot ID. */
    private int lastSent;

    /** List of entities that were removed, and need not be added while syncing. */
    private IntSet removed = new IntSet();
    /** Byte stream for reading in snapshots. */
    private ReusableByteInStream byteStream = new ReusableByteInStream();
    private DataInputStream dataStream = new DataInputStream(byteStream);
    /** Packet handlers for custom types of messages. */
    private ObjectMap<String, Seq<Cons<String>>> customPacketHandlers = new ObjectMap<>();

    public NetClient(){

        net.handleClient(Connect.class, packet -> {
            Log.info("Connecting to server: @", packet.addressTCP);

            player.admin(false);

            reset();

            ui.loadfrag.hide();
            ui.loadfrag.show("$connecting.data");

            ui.loadfrag.setButton(() -> {
                ui.loadfrag.hide();
                disconnectQuietly();
            });

            ConnectPacket c = new ConnectPacket();
            c.name = player.name;
            c.mods = mods.getModStrings();
            c.mobile = mobile;
            c.versionType = Version.type;
            c.color = player.color().rgba();
            c.usid = getUsid(packet.addressTCP);
            c.uuid = platform.getUUID();

            if(c.uuid == null){
                ui.showErrorMessage("$invalidid");
                ui.loadfrag.hide();
                disconnectQuietly();
                return;
            }

            net.send(c, SendMode.tcp);
        });

        net.handleClient(Disconnect.class, packet -> {
            if(quietReset) return;

            connecting = false;
            logic.reset();
            platform.updateRPC();
            player.name(Core.settings.getString("name"));
            player.color().set(Core.settings.getInt("color-0"));

            if(quiet) return;

            Time.runTask(3f, ui.loadfrag::hide);

            if(packet.reason != null){
                if(packet.reason.equals("closed")){
                    ui.showSmall("$disconnect", "$disconnect.closed");
                }else if(packet.reason.equals("timeout")){
                    ui.showSmall("$disconnect", "$disconnect.timeout");
                }else if(packet.reason.equals("error")){
                    ui.showSmall("$disconnect", "$disconnect.error");
                }
            }else{
                ui.showErrorMessage("$disconnect");
            }
        });

        net.handleClient(WorldStream.class, data -> {
            Log.info("Received world data: @ bytes.", data.stream.available());
            NetworkIO.loadWorld(new InflaterInputStream(data.stream));

            finishConnecting();
        });

        net.handleClient(InvokePacket.class, packet -> {
            RemoteReadClient.readPacket(packet.reader(), packet.type);
        });
    }

    public void addPacketHandler(String type, Cons<String> handler){
        customPacketHandlers.get(type, Seq::new).add(handler);
    }

    public Seq<Cons<String>> getPacketHandlers(String type){
        return customPacketHandlers.get(type, Seq::new);
    }

    @Remote(targets = Loc.server, variants = Variant.both)
    public static void clientPacketReliable(String type, String contents){
        if(netClient.customPacketHandlers.containsKey(type)){
            for(Cons<String> c : netClient.customPacketHandlers.get(type)){
                c.get(contents);
            }
        }
    }

    @Remote(targets = Loc.server, variants = Variant.both, unreliable = true)
    public static void clientPacketUnreliable(String type, String contents){
        clientPacketReliable(type, contents);
    }

    //called on all clients
    @Remote(targets = Loc.server, variants = Variant.both)
    public static void sendMessage(String message, String sender, Player playersender){
        if(Vars.ui != null){
            Vars.ui.chatfrag.addMessage(message, sender);
        }

        if(playersender != null){
            playersender.lastText(message);
            playersender.textFadeTime(1f);
        }
    }

    //equivalent to above method but there's no sender and no console log
    @Remote(called = Loc.server, targets = Loc.server)
    public static void sendMessage(String message){
        if(Vars.ui != null){
            Vars.ui.chatfrag.addMessage(message, null);
        }
    }

    //called when a server receives a chat message from a player
    @Remote(called = Loc.server, targets = Loc.client)
    public static void sendChatMessage(Player player, String message){
        if(message.length() > maxTextLength){
            throw new ValidateException(player, "Player has sent a message above the text limit.");
        }

        Events.fire(new PlayerChatEvent(player, message));

        //check if it's a command
        CommandResponse response = netServer.clientCommands.handleMessage(message, player);
        if(response.type == ResponseType.noCommand){ //no command to handle
            message = netServer.admins.filterMessage(player, message);
            //supress chat message if it's filtered out
            if(message == null){
                return;
            }

            //special case; graphical server needs to see its message
            if(!headless){
                sendMessage(message, colorizeName(player.id(), player.name), player);
            }

            //server console logging
            Log.info("&y@: &lb@", player.name, message);

            //invoke event for all clients but also locally
            //this is required so other clients get the correct name even if they don't know who's sending it yet
            Call.sendMessage(message, colorizeName(player.id(), player.name), player);
        }else{
            //log command to console but with brackets
            Log.info("<&y@: &lm@&lg>", player.name, message);

            //a command was sent, now get the output
            if(response.type != ResponseType.valid){
                String text;

                //send usage
                if(response.type == ResponseType.manyArguments){
                    text = "[scarlet]Too many arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
                }else if(response.type == ResponseType.fewArguments){
                    text = "[scarlet]Too few arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
                }else{ //unknown command
                    text = "[scarlet]Unknown command. Check [lightgray]/help[scarlet].";
                }

                player.sendMessage(text);
            }
        }
    }

    public static String colorizeName(int id, String name){
        Player player = Groups.player.getByID(id);
        if(name == null || player == null) return null;
        return "[#" + player.color().toString().toUpperCase() + "]" + name;
    }

    @Remote(called = Loc.client, variants = Variant.one)
    public static void connect(String ip, int port){
        netClient.disconnectQuietly();
        logic.reset();

        ui.join.connect(ip, port);
    }
    
    @Remote(targets = Loc.client)
    public static void ping(Player player, long time){
        Call.pingResponse(player.con, time);
    }

    @Remote(variants = Variant.one)
    public static void pingResponse(long time){
        netClient.ping = Time.timeSinceMillis(time);
    }

    @Remote(variants = Variant.one)
    public static void traceInfo(Player player, TraceInfo info){
        if(player != null){
            ui.traces.show(player, info);
        }
    }

    @Remote(variants = Variant.one, priority = PacketPriority.high)
    public static void kick(KickReason reason){
        netClient.disconnectQuietly();
        logic.reset();

        if(!reason.quiet){
            if(reason.extraText() != null){
                ui.showText(reason.toString(), reason.extraText());
            }else{
                ui.showText("$disconnect", reason.toString());
            }
        }
        ui.loadfrag.hide();
    }

    @Remote(variants = Variant.one, priority = PacketPriority.high)
    public static void kick(String reason){
        netClient.disconnectQuietly();
        logic.reset();
        ui.showText("$disconnect", reason, Align.left);
        ui.loadfrag.hide();
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void setHudText(String message){
        if(message == null) return;

        ui.hudfrag.setHudText(message);
    }

    @Remote(variants = Variant.both)
    public static void hideHudText(){
        ui.hudfrag.toggleHudText(false);
    }

    /** TCP version */
    @Remote(variants = Variant.both)
    public static void setHudTextReliable(String message){
        setHudText(message);
    }

    @Remote(variants = Variant.both)
    public static void infoMessage(String message){
        if(message == null) return;

        ui.showText("", message);
    }

    @Remote(variants = Variant.both)
    public static void infoPopup(String message, float duration, int align, int top, int left, int bottom, int right){
        if(message == null) return;

        ui.showInfoPopup(message, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both)
    public static void label(String message, float duration, float worldx, float worldy){
        if(message == null) return;

        ui.showLabel(message, duration, worldx, worldy);
    }

    /*
    @Remote(variants = Variant.both, unreliable = true)
    public static void onEffect(Effect effect, float x, float y, float rotation, Color color){
        if(effect == null) return;

        effect.at(x, y, rotation, color);
    }

    @Remote(variants = Variant.both)
    public static void onEffectReliable(Effect effect, float x, float y, float rotation, Color color){
        onEffect(effect, x, y, rotation, color);
    }*/

    @Remote(variants = Variant.both)
    public static void infoToast(String message, float duration){
        if(message == null) return;

        ui.showInfoToast(message, duration);
    }

    @Remote(variants = Variant.both)
    public static void setRules(Rules rules){
        state.rules = rules;
    }

    @Remote(variants = Variant.both)
    public static void worldDataBegin(){
        Groups.clear();
        netClient.removed.clear();
        logic.reset();

        net.setClientLoaded(false);

        ui.loadfrag.show("$connecting.data");

        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();

            netClient.disconnectQuietly();
        });
    }

    @Remote(variants = Variant.one)
    public static void setPosition(float x, float y){
        player.unit().set(x, y);
        player.set(x, y);
    }

    @Remote
    public static void playerDisconnect(int playerid){
        Groups.player.removeByID(playerid);
    }

    @Remote(variants = Variant.one, priority = PacketPriority.low, unreliable = true)
    public static void entitySnapshot(short amount, short dataLen, byte[] data){
        try{
            netClient.byteStream.setBytes(net.decompressSnapshot(data, dataLen));
            DataInputStream input = netClient.dataStream;

            //go through each entity
            for(int j = 0; j < amount; j++){
                int id = input.readInt();
                byte typeID = input.readByte();

                Syncc entity = Groups.sync.getByID(id);
                boolean add = false, created = false;

                if(entity == null && id == player.id()){
                    entity = player;
                    add = true;
                }

                //entity must not be added yet, so create it
                if(entity == null){
                    entity = (Syncc)EntityMapping.map(typeID).get();
                    entity.id(id);
                    if(!netClient.isEntityUsed(entity.id())){
                        add = true;
                    }
                    created = true;
                }

                //read the entity
                entity.readSync(Reads.get(input));

                if(created){
                    //snap initial starting position
                    entity.snapSync();
                }

                if(add){
                    entity.add();
                    netClient.addRemovedEntity(entity.id());
                }
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Remote(variants = Variant.both, priority = PacketPriority.low, unreliable = true)
    public static void blockSnapshot(short amount, short dataLen, byte[] data){
        try{
            netClient.byteStream.setBytes(net.decompressSnapshot(data, dataLen));
            DataInputStream input = netClient.dataStream;

            for(int i = 0; i < amount; i++){
                int pos = input.readInt();
                Tile tile = world.tile(pos);
                if(tile == null || tile.build == null){
                    Log.warn("Missing entity at @. Skipping block snapshot.", tile);
                    break;
                }
                tile.build.readAll(Reads.get(input), tile.build.version());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Remote(variants = Variant.one, priority = PacketPriority.low, unreliable = true)
    public static void stateSnapshot(float waveTime, int wave, int enemies, boolean paused, short coreDataLen, byte[] coreData){
        try{
            if(wave > state.wave){
                state.wave = wave;
                Events.fire(new WaveEvent());
            }

            state.wavetime = waveTime;
            state.wave = wave;
            state.enemies = enemies;
            state.serverPaused = paused;

            netClient.byteStream.setBytes(net.decompressSnapshot(coreData, coreDataLen));
            DataInputStream input = netClient.dataStream;

            byte cores = input.readByte();
            for(int i = 0; i < cores; i++){
                int pos = input.readInt();
                Tile tile = world.tile(pos);

                if(tile != null && tile.build != null){
                    tile.build.items.read(Reads.get(input));
                }else{
                    new ItemModule().read(Reads.get(input));
                }
            }

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(){
        if(!net.client()) return;

        if(state.isGame()){
            if(!connecting) sync();
        }else if(!connecting){
            net.disconnect();
        }else{ //...must be connecting
            timeoutTime += Time.delta;
            if(timeoutTime > dataTimeout){
                Log.err("Failed to load data!");
                ui.loadfrag.hide();
                quiet = true;
                ui.showErrorMessage("$disconnect.data");
                net.disconnect();
                timeoutTime = 0f;
            }
        }
    }

    public boolean isConnecting(){
        return connecting;
    }

    public int getPing(){
        return (int)ping;
    }

    private void finishConnecting(){
        state.set(State.playing);
        connecting = false;
        ui.join.hide();
        net.setClientLoaded(true);
        Core.app.post(Call::connectConfirm);
        Time.runTask(40f, platform::updateRPC);
        Core.app.post(ui.loadfrag::hide);
    }

    private void reset(){
        net.setClientLoaded(false);
        removed.clear();
        timeoutTime = 0f;
        connecting = true;
        quietReset = false;
        quiet = false;
        lastSent = 0;

        Groups.clear();
        ui.chatfrag.clearMessages();
    }

    public void beginConnecting(){
        connecting = true;
    }

    /** Disconnects, resetting state to the menu. */
    public void disconnectQuietly(){
        quiet = true;
        connecting = false;
        net.disconnect();
    }

    /** Disconnects, causing no further changes or reset.*/
    public void disconnectNoReset(){
        quiet = quietReset = true;
        net.disconnect();
    }

    /** When set, any disconnects will be ignored and no dialogs will be shown. */
    public void setQuiet(){
        quiet = true;
    }

    public void addRemovedEntity(int id){
        removed.add(id);
    }

    public boolean isEntityUsed(int id){
        return removed.contains(id);
    }

    void sync(){
        if(timer.get(0, playerSyncTime)){
            BuildPlan[] requests = null;
            if(player.isBuilder() && control.input.isBuilding){
                //limit to 10 to prevent buffer overflows
                int usedRequests = Math.min(player.builder().plans().size, 10);

                requests = new BuildPlan[usedRequests];
                for(int i = 0; i < usedRequests; i++){
                    requests[i] = player.builder().plans().get(i);
                }
            }

            Unit unit = player.dead() ? Nulls.unit : player.unit();

            Call.clientShapshot(lastSent++,
            player.dead(),
            unit.x, unit.y,
            player.unit().aimX(), player.unit().aimY(),
            unit.rotation,
            unit instanceof Mechc ? ((Mechc)unit).baseRotation() : 0,
            unit.vel.x, unit.vel.y,
            player.miner().mineTile(),
            player.boosting, player.shooting, ui.chatfrag.shown(),
            requests,
            Core.camera.position.x, Core.camera.position.y,
            Core.camera.width * viewScale, Core.camera.height * viewScale);
        }

        if(timer.get(1, 60)){
            Call.ping(Time.millis());
        }
    }

    String getUsid(String ip){
        //consistently use the latter part of an IP, if possible
        if(ip.contains("/")){
            ip = ip.substring(ip.indexOf("/") + 1);
        }

        if(Core.settings.getString("usid-" + ip, null) != null){
            return Core.settings.getString("usid-" + ip, null);
        }else{
            byte[] bytes = new byte[8];
            new Rand().nextBytes(bytes);
            String result = new String(Base64Coder.encode(bytes));
            Core.settings.put("usid-" + ip, result);
            return result;
        }
    }
}
