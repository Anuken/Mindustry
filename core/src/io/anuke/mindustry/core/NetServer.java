package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Administration;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Placement;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class NetServer extends Module{
    private final static float serverSyncTime = 4, itemSyncTime = 10, blockSyncTime = 120;
    private final static boolean sendBlockSync = false;

    private final static int timerEntitySync = 0;
    private final static int timerStateSync = 1;
    private final static int timerBlockSync = 2;

    public final Administration admins = new Administration();

    /**Maps connection IDs to players.*/
    private IntMap<Player> connections = new IntMap<>();
    private ObjectMap<String, ByteArray> weapons = new ObjectMap<>();
    private boolean closing = false;
    private Timer timer = new Timer(5);

    public NetServer(){

        Events.on(GameOverEvent.class, () -> weapons.clear());

        Net.handleServer(Connect.class, (id, connect) -> {
            if(admins.isBanned(connect.addressTCP)){
                Net.kickConnection(id, KickReason.banned);
            }
        });

        Net.handleServer(ConnectPacket.class, (id, packet) -> {
            if(Net.getConnection(id) == null ||
                    admins.isBanned(Net.getConnection(id).address)) return;

            admins.setKnownName(Net.getConnection(id).address, packet.name);

            if(packet.version != Version.build && Version.build != -1){
                if(packet.version == -1){
                    admins.banPlayer(Net.getConnection(id).address);
                    Net.kickConnection(id, KickReason.banned);
                }else {
                    Net.kickConnection(id, packet.version > Version.build ? KickReason.serverOutdated : KickReason.clientOutdated);
                }

                return;
            }

            Log.info("Sending data to player '{0}' / {1}", packet.name, id);

            Player player = new Player();
            player.isAdmin = admins.isAdmin(Net.getConnection(id).address);
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
            NetworkIO.writeWorld(player, weapons.get(player.name, new ByteArray()), stream);
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

            if(!Placement.validPlace(packet.x, packet.y, Block.getByID(packet.block))) return;

            Recipe recipe = Recipes.getByResult(Block.getByID(packet.block));

            if(recipe == null) return;

            state.inventory.removeItems(recipe.requirements);

            Placement.placeBlock(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false);

            Net.send(packet, SendMode.tcp);
        });

        Net.handleServer(BreakPacket.class, (id, packet) -> {
            packet.playerid = connections.get(id).id;

            if(!Placement.validBreak(packet.x, packet.y)) return;

            Placement.breakBlock(packet.x, packet.y, true, false);

            Net.send(packet, SendMode.tcp);
        });

        Net.handleServer(ChatPacket.class, (id, packet) -> {
            if(!Timers.get("chatFlood" + id, 20)){
                ChatPacket warn = new ChatPacket();
                warn.text = "[scarlet]You are sending messages too quickly.";
                Net.sendTo(id, warn, SendMode.tcp);
                return;
            }
            Player player = connections.get(id);
            packet.name = player.name;
            packet.id = player.id;
            Net.send(packet, SendMode.tcp);
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

        Net.handleServer(AdministerRequestPacket.class, (id, packet) -> {
            Player player = connections.get(id);

            if(!player.isAdmin){
                Log.err("ACCESS DENIED: Player {0} / {1} attempted to perform admin action without proper security access.",
                        player.name, Net.getConnection(player.clientid).address);
                return;
            }

            Player other = playerGroup.getByID(packet.id);

            if(other == null){
                Log.err("{0} attempted to perform admin action on nonexistant player.", player.name);
                return;
            }

            if(packet.action == AdminAction.ban){
                admins.banPlayer(Net.getConnection(other.clientid).address);
                Net.kickConnection(other.clientid, KickReason.banned);
                Log.info("&lc{0} has banned {1}.", player.name, other.name);
            }else if(packet.action == AdminAction.kick){
                Net.kickConnection(other.clientid, KickReason.kick);
                Log.info("&lc{0} has kicked {1}.", player.name, other.name);
            }
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

        if(timer.get(timerEntitySync, serverSyncTime)){
            //scan through all groups with syncable entities
            for(EntityGroup<?> group : Entities.getAllGroups()) {
                if(group.size() == 0 || !(group.all().iterator().next() instanceof SyncEntity)) continue;

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

        if(timer.get(timerStateSync, itemSyncTime)){
            StateSyncPacket packet = new StateSyncPacket();
            packet.items = state.inventory.getItems();
            packet.countdown = state.wavetime;
            packet.enemies = state.enemies;
            packet.wave = state.wave;
            packet.time = Timers.time();
            packet.timestamp = TimeUtils.millis();

            Net.send(packet, SendMode.udp);
        }


        if(sendBlockSync && timer.get(timerBlockSync, blockSyncTime)){

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
        }
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

                    if (tile == null || tile.entity == null || !tile.block().syncEntity()) continue;

                    stream.writeInt(tile.packedPosition());
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
