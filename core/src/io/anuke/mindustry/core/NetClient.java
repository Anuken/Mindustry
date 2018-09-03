package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntMap.Entry;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.annotations.Annotations.PacketPriority;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.annotations.Annotations.Variant;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.gen.RemoteReadClient;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.TraceInfo;
import io.anuke.mindustry.world.modules.InventoryModule;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.io.ReusableByteArrayInputStream;
import io.anuke.ucore.io.delta.DEZDecoder;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.Timer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.*;

public class NetClient extends Module{
    private final static float dataTimeout = 60 * 18;
    private final static float playerSyncTime = 2;
    private final static IntArray removals = new IntArray();

    private Timer timer = new Timer(5);
    /**Whether the client is currently connecting.*/
    private boolean connecting = false;
    /**If true, no message will be shown on disconnect.*/
    private boolean quiet = false;
    /**Counter for data timeout.*/
    private float timeoutTime = 0f;
    /**Last sent client snapshot ID.*/
    private int lastSent;

    /**Last snapshot ID recieved.*/
    private int lastSnapshotBaseID = -1;

    private IntMap<byte[]> recievedSnapshots = new IntMap<>();
    /**Current snapshot that is being built from chinks.*/
    private byte[] currentSnapshot;
    /**Array of recieved chunk statuses.*/
    private boolean[] recievedChunks;
    /**Counter of how many chunks have been recieved.*/
    private int recievedChunkCounter;
    /**ID of snapshot that is currently being constructed.*/
    private int currentSnapshotID = -1;

    /**Decoder for uncompressing snapshots.*/
    private DEZDecoder decoder = new DEZDecoder();
    /**List of entities that were removed, and need not be added while syncing.*/
    private IntSet removed = new IntSet();
    /**Byte stream for reading in snapshots.*/
    private ReusableByteArrayInputStream byteStream = new ReusableByteArrayInputStream();
    private DataInputStream dataStream = new DataInputStream(byteStream);

    public NetClient(){

        Net.handleClient(Connect.class, packet -> {
            Player player = players[0];

            player.isAdmin = false;

            reset();

            ui.loadfrag.hide();
            ui.loadfrag.show("$text.connecting.data");

            ui.loadfrag.setButton(() -> {
                ui.loadfrag.hide();
                connecting = false;
                quiet = true;
                Net.disconnect();
            });

            ConnectPacket c = new ConnectPacket();
            c.name = player.name;
            c.mobile = mobile;
            c.color = Color.rgba8888(player.color);
            c.usid = getUsid(packet.addressTCP);
            c.uuid = Platform.instance.getUUID();

            if(c.uuid == null){
                ui.showError("$text.invalidid");
                ui.loadfrag.hide();
                disconnectQuietly();
                return;
            }

            Net.send(c, SendMode.tcp);
        });

        Net.handleClient(Disconnect.class, packet -> {
            if(quiet) return;

            Timers.runTask(3f, ui.loadfrag::hide);

            state.set(State.menu);

            ui.showError("$text.disconnect");
            connecting = false;

            Platform.instance.updateRPC();
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

    @Remote(variants = Variant.one, priority = PacketPriority.high)
    public static void onKick(KickReason reason){
        netClient.disconnectQuietly();
        state.set(State.menu);
        if(!reason.quiet){
            if(reason.extraText() != null){
                ui.showText(reason.toString(), reason.extraText());
            }else{
                ui.showText("$text.disconnect", reason.toString());
            }
        }
        ui.loadfrag.hide();
    }

    @Remote(variants = Variant.both)
    public static void onInfoMessage(String message){
        threads.runGraphics(() -> ui.showText("", message));
    }

    @Remote(variants = Variant.both)
    public static void onWorldDataBegin(){
        Entities.clear();
        netClient.removed.clear();

        ui.chatfrag.clearMessages();
        Net.setClientLoaded(false);

        threads.runGraphics(() -> {
            ui.loadfrag.show("$text.connecting.data");

            ui.loadfrag.setButton(() -> {
                ui.loadfrag.hide();
                netClient.connecting = false;
                netClient.quiet = true;
                Net.disconnect();
            });
        });
    }

    @Remote(variants = Variant.one)
    public static void onPositionSet(float x, float y){
        players[0].x = x;
        players[0].y = y;
    }

    @Remote(variants = Variant.one)
    public static void onTraceInfo(TraceInfo info){
        Player player = playerGroup.getByID(info.playerid);
        ui.traces.show(player, info);
    }

    @Remote
    public static void onPlayerDisconnect(int playerid){
        playerGroup.removeByID(playerid);
    }

    @Remote(variants = Variant.one, priority = PacketPriority.low, unreliable = true)
    public static void onSnapshot(byte[] chunk, int snapshotID, short chunkID, int totalLength, int base){
        if(NetServer.debugSnapshots)
            Log.info("Recieved snapshot: len {0} ID {1} chunkID {2} totalLength {3} base {4} client-base {5}", chunk.length, snapshotID, chunkID, totalLength, base, netClient.lastSnapshotBaseID);

        //skip snapshot IDs that have already been recieved OR snapshots that are too far in front
        if(base != -1 && (snapshotID < netClient.lastSnapshotBaseID || !netClient.recievedSnapshots.containsKey(base))){
            if(NetServer.debugSnapshots) Log.info("//SKIP SNAPSHOT");
            return;
        }

        try{
            byte[] snapshot;

            //total length exceeds that needed to hold one snapshot, therefore, it is split into chunks
            if(totalLength > NetServer.maxSnapshotSize){
                //total amount of chunks to recieve
                int totalChunks = Mathf.ceil((float) totalLength / NetServer.maxSnapshotSize);

                //reset status when a new snapshot sending begins
                if(netClient.currentSnapshotID != snapshotID || netClient.recievedChunks == null){
                    netClient.currentSnapshotID = snapshotID;
                    netClient.currentSnapshot = new byte[totalLength];
                    netClient.recievedChunkCounter = 0;
                    netClient.recievedChunks = new boolean[totalChunks];
                }

                //if this chunk hasn't been recieved yet...
                if(!netClient.recievedChunks[chunkID]){
                    netClient.recievedChunks[chunkID] = true;
                    netClient.recievedChunkCounter++; //update recieved status
                    //copy the recieved bytes into the holding array
                    System.arraycopy(chunk, 0, netClient.currentSnapshot, chunkID * NetServer.maxSnapshotSize,
                            Math.min(NetServer.maxSnapshotSize, totalLength - chunkID * NetServer.maxSnapshotSize));
                }

                //when all chunks have been recieved, begin
                if(netClient.recievedChunkCounter >= totalChunks){
                    snapshot = netClient.currentSnapshot;
                }else{
                    return;
                }
            }else{
                snapshot = chunk;
            }

            if(NetServer.debugSnapshots)
                Log.info("Finished recieving snapshot ID {0} length {1}", snapshotID, chunk.length);

            byte[] result;
            int length;
            if(base == -1){ //fresh snapshot
                result = snapshot;
                length = snapshot.length;
                netClient.recievedSnapshots.put(snapshotID, Arrays.copyOf(snapshot, snapshot.length));
            }else{ //otherwise, last snapshot must not be null, decode it
                byte[] baseBytes = netClient.recievedSnapshots.get(base);
                if(NetServer.debugSnapshots)
                    Log.info("Base size: {0} Patch size: {1}", baseBytes.length, snapshot.length);
                netClient.decoder.init(baseBytes, snapshot);
                result = netClient.decoder.decode();
                length = netClient.decoder.getDecodedLength();
                //set last snapshot to a copy to prevent issues
                netClient.recievedSnapshots.put(snapshotID, Arrays.copyOf(result, length));
            }

            netClient.lastSnapshotBaseID = snapshotID;

            //set stream bytes to begin snapshot reading
            netClient.byteStream.setBytes(result, 0, length);

            //get data input for reading from the stream
            DataInputStream input = netClient.dataStream;

            netClient.readSnapshot(input);

            //confirm that snapshot has been recieved
            netClient.lastSnapshotBaseID = snapshotID;

            removals.clear();
            for(Entry<byte[]> entry : netClient.recievedSnapshots.entries()){
                if(entry.key < base){
                    removals.add(entry.key);
                }
            }
            for(int i = 0; i < removals.size; i++){
                netClient.recievedSnapshots.remove(removals.get(i));
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void readSnapshot(DataInputStream input) throws IOException{

        //read wave info
        state.wavetime = input.readFloat();
        state.wave = input.readInt();

        byte cores = input.readByte();
        for(int i = 0; i < cores; i++){
            int pos = input.readInt();
            TileEntity entity = world.tile(pos).entity;
            if(entity != null){
                entity.items.read(input);
            }else{
                new InventoryModule().read(input);
            }
        }

        long timestamp = input.readLong();

        byte totalGroups = input.readByte();
        //for each group...
        for(int i = 0; i < totalGroups; i++){
            //read group info
            byte groupID = input.readByte();
            short amount = input.readShort();

            EntityGroup group = Entities.getGroup(groupID);

            //go through each entity
            for(int j = 0; j < amount; j++){
                int id = input.readInt();
                byte typeID = input.readByte();

                SyncTrait entity = (SyncTrait) group.getByID(id);
                boolean add = false;

                //entity must not be added yet, so create it
                if(entity == null){
                    entity = (SyncTrait) TypeTrait.getTypeByID(typeID).get(); //create entity from supplier
                    entity.resetID(id);
                    if(!netClient.isEntityUsed(entity.getID())){
                        add = true;
                    }
                }

                //read the entity
                entity.read(input, timestamp);

                if(add){
                    entity.add();
                    netClient.addRemovedEntity(entity.getID());
                }
            }
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
            timeoutTime += Timers.delta();
            if(timeoutTime > dataTimeout){
                Log.err("Failed to load data!");
                ui.loadfrag.hide();
                quiet = true;
                ui.showError("$text.disconnect.data");
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
        Gdx.app.postRunnable(Call::connectConfirm);
        Timers.runTask(40f, Platform.instance::updateRPC);
    }

    private void reset(){
        Net.setClientLoaded(false);
        removed.clear();
        timeoutTime = 0f;
        connecting = true;
        quiet = false;
        lastSent = 0;
        recievedSnapshots.clear();
        currentSnapshot = null;
        currentSnapshotID = -1;
        lastSnapshotBaseID = -1;

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

    public synchronized void addRemovedEntity(int id){
        removed.add(id);
    }

    public synchronized boolean isEntityUsed(int id){
        return removed.contains(id);
    }

    void sync(){

        if(timer.get(0, playerSyncTime)){

            ClientSnapshotPacket packet = Pooling.obtain(ClientSnapshotPacket.class);
            packet.lastSnapshot = lastSnapshotBaseID;
            packet.snapid = lastSent++;
            Net.send(packet, SendMode.udp);
        }

        if(timer.get(1, 60)){
            Net.updatePing();
        }
    }

    String getUsid(String ip){
        if(Settings.getString("usid-" + ip, null) != null){
            return Settings.getString("usid-" + ip, null);
        }else{
            byte[] bytes = new byte[8];
            new Random().nextBytes(bytes);
            String result = new String(Base64Coder.encode(bytes));
            Settings.putString("usid-" + ip, result);
            Settings.save();
            return result;
        }
    }
}