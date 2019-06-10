package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.RandomXS128;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.ReusableByteInStream;
import io.anuke.arc.util.serialization.Base64Coder;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Entities;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.gen.RemoteReadClient;
import io.anuke.mindustry.net.Administration.TraceInfo;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.modules.ItemModule;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.*;

public class NetClient implements ApplicationListener{
    private final static float dataTimeout = 60 * 18;
    private final static float playerSyncTime = 2;
    public final static float viewScale = 2f;

    private Interval timer = new Interval(5);
    /** Whether the client is currently connecting. */
    private boolean connecting = false;
    /** If true, no message will be shown on disconnect. */
    private boolean quiet = false;
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

        Net.handleClient(Connect.class, packet -> {
            Log.info("Connecting to server: {0}", packet.addressTCP);

            player.isAdmin = false;

            reset();

            ui.loadfrag.hide();
            ui.loadfrag.show("$connecting.data");

            ui.loadfrag.setButton(() -> {
                ui.loadfrag.hide();
                connecting = false;
                quiet = true;
                Net.disconnect();
            });

            ConnectPacket c = new ConnectPacket();
            c.name = player.name;
            c.mobile = mobile;
            c.versionType = Version.type;
            c.color = Color.rgba8888(player.color);
            c.usid = getUsid(packet.addressTCP);
            c.uuid = Platform.instance.getUUID();

            if(c.uuid == null){
                ui.showError("$invalidid");
                ui.loadfrag.hide();
                disconnectQuietly();
                return;
            }

            Net.send(c, SendMode.tcp);
        });

        Net.handleClient(Disconnect.class, packet -> {
            state.set(State.menu);
            connecting = false;
            logic.reset();
            Platform.instance.updateRPC();

            if(quiet) return;

            Time.runTask(3f, ui.loadfrag::hide);

            ui.showError("$disconnect");
        });

        Net.handleClient(WorldStream.class, data -> {
            Log.info("Recieved world data: {0} bytes.", data.stream.available());
            NetworkIO.loadWorld(new InflaterInputStream(data.stream));

            finishConnecting();
        });

        Net.handleClient(InvokePacket.class, packet -> {
            packet.writeBuffer.position(0);
            RemoteReadClient.readPacket(packet.writeBuffer, packet.type);
        });
    }

    //called on all clients
    @Remote(called = Loc.server, targets = Loc.server, variants = Variant.both)
    public static void sendMessage(String message, String sender, Player playersender){
        if(Vars.ui != null){
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

        //server console logging
        Log.info("&y{0}: &lb{1}", player.name, message);

        //invoke event for all clients but also locally
        //this is required so other clients get the correct name even if they don't know who's sending it yet
        Call.sendMessage(message, colorizeName(player.id, player.name), player);
    }

    private static String colorizeName(int id, String name){
        Player player = playerGroup.getByID(id);
        if(name == null || player == null) return null;
        return "[#" + player.color.toString().toUpperCase() + "]" + name;
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

        if(!reason.quiet){
            if(reason.extraText() != null){
                ui.showText(reason.toString(), reason.extraText());
            }else{
                ui.showText("$disconnect", reason.toString());
            }
        }
        ui.loadfrag.hide();
    }

    @Remote(variants = Variant.both)
    public static void onInfoMessage(String message){
        ui.showText("", message);
    }

    @Remote(variants = Variant.both)
    public static void onWorldDataBegin(){
        Entities.clear();
        netClient.removed.clear();
        logic.reset();

        ui.chatfrag.clearMessages();
        Net.setClientLoaded(false);

        ui.loadfrag.show("$connecting.data");

        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.connecting = false;
            netClient.quiet = true;
            Net.disconnect();
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
            netClient.byteStream.setBytes(Net.decompressSnapshot(data, dataLen));
            DataInputStream input = netClient.dataStream;

            EntityGroup group = Entities.getGroup(groupID);

            //go through each entity
            for(int j = 0; j < amount; j++){
                int id = input.readInt();
                byte typeID = input.readByte();

                SyncTrait entity = group == null ? null : (SyncTrait)group.getByID(id);
                boolean add = false;

                if(entity == null && id == player.id){
                    entity = player;
                    add = true;
                }

                //entity must not be added yet, so create it
                if(entity == null){
                    entity = (SyncTrait)TypeTrait.getTypeByID(typeID).get(); //create entity from supplier
                    entity.resetID(id);
                    if(!netClient.isEntityUsed(entity.getID())){
                        add = true;
                    }
                }

                //read the entity
                entity.read(input);

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
            state.wavetime = waveTime;
            state.wave = wave;
            state.enemies = enemies;

            netClient.byteStream.setBytes(Net.decompressSnapshot(coreData, coreDataLen));
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
        if(!Net.client()) return;

        if(!state.is(State.menu)){
            if(!connecting) sync();
        }else if(!connecting){
            Net.disconnect();
        }else{ //...must be connecting
            timeoutTime += Time.delta();
            if(timeoutTime > dataTimeout){
                Log.err("Failed to load data!");
                ui.loadfrag.hide();
                quiet = true;
                ui.showError("$disconnect.data");
                Net.disconnect();
                timeoutTime = 0f;
            }
        }
    }

    public boolean isConnecting(){
        return connecting;
    }

    private void finishConnecting(){
        state.set(State.playing);
        connecting = false;
        ui.loadfrag.hide();
        ui.join.hide();
        Net.setClientLoaded(true);
        Core.app.post(Call::connectConfirm);
        Time.runTask(40f, Platform.instance::updateRPC);
    }

    private void reset(){
        Net.setClientLoaded(false);
        removed.clear();
        timeoutTime = 0f;
        connecting = true;
        quiet = false;
        lastSent = 0;

        Entities.clear();
        ui.chatfrag.clearMessages();
    }

    public void beginConnecting(){
        connecting = true;
    }

    public void disconnectQuietly(){
        quiet = true;
        Net.disconnect();
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
            int usedRequests = Math.min(player.getPlaceQueue().size, 10);

            requests = new BuildRequest[usedRequests];
            for(int i = 0; i < usedRequests; i++){
                requests[i] = player.getPlaceQueue().get(i);
            }

            Call.onClientShapshot(lastSent++, player.x, player.y,
            player.pointerX, player.pointerY, player.rotation, player.baseRotation,
            player.velocity().x, player.velocity().y,
            player.getMineTile(),
            player.isBoosting, player.isShooting, ui.chatfrag.chatOpen(),
            requests,
            Core.camera.position.x, Core.camera.position.y,
            Core.camera.width * viewScale, Core.camera.height * viewScale);
        }

        if(timer.get(1, 60)){
            Net.updatePing();
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