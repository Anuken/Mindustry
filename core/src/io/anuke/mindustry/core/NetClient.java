package io.anuke.mindustry.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.Pools;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.annotations.Annotations.Variant;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.gen.RemoteReadClient;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.TraceInfo;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.io.ReusableByteArrayInputStream;
import io.anuke.ucore.io.delta.DEZDecoder;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Timer;

import java.io.DataInputStream;
import java.util.Arrays;
import java.util.Random;

import static io.anuke.mindustry.Vars.*;

public class NetClient extends Module {
    private final static float dataTimeout = 60*18;
    private final static float playerSyncTime = 2;

    private Timer timer = new Timer(5);
    /**Whether the client is currently connecting.*/
    private boolean connecting = false;
    /**If true, no message will be shown on disconnect.*/
    private boolean quiet = false;
    /**Counter for data timeout.*/
    private float timeoutTime = 0f;
    /**Last sent client snapshot ID.*/
    private int lastSent;
    /**Last snapshot recieved.*/
    private byte[] lastSnapshot;
    /**Last snapshot ID recieved.*/
    private int lastSnapshotID = -1;
    /**Decoder for uncompressing snapshots.*/
    private DEZDecoder decoder = new DEZDecoder();
    /**Byte stream for reading in snapshots.*/
    private ReusableByteArrayInputStream byteStream = new ReusableByteArrayInputStream();
    private DataInputStream dataStream = new DataInputStream(byteStream);

    public NetClient(){

        Net.handleClient(Connect.class, packet -> {
            Player player = players[0];

            player.isAdmin = false;

            Net.setClientLoaded(false);
            timeoutTime = 0f;
            connecting = true;
            quiet = false;

            ui.chatfrag.clearMessages();
            ui.loadfrag.hide();
            ui.loadfrag.show("$text.connecting.data");

            Entities.clear();

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
            if (quiet) return;

            Timers.runTask(3f, ui.loadfrag::hide);

            state.set(State.menu);

            ui.showError("$text.disconnect");
            connecting = false;

            Platform.instance.updateRPC();
        });

        Net.handleClient(WorldStream.class, data -> {
            Log.info("Recieved world data: {0} bytes.", data.stream.available());
            NetworkIO.loadWorld(data.stream);

            finishConnecting();
        });

        Net.handleClient(InvokePacket.class, packet -> {
            packet.writeBuffer.position(0);
            RemoteReadClient.readPacket(packet.writeBuffer, packet.type);
        });
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
        Call.connectConfirm();
        Timers.runTask(40f, Platform.instance::updateRPC);
    }

    public void beginConnecting(){
        connecting = true;
    }

    public void disconnectQuietly(){
        quiet = true;
        Net.disconnect();
    }

    void sync(){

        if(timer.get(0, playerSyncTime)){

            ClientSnapshotPacket packet = Pools.obtain(ClientSnapshotPacket.class);
            packet.lastSnapshot = lastSnapshotID;
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
            return result;
        }
    }

    @Remote(variants = Variant.one)
    public static void onKick(KickReason reason){
        netClient.disconnectQuietly();
        state.set(State.menu);
        if(!reason.quiet) ui.showError("$text.server.kicked." + reason.name());
        ui.loadfrag.hide();
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

    @Remote(variants = Variant.one, unreliable = true)
    public static void onSnapshot(byte[] snapshot, int snapshotID){
        //skip snapshot IDs that have already been recieved
        if(snapshotID == netClient.lastSnapshotID){
            return;
        }

        try {

            byte[] result;
            int length;
            if (snapshotID == 0) { //fresh snapshot
                result = snapshot;
                length = snapshot.length;
                netClient.lastSnapshot = snapshot;
            } else { //otherwise, last snapshot must not be null, decode it
                netClient.decoder.init(netClient.lastSnapshot, snapshot);
                result = netClient.decoder.decode();
                length = netClient.decoder.getDecodedLength();
                //set last snapshot to a copy to prevent issues
                netClient.lastSnapshot = Arrays.copyOf(result, length);
            }

            netClient.lastSnapshotID = snapshotID;

            //set stream bytes to begin write
            netClient.byteStream.setBytes(result, 0, length);

            //get data input for reading from the stream
            DataInputStream input = netClient.dataStream;
            
            byte cores = input.readByte();
            for (int i = 0; i < cores; i++) {
                int pos = input.readInt();
                world.tile(pos).entity.items.read(input);
            }

            long timestamp = input.readLong();

            byte totalGroups = input.readByte();
            //for each group...
            for (int i = 0; i < totalGroups; i++) {
                //read group info
                byte groupID = input.readByte();
                short amount = input.readShort();

                EntityGroup group = Entities.getGroup(groupID);

                //go through each entity
                for (int j = 0; j < amount; j++) {
                    int position = netClient.byteStream.position(); //save position to check read/write correctness
                    int id = input.readInt();
                    byte typeID = input.readByte();

                    SyncTrait entity = (SyncTrait) group.getByID(id);
                    boolean add = false;

                    //entity must not be added yet, so create it
                    if(entity == null){
                        entity = (SyncTrait) TypeTrait.getTypeByID(typeID).get(); //create entity from supplier
                        entity.resetID(id);
                        add = true;
                    }

                    //read the entity
                    entity.read(input, timestamp);

                    byte readLength = input.readByte();
                    if(netClient.byteStream.position() - position - 1 != readLength){
                        throw new RuntimeException("Error reading entity of type '"+ group.getType() + "': Read length mismatch [write=" + readLength + ", read=" + (netClient.byteStream.position() - position - 1)+ "]");
                    }

                    if(add){
                        entity.add();
                    }
                }
            }

            //confirm that snapshot has been recieved
            netClient.lastSnapshotID = snapshotID;

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}