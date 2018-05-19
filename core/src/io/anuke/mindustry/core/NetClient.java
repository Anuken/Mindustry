package io.anuke.mindustry.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.UpgradeRecipes;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.Placement;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BaseBulletType;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Timer;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class NetClient extends Module {
    private final static float dataTimeout = 60*18; //18 seconds timeout
    private final static float playerSyncTime = 2;
    private final static int maxRequests = 50;

    private Timer timer = new Timer(5);
    private boolean connecting = false;
    private boolean kicked = false;
    private IntSet recieved = new IntSet();
    private IntMap<Entity> recent = new IntMap<>();
    private int requests = 0;
    private float timeoutTime = 0f; //data timeout counter

    public NetClient(){

        Net.handleClient(Connect.class, packet -> {
            player.isAdmin = false;

            Net.setClientLoaded(false);
            recieved.clear();
            recent.clear();
            timeoutTime = 0f;
            connecting = true;
            kicked = false;

            ui.chatfrag.clearMessages();
            ui.loadfrag.hide();
            ui.loadfrag.show("$text.connecting.data");

            Entities.clear();

            ConnectPacket c = new ConnectPacket();
            c.name = player.name;
            c.android = mobile;
            c.color = Color.rgba8888(player.color);
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
            if (kicked) return;

            Timers.runTask(3f, ui.loadfrag::hide);

            state.set(State.menu);

            ui.showError("$text.disconnect");
            connecting = false;

            Platform.instance.updateRPC();
        });

        Net.handleClient(WorldData.class, data -> {
            Log.info("Recieved world data: {0} bytes.", data.stream.available());
            NetworkIO.loadWorld(data.stream);
            player.set(world.getSpawnX(), world.getSpawnY());

            finishConnecting();
        });

        Net.handleClient(CustomMapPacket.class, packet -> {
            Log.info("Recieved custom map: {0} bytes.", packet.stream.available());

            //custom map is always sent before world data
            Map map = NetworkIO.loadMap(packet.stream);

            world.maps().setNetworkMap(map);

            MapAckPacket ack = new MapAckPacket();
            Net.send(ack, SendMode.tcp);
        });

        Net.handleClient(SyncPacket.class, packet -> {
            if (connecting) return;
            int players = 0;
            int enemies = 0;

            ByteBuffer data = ByteBuffer.wrap(packet.data);
            long time = data.getLong();

            byte groupid = data.get();

            EntityGroup<?> group = Entities.getGroup(groupid);

            while (data.position() < data.capacity()) {
                int id = data.getInt();

                SyncEntity entity = (SyncEntity) group.getByID(id);

                if(entity instanceof Player) players ++;
                if(entity instanceof Enemy) enemies ++;

                if (entity == null || id == player.id) {
                    if (id != player.id && requests < maxRequests) {
                        EntityRequestPacket req = new EntityRequestPacket();
                        req.id = id;
                        req.group = groupid;
                        Net.send(req, SendMode.udp);
                        requests ++;
                    }
                    data.position(data.position() + SyncEntity.getWriteSize((Class<? extends SyncEntity>) group.getType()));
                } else {
                    entity.read(data, time);
                }
            }

            if(debugNet){
                clientDebug.setSyncDebug(players, enemies);
            }
        });

        Net.handleClient(StateSyncPacket.class, packet -> {

            System.arraycopy(packet.items, 0, state.inventory.getItems(), 0, packet.items.length);

            state.enemies = packet.enemies;
            state.wavetime = packet.countdown;
            state.wave = packet.wave;

            ui.hudfrag.updateItems();
        });

        Net.handleClient(PlacePacket.class, (packet) -> {
            Placement.placeBlock(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, Timers.get("placeblocksound", 10));

            if(packet.playerid == player.id){
                Tile tile = world.tile(packet.x, packet.y);
                if(tile != null) Block.getByID(packet.block).placed(tile);
            }
        });

        Net.handleClient(BreakPacket.class, (packet) ->
                Placement.breakBlock(packet.x, packet.y, true, Timers.get("breakblocksound", 10)));

        Net.handleClient(EntitySpawnPacket.class, packet -> {
            EntityGroup group = packet.group;

            //duplicates.
            if (group.getByID(packet.entity.id) != null ||
                    recieved.contains(packet.entity.id)) return;

            recieved.add(packet.entity.id);
            recent.put(packet.entity.id, packet.entity);

            packet.entity.add();

            Log.info("Recieved entity {0}", packet.entity.id);
        });

        Net.handleClient(EnemyDeathPacket.class, packet -> {
            Enemy enemy = enemyGroup.getByID(packet.id);
            if (enemy != null){
                enemy.type.onDeath(enemy, true);
            }else if(recent.get(packet.id) != null){
                recent.get(packet.id).remove();
            }else{
                Log.err("Got remove for null entity! {0}", packet.id);
            }
            recieved.add(packet.id);
        });

        Net.handleClient(BulletPacket.class, packet -> {
            //TODO shoot effects for enemies, clientside as well as serverside
            BulletType type = (BulletType) BaseBulletType.getByID(packet.type);
            Entity owner = enemyGroup.getByID(packet.owner);
            new Bullet(type, owner, packet.x, packet.y, packet.angle).add();
        });

        Net.handleClient(BlockDestroyPacket.class, packet -> {
            Tile tile = world.tile(packet.position % world.width(), packet.position / world.width());
            if (tile != null && tile.entity != null) {
                tile.entity.onDeath(true);
            }
        });

        Net.handleClient(BlockUpdatePacket.class, packet -> {
            Tile tile = world.tile(packet.position % world.width(), packet.position / world.width());
            if (tile != null && tile.entity != null) {
                tile.entity.health = packet.health;
            }
        });

        Net.handleClient(DisconnectPacket.class, packet -> {
            Player player = playerGroup.getByID(packet.playerid);

            if (player != null) {
                player.remove();
            }

            Platform.instance.updateRPC();
        });

        Net.handleClient(KickPacket.class, packet -> {
            kicked = true;
            Net.disconnect();
            state.set(State.menu);
            if(!packet.reason.quiet) ui.showError("$text.server.kicked." + packet.reason.name());
            ui.loadfrag.hide();
        });

        Net.handleClient(GameOverPacket.class, packet -> {
            if(world.getCore().block() != ProductionBlocks.core &&
                    world.getCore().entity != null){
                world.getCore().entity.onDeath(true);
            }
            kicked = true;
            ui.restart.show();
        });

        Net.handleClient(FriendlyFireChangePacket.class, packet -> state.friendlyFire = packet.enabled);

        Net.handleClient(ItemTransferPacket.class, packet -> {
            Runnable r = () -> {
                Tile tile = world.tile(packet.position);
                if (tile == null || tile.entity == null) return;
                Tile next = tile.getNearby(packet.rotation);
                tile.entity.items[packet.itemid] --;
                next.block().handleItem(Item.getByID(packet.itemid), next, tile);
            };

            threads.run(r);
        });

        Net.handleClient(ItemSetPacket.class, packet -> {
            Runnable r = () -> {
                Tile tile = world.tile(packet.position);
                if (tile == null || tile.entity == null) return;
                tile.entity.items[packet.itemid] = packet.amount;
            };

            threads.run(r);
        });

        Net.handleClient(ItemOffloadPacket.class, packet -> {
            Runnable r = () -> {
                Tile tile = world.tile(packet.position);
                if (tile == null || tile.entity == null) return;
                Tile next = tile.getNearby(tile.getRotation());
                next.block().handleItem(Item.getByID(packet.itemid), next, tile);
            };

            threads.run(r);
        });

        Net.handleClient(NetErrorPacket.class, packet -> {
            ui.showError(packet.message);
            disconnectQuietly();
        });

        Net.handleClient(PlayerAdminPacket.class, packet -> {
            Player player = playerGroup.getByID(packet.id);
            player.isAdmin = packet.admin;
            ui.listfrag.rebuild();
        });

        Net.handleClient(TracePacket.class, packet -> {
            Player player = playerGroup.getByID(packet.info.playerid);
            ui.traces.show(player, packet.info);
        });

        Net.handleClient(UpgradePacket.class, packet -> {
            Weapon weapon = (Weapon) Upgrade.getByID(packet.id);

            state.inventory.removeItems(UpgradeRecipes.get(weapon));
            control.upgrades().addWeapon(weapon);
            ui.hudfrag.updateWeapons();
            Effects.sound("purchase");
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
                kicked = true;
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
        Timers.runTask(1f, () -> Net.send(new ConnectConfirmPacket(), SendMode.tcp));
        Timers.runTask(40f, Platform.instance::updateRPC);
    }

    public void beginConnecting(){
        connecting = true;
    }

    public void disconnectQuietly(){
        kicked = true;
        Net.disconnect();
    }

    public void clearRecieved(){
        recieved.clear();
    }

    void sync(){
        requests = 0;

        if(timer.get(0, playerSyncTime)){

            byte[] bytes = new byte[player.getWriteSize() + 8];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.putLong(TimeUtils.millis());
            player.write(buffer);

            PositionPacket packet = new PositionPacket();
            packet.data = bytes;
            Net.send(packet, SendMode.udp);
        }

        if(timer.get(1, 60)){
            Net.updatePing();
        }
    }
}
