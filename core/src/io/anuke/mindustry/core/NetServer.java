package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.ByteArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.UpgradeRecipes;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class NetServer extends Module{
    /**Maps connection IDs to players.*/
    IntMap<Player> connections = new IntMap<>();
    ObjectMap<String, ByteArray> weapons = new ObjectMap<>();
    float serverSyncTime = 4, itemSyncTime = 10, blockSyncTime = 120;
    boolean closing = false;

    public NetServer(){

        Events.on(GameOverEvent.class, () -> weapons.clear());

        Net.handleServer(Connect.class, (id, connect) -> {});

        Net.handleServer(ConnectPacket.class, (id, packet) -> {

            if(packet.version != versionBuild){
                Net.kickConnection(id, packet.version > versionBuild ? KickReason.serverOutdated : KickReason.clientOutdated);
                return;
            }

            Log.info("Sending data to player '{0}' / {1}", packet.name, id);

            Player player = new Player();
            player.clientid = id;
            player.name = packet.name;
            player.isAndroid = packet.android;
            player.set(world.getSpawnX(), world.getSpawnY());
            player.setNet(player.x, player.y);
            player.setNet(player.x, player.y);
            player.color.set(packet.color);
            connections.put(id, player);

            if(world.getMap().custom){
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                NetworkIO.writeMap(world.getMap(), stream);
                CustomMapPacket data = new CustomMapPacket();
                data.stream = new ByteArrayInputStream(stream.toByteArray());
                Net.sendStream(id, data);

                Log.info("Sending custom map: Packed {0} uncompressed bytes of MAP data.", stream.size());
            }else{
                //hack-- simulate the map ack packet recieved to send the world data to the client.
                Net.handleServerReceived(id, new MapAckPacket());
            }

            Platform.instance.updateRPC();
        });

        Net.handleServer(MapAckPacket.class, (id, packet) -> {
            Player player = connections.get(id);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            NetworkIO.writeWorld(player.id, weapons.get(player.name, new ByteArray()), stream);
            WorldData data = new WorldData();
            data.stream = new ByteArrayInputStream(stream.toByteArray());
            Net.sendStream(id, data);

            Log.info("Packed {0} uncompressed bytes of WORLD data.", stream.size());
        });

        Net.handleServer(ConnectConfirmPacket.class, (id, packet) -> {
            Player player = connections.get(id);

            if (player == null) return;

            player.add();
            Log.info("&y{0} has connected.", player.name);
            netCommon.sendMessage("[accent]" + player.name + " has connected.");
        });

        Net.handleServer(Disconnect.class, (id, packet) -> {
            Player player = connections.get(packet.id);

            if (player == null) {
                Log.err("Unknown client has disconnected (ID={0})", id);
                return;
            }

            Log.info("&y{0} has disconnected.", player.name);
            netCommon.sendMessage("[accent]" + player.name + " has disconnected.");
            player.remove();

            DisconnectPacket dc = new DisconnectPacket();
            dc.playerid = player.id;

            Net.send(dc, SendMode.tcp);

            Platform.instance.updateRPC();
        });

        Net.handleServer(PositionPacket.class, (id, packet) -> {
            ByteBuffer buffer = ByteBuffer.wrap(packet.data);
            long time = buffer.getLong();

            Player player = connections.get(id);
            player.read(buffer, time);
        });

        Net.handleServer(ShootPacket.class, (id, packet) -> {
            packet.playerid = connections.get(id).id;
            Net.sendExcept(id, packet, SendMode.udp);
        });

        Net.handleServer(PlacePacket.class, (id, packet) -> {
            packet.playerid = connections.get(id).id;
            Net.sendExcept(id, packet, SendMode.tcp);
        });

        Net.handleServer(BreakPacket.class, (id, packet) -> {
            packet.playerid = connections.get(id).id;
            Net.sendExcept(id, packet, SendMode.tcp);
        });

        Net.handleServer(ChatPacket.class, (id, packet) -> {
            Player player = connections.get(id);
            packet.name = player.name;
            packet.id = player.id;
            Net.sendExcept(player.clientid, packet, SendMode.tcp);
        });

        Net.handleServer(UpgradePacket.class, (id, packet) -> {
            Player player = connections.get(id);

            Weapon weapon = (Weapon) Upgrade.getByID(packet.id);

            if (!weapons.containsKey(player.name)) weapons.put(player.name, new ByteArray());
            if (!weapons.get(player.name).contains(weapon.id)) weapons.get(player.name).add(weapon.id);

            state.inventory.removeItems(UpgradeRecipes.get(weapon));
        });

        Net.handleServer(WeaponSwitchPacket.class, (id, packet) -> {
            packet.playerid = connections.get(id).id;
            Net.sendExcept(id, packet, SendMode.tcp);
        });

        Net.handleServer(BlockTapPacket.class, (id, packet) -> {
            Net.sendExcept(id, packet, SendMode.tcp);
        });

        Net.handleServer(BlockConfigPacket.class, (id, packet) -> {
            Net.sendExcept(id, packet, SendMode.tcp);
        });

        Net.handleServer(EntityRequestPacket.class, (cid, packet) -> {
            int id = packet.id;
            int dest = cid;
            EntityGroup group = Entities.getGroup(packet.group);
            if(group.getByID(id) != null){
                EntitySpawnPacket p = new EntitySpawnPacket();
                p.entity = (SyncEntity)group.getByID(id);
                p.group = group;
                Net.sendTo(dest, p, SendMode.tcp);
            }
        });

        Net.handleServer(PlayerDeathPacket.class, (id, packet) -> {
            packet.id = connections.get(id).id;
            Net.sendExcept(id, packet, SendMode.tcp);
        });
    }

    public void update(){
        if(!headless && !closing && Net.server() && state.is(State.menu)){
            closing = true;
            reset();
            ui.loadfrag.show("$text.server.closing");
            Timers.runTask(5f, () -> {
                Net.closeServer();
                ui.loadfrag.hide();
                closing = false;
            });
        }

        if(!state.is(State.menu) && Net.server()){
            sync();
        }
    }

    public void reset(){
        weapons.clear();
    }

    void sync(){

        if(Timers.get("serverSync", serverSyncTime)){
            //scan through all groups with syncable entities
            for(EntityGroup<?> group : Entities.getAllGroups()) {
                if(group.size() == 0 || !(group.all().first() instanceof SyncEntity)) continue;

                //get write size for one entity (adding 4, as you need to write the ID as well)
                int writesize = SyncEntity.getWriteSize((Class<? extends SyncEntity>)group.getType()) + 4;
                //amount of entities
                int amount = group.size();
                //maximum amount of entities per packet
                int maxsize = 64;

                //current buffer you're writing to
                ByteBuffer current = null;
                //number of entities written to this packet/buffer
                int written = 0;

                //for all the entities...
                for (int i = 0; i < amount; i++) {
                    //if the buffer is null, create a new one
                    if(current == null){
                        //calculate amount of entities to go into this packet
                        int csize = Math.min(amount-i, maxsize);
                        //create a byte array to write to
                        byte[] bytes = new byte[csize*writesize + 1 + 8];
                        //wrap it for easy writing
                        current = ByteBuffer.wrap(bytes);
                        current.putLong(TimeUtils.millis());
                        //write the group ID so the client knows which group this is
                        current.put((byte)group.getID());
                    }

                    SyncEntity entity = (SyncEntity) group.all().get(i);

                    //write ID to the buffer
                    current.putInt(entity.id);

                    int previous = current.position();
                    //write extra data to the buffer
                    entity.write(current);

                    written ++;

                    //if the packet is too big now...
                    if(written >= maxsize){
                        //send the packet.
                        SyncPacket packet = new SyncPacket();
                        packet.data = current.array();
                        Net.send(packet, SendMode.udp);

                        //reset data, send the next packet
                        current = null;
                        written = 0;
                    }
                }

                //make sure to send incomplete packets too
                if(current != null){
                    SyncPacket packet = new SyncPacket();
                    packet.data = current.array();
                    Net.send(packet, SendMode.udp);
                }
            }
        }

        if(Timers.get("serverItemSync", itemSyncTime)){
            StateSyncPacket packet = new StateSyncPacket();
            packet.items = state.inventory.getItems();
            packet.countdown = state.wavetime;
            packet.enemies = state.enemies;
            packet.wave = state.wave;
            packet.time = Timers.time();
            packet.timestamp = TimeUtils.millis();

            Net.send(packet, SendMode.udp);
        }
        /*
        if(Timers.get("serverBlockSync", blockSyncTime)){

            Array<NetConnection> connections = Net.getConnections();

            for(int i = 0; i < connections.size; i ++){
                int id = connections.get(i).id;
                Player player = this.connections.get(id);
                if(player == null) continue;
                int x = Mathf.scl2(player.x, tilesize);
                int y = Mathf.scl2(player.y, tilesize);
                int w = 22;
                int h = 16;
                sendBlockSync(id, x, y, w, h);
            }
        }*/
    }

    public void sendBlockSync(int client, int x, int y, int viewx, int viewy){
        BlockSyncPacket packet = new BlockSyncPacket();
        ByteArrayOutputStream bs = new ByteArrayOutputStream();

        //TODO compress stream

        try {
            DataOutputStream stream = new DataOutputStream(bs);

            stream.writeFloat(Timers.time());

            for (int rx = -viewx / 2; rx <= viewx / 2; rx++) {
                for (int ry = -viewy / 2; ry <= viewy / 2; ry++) {
                    Tile tile = world.tile(x + rx, y + ry);

                    if (tile == null || tile.entity == null) continue;

                    stream.writeInt(tile.packedPosition());
                    byte times = 0;

                    for(; times < tile.entity.timer.getTimes().length; times ++){
                        if(tile.entity.timer.getTimes()[times] <= 1f){
                            break;
                        }
                    }

                    stream.writeByte(times);

                    for(int i = 0; i < times; i ++){
                        stream.writeFloat(tile.entity.timer.getTimes()[i]);
                    }

                    stream.writeShort(tile.getPackedData());

                    tile.entity.write(stream);
                }
            }

        }catch (IOException e){
            throw new RuntimeException(e);
        }

        packet.stream = new ByteArrayInputStream(bs.toByteArray());

        Net.sendStream(client, packet);
    }
}
