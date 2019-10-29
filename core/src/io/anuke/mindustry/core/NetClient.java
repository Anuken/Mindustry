package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.CommandHandler.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.Administration.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.type.TypeID;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.modules.*;

import java.io.*;
import java.util.zip.*;

import static io.anuke.mindustry.Vars.*;

public class NetClient implements ApplicationListener{
    private final static float dataTimeout = 60 * 18;
    private final static float playerSyncTime = 2;
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

    public NetClient(){

        net.handleClient(Connect.class, packet -> {
            Log.info("Connecting to server: {0}", packet.addressTCP);

            player.isAdmin = false;

            reset();

            ui.loadfrag.hide();
            ui.loadfrag.show("$connecting.data");

            ui.loadfrag.setButton(() -> {
                ui.loadfrag.hide();
                connecting = false;
                quiet = true;
                net.disconnect();
            });

            ConnectPacket c = new ConnectPacket();
            c.name = player.name;
            c.mods = mods.getModStrings();
            c.mobile = mobile;
            c.versionType = Version.type;
            c.color = Color.rgba8888(player.color);
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
            state.set(State.menu);
            logic.reset();
            platform.updateRPC();

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
            Log.info("Recieved world data: {0} bytes.", data.stream.available());
            NetworkIO.loadWorld(new InflaterInputStream(data.stream));

            finishConnecting();
        });

        net.handleClient(InvokePacket.class, packet -> {
            packet.writeBuffer.position(0);
            RemoteReadClient.readPacket(packet.writeBuffer, packet.type);
        });
    }

    //called on all clients
    @Remote(called = Loc.server, targets = Loc.server, variants = Variant.both)
    public static void sendMessage(String message, String sender, Player playersender){
        if(Vars.ui != null && !(playersender != null && net.server() && sender.startsWith("[#" + player.getTeam().color.toString() + "]<T>"))){
            Vars.ui.chatfrag.addMessage(message, sender);
        }

        if(playersender != null){
            playersender.lastText = message;
            playersender.textFadeTime = 1f;
        }
    }

    //equivalent to above method but there's no sender and no console log
    @Remote(called = Loc.server, targets = Loc.server)
    public static void sendMessage(String message){
        if(Vars.ui != null){
            Vars.ui.chatfrag.addMessage(message, null);
        }
    }

    //called when a server recieves a chat message from a player
    @Remote(called = Loc.server, targets = Loc.client)
    public static void sendChatMessage(Player player, String message){
        if(message.length() > maxTextLength){
            throw new ValidateException(player, "Player has sent a message above the text limit.");
        }

        //check if it's a command
        CommandResponse response = netServer.clientCommands.handleMessage(message, player);
        if(response.type == ResponseType.noCommand){ //no command to handle
            //server console logging
            Log.info("&y{0}: &lb{1}", player.name, message);

            //invoke event for all clients but also locally
            //this is required so other clients get the correct name even if they don't know who's sending it yet
            Call.sendMessage(message, colorizeName(player.id, player.name), player);
        }else{
            //log command to console but with brackets
            Log.info("<&y{0}: &lm{1}&lg>", player.name, message);

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

        Events.fire(new PlayerChatEvent(player, message));
    }

    public static String colorizeName(int id, String name){
        Player player = playerGroup.getByID(id);
        if(name == null || player == null) return null;
        return "[#" + player.color.toString().toUpperCase() + "]" + name;
    }

    @Remote(called = Loc.client, variants = Variant.one)
    public static void onConnect(String ip, int port){
        netClient.disconnectQuietly();
        state.set(State.menu);
        logic.reset();

        ui.join.connect(ip, port);
    }
    
    @Remote(targets = Loc.client)
    public static void onPing(Player player, long time){
        Call.onPingResponse(player.con, time);
    }

    @Remote(variants = Variant.one)
    public static void onPingResponse(long time){
        netClient.ping = Time.timeSinceMillis(time);
    }

    @Remote(variants = Variant.one)
    public static void onTraceInfo(Player player, TraceInfo info){
        if(player != null){
            ui.traces.show(player, info);
        }
    }

    @Remote(variants = Variant.one, priority = PacketPriority.high)
    public static void onKick(KickReason reason){
        netClient.disconnectQuietly();
        state.set(State.menu);
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
    public static void onKick(String reason){
        netClient.disconnectQuietly();
        state.set(State.menu);
        logic.reset();
        ui.showText("$disconnect", reason, Align.left);
        ui.loadfrag.hide();
    }

    @Remote(variants = Variant.both)
    public static void onInfoMessage(String message){
        ui.showText("", message);
    }

    @Remote(variants = Variant.both)
    public static void onSetRules(Rules rules){
        state.rules = rules;
    }

    @Remote(variants = Variant.both)
    public static void onWorldDataBegin(){
        entities.clear();
        netClient.removed.clear();
        logic.reset();

        ui.chatfrag.clearMessages();
        net.setClientLoaded(false);

        ui.loadfrag.show("$connecting.data");

        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.connecting = false;
            netClient.quiet = true;
            net.disconnect();
        });
    }

    @Remote(variants = Variant.one)
    public static void onPositionSet(float x, float y){
        player.x = x;
        player.y = y;
    }

    @Remote
    public static void onPlayerDisconnect(int playerid){
        playerGroup.removeByID(playerid);
    }

    @Remote(variants = Variant.one, priority = PacketPriority.low, unreliable = true)
    public static void onEntitySnapshot(byte groupID, short amount, short dataLen, byte[] data){
        try{
            netClient.byteStream.setBytes(net.decompressSnapshot(data, dataLen));
            DataInputStream input = netClient.dataStream;

            EntityGroup group = entities.get(groupID);

            //go through each entity
            for(int j = 0; j < amount; j++){
                int id = input.readInt();
                byte typeID = input.readByte();

                SyncTrait entity = group == null ? null : (SyncTrait)group.getByID(id);
                boolean add = false, created = false;

                if(entity == null && id == player.id){
                    entity = player;
                    add = true;
                }

                //entity must not be added yet, so create it
                if(entity == null){
                    entity = (SyncTrait)content.<TypeID>getByID(ContentType.typeid, typeID).constructor.get();
                    entity.resetID(id);
                    if(!netClient.isEntityUsed(entity.getID())){
                        add = true;
                    }
                    created = true;
                }

                //read the entity
                entity.read(input);

                if(created && entity.getInterpolator() != null && entity.getInterpolator().target != null){
                    //set initial starting position
                    entity.setNet(entity.getInterpolator().target.x, entity.getInterpolator().target.y);
                    if(entity instanceof Unit && entity.getInterpolator().targets.length > 0){
                        ((Unit)entity).rotation = entity.getInterpolator().targets[0];
                    }
                }

                if(add){
                    entity.add();
                    netClient.addRemovedEntity(entity.getID());
                }
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Remote(variants = Variant.one, priority = PacketPriority.low, unreliable = true)
    public static void onStateSnapshot(float waveTime, int wave, int enemies, short coreDataLen, byte[] coreData){
        try{
            if(wave > state.wave){
                state.wave = wave;
                Events.fire(new WaveEvent());
            }

            state.wavetime = waveTime;
            state.wave = wave;
            state.enemies = enemies;

            netClient.byteStream.setBytes(net.decompressSnapshot(coreData, coreDataLen));
            DataInputStream input = netClient.dataStream;

            byte cores = input.readByte();
            for(int i = 0; i < cores; i++){
                int pos = input.readInt();
                Tile tile = world.tile(pos);

                if(tile != null && tile.entity != null){
                    tile.entity.items.read(input);
                }else{
                    new ItemModule().read(input);
                }
            }

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(){
        if(!net.client()) return;

        if(!state.is(State.menu)){
            if(!connecting) sync();
        }else if(!connecting){
            net.disconnect();
        }else{ //...must be connecting
            timeoutTime += Time.delta();
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
        Core.app.post(() -> ui.loadfrag.hide());
    }

    private void reset(){
        net.setClientLoaded(false);
        removed.clear();
        timeoutTime = 0f;
        connecting = true;
        quietReset = false;
        quiet = false;
        lastSent = 0;

        entities.clear();
        ui.chatfrag.clearMessages();
    }

    public void beginConnecting(){
        connecting = true;
    }

    /** Disconnects, resetting state to the menu. */
    public void disconnectQuietly(){
        quiet = true;
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
            BuildRequest[] requests;
            //limit to 10 to prevent buffer overflows
            int usedRequests = Math.min(player.buildQueue().size, 10);

            requests = new BuildRequest[usedRequests];
            for(int i = 0; i < usedRequests; i++){
                requests[i] = player.buildQueue().get(i);
            }

            Call.onClientShapshot(lastSent++, player.x, player.y,
            player.pointerX, player.pointerY, player.rotation, player.baseRotation,
            player.velocity().x, player.velocity().y,
            player.getMineTile(),
            player.isBoosting, player.isShooting, ui.chatfrag.chatOpen(), player.isBuilding,
            requests,
            Core.camera.position.x, Core.camera.position.y,
            Core.camera.width * viewScale, Core.camera.height * viewScale);
        }

        if(timer.get(1, 60)){
            Call.onPing(Time.millis());
        }
    }

    String getUsid(String ip){
        if(Core.settings.getString("usid-" + ip, null) != null){
            return Core.settings.getString("usid-" + ip, null);
        }else{
            byte[] bytes = new byte[8];
            new RandomXS128().nextBytes(bytes);
            String result = new String(Base64Coder.encode(bytes));
            Core.settings.put("usid-" + ip, result);
            Core.settings.save();
            return result;
        }
    }
}
