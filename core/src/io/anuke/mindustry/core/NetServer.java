package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class NetServer extends Module{
    /**Maps connection IDs to players.*/
    IntMap<Player> connections = new IntMap<>();
    ObjectMap<String, ByteArray> weapons = new ObjectMap<>();
    float serverSyncTime = 4, itemSyncTime = 10, blockSyncTime = 120;
    boolean closing = false;

    public NetServer(){

        Net.handleServer(Connect.class, connect -> UCore.log("Connection found: " + connect.addressTCP));

        Net.handleServer(ConnectPacket.class, packet -> {
            int id = Net.getLastConnection();

            UCore.log("Sending world data to client (ID="+id+")");

            Gdx.app.postRunnable(() -> {
                Player player = new Player();
                player.clientid = id;
                player.name = packet.name;
                player.isAndroid = packet.android;
                player.set(Vars.control.core.worldx(), Vars.control.core.worldy() - Vars.tilesize*2);
                player.interpolator.last.set(player.x, player.y);
                player.interpolator.target.set(player.x, player.y);
                connections.put(id, player);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                NetworkIO.write(player.id, weapons.get(packet.name, new ByteArray()), stream);

                UCore.log("Packed " + stream.size() + " uncompressed bytes of data.");

                WorldData data = new WorldData();
                data.stream = new ByteArrayInputStream(stream.toByteArray());

                Net.sendStream(id, data);
            });
        });

        Net.handleServer(ConnectConfirmPacket.class, packet -> {
            Player player = connections.get(Net.getLastConnection());

            if(player == null) return;

            Gdx.app.postRunnable(player::add);
            sendMessage("[accent]"+Bundles.format("text.server.connected", player.name));
        });

        Net.handleServer(Disconnect.class, packet -> {
            Player player = connections.get(packet.id);

            if(player == null) {
                sendMessage("[accent]"+Bundles.format("text.server.disconnected", "<???>"));
                return;
            }

            sendMessage("[accent]"+Bundles.format("text.server.disconnected", player.name));
            Gdx.app.postRunnable(() -> {
                player.remove();

                DisconnectPacket dc = new DisconnectPacket();
                dc.playerid = player.id;

                Net.send(dc, SendMode.tcp);
            });
        });

        Net.handleServer(PositionPacket.class, pos -> {
            Player player = connections.get(Net.getLastConnection());
            player.read(ByteBuffer.wrap(pos.data));
        });

        Net.handleServer(ShootPacket.class, packet -> {
            Player player = connections.get(Net.getLastConnection());

            Weapon weapon = (Weapon)Upgrade.getByID(packet.weaponid);
            weapon.shoot(player, packet.x, packet.y, packet.rotation);
            packet.playerid = player.id;

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.udp);
        });

        Net.handleServer(PlacePacket.class, packet -> {
            Gdx.app.postRunnable(() -> Vars.control.input.placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false));
            packet.playerid = connections.get(Net.getLastConnection()).id;

            Recipe recipe = Recipes.getByResult(Block.getByID(packet.block));
            if(recipe != null){
                for(ItemStack stack : recipe.requirements){
                    Vars.control.removeItem(stack);
                }
            }

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.tcp);
        });

        Net.handleServer(BreakPacket.class, packet -> {
            Gdx.app.postRunnable(() -> Vars.control.input.breakBlockInternal(packet.x, packet.y, false));
            packet.playerid = connections.get(Net.getLastConnection()).id;

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.tcp);
        });

        Net.handleServer(ChatPacket.class, packet -> {
            Player player = connections.get(Net.getLastConnection());

            if(player == null){
                Gdx.app.error("Mindustry", "Could not find player for chat: " + Net.getLastConnection());
                return; //GHOSTS AAAA
            }

            packet.name = player.name;
            packet.id = player.id;
            Net.sendExcept(player.clientid, packet, SendMode.tcp);
            Gdx.app.postRunnable(() -> Vars.ui.chatfrag.addMessage(packet.text, Vars.netClient.colorizeName(packet.id, packet.name)));
        });

        Net.handleServer(UpgradePacket.class, packet -> {
            Player player = connections.get(Net.getLastConnection());

            Weapon weapon = (Weapon)Upgrade.getByID(packet.id);

            if(!weapons.containsKey(player.name)) weapons.put(player.name, new ByteArray());
            if(!weapons.get(player.name).contains(weapon.id)) weapons.get(player.name).add(weapon.id);

            Vars.control.removeItems(UpgradeRecipes.get(weapon));
        });

        Net.handleServer(WeaponSwitchPacket.class, packet -> {
            Player player = connections.get(Net.getLastConnection());

            if(player == null) return;

            packet.playerid = player.id;

            player.weaponLeft = (Weapon)Upgrade.getByID(packet.left);
            player.weaponRight = (Weapon)Upgrade.getByID(packet.right);

            Net.sendExcept(player.clientid, packet, SendMode.tcp);
        });

        Net.handleServer(BlockTapPacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position);
            tile.block().tapped(tile);

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.tcp);
        });

        Net.handleServer(BlockConfigPacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position);
            if(tile != null) tile.block().configure(tile, packet.data);

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.tcp);
        });

        Net.handleServer(EntityRequestPacket.class, packet -> {
            int id = packet.id;
            int dest = Net.getLastConnection();
            Gdx.app.postRunnable(() -> {
                if(Vars.control.playerGroup.getByID(id) != null){
                    Player player = Vars.control.playerGroup.getByID(id);
                    PlayerSpawnPacket p = new PlayerSpawnPacket();
                    p.x = player.x;
                    p.y = player.y;
                    p.id = player.id;
                    p.name = player.name;
                    p.weaponleft = player.weaponLeft.id;
                    p.weaponright = player.weaponRight.id;
                    p.android = player.isAndroid;
                    Net.sendTo(dest, p, SendMode.tcp);
                    Gdx.app.error("Mindustry", "Replying to entity request ("+Net.getLastConnection()+"): player, " + id);
                }else if (Vars.control.enemyGroup.getByID(id) != null){
                    Enemy enemy = Vars.control.enemyGroup.getByID(id);
                    EnemySpawnPacket e = new EnemySpawnPacket();
                    e.x = enemy.x;
                    e.y = enemy.y;
                    e.id = enemy.id;
                    e.tier = (byte)enemy.tier;
                    e.lane = (byte)enemy.lane;
                    e.type = enemy.type.id;
                    e.health = (short)enemy.health;
                    Net.sendTo(dest, e, SendMode.tcp);
                    Gdx.app.error("Mindustry", "Replying to entity request("+Net.getLastConnection()+"): enemy, " + id);
                }else{
                    Gdx.app.error("Mindustry", "Entity request target not found!");
                }
            });
        });
    }

    public void sendMessage(String message){
        ChatPacket packet = new ChatPacket();
        packet.name = null;
        packet.text = message;
        Net.send(packet, SendMode.tcp);

        Gdx.app.postRunnable(() -> Vars.ui.chatfrag.addMessage(message, null));
    }

    public void handleFriendlyFireChange(boolean enabled){
        FriendlyFireChangePacket packet = new FriendlyFireChangePacket();
        packet.enabled = enabled;

        sendMessage(enabled ? "[accent]Friendly fire enabled." : "[accent]Friendly fire disabled.");

        Net.send(packet, SendMode.tcp);
    }

    public void handleGameOver(){
        Net.send(new GameOverPacket(), SendMode.tcp);
        Timers.runTask(30f, () -> GameState.set(State.menu));
    }

    public void handleBullet(BulletType type, Entity owner, float x, float y, float angle, short damage){
        BulletPacket packet = new BulletPacket();
        packet.x = x;
        packet.y = y;
        packet.angle = angle;
        packet.damage = damage;
        packet.owner = owner.id;
        packet.type = type.id;
        Net.send(packet, SendMode.udp);
    }

    public void handleEnemyDeath(Enemy enemy){
        EnemyDeathPacket packet = new EnemyDeathPacket();
        packet.id = enemy.id;
        Net.send(packet, SendMode.tcp);
    }

    public void handleBlockDestroyed(TileEntity entity){
        BlockDestroyPacket packet = new BlockDestroyPacket();
        packet.position = entity.tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public void handleBlockDamaged(TileEntity entity){
        BlockUpdatePacket packet = new BlockUpdatePacket();
        packet.health = entity.health;
        packet.position = entity.tile.packedPosition();
        Net.send(packet, SendMode.udp);
    }

    public void update(){
        if(!Net.server()) return;

        if(!GameState.is(State.menu) && Net.active()){
            sync();
        }else if(!closing){
            closing = true;
            weapons.clear();
            Vars.ui.loadfrag.show("$text.server.closing");
            Timers.runTask(5f, () -> {
                Net.closeServer();
                Vars.ui.loadfrag.hide();
                closing = false;
            });
        }
    }

    void sync(){

        if(Timers.get("serverSync", serverSyncTime)){
            //scan through all groups with syncable entities
            for(EntityGroup<?> group : Entities.getAllGroups()) {
                if(group.amount() == 0 || !(group.all().first() instanceof SyncEntity)) continue;

                //get write size for one entity (adding 4, as you need to write the ID as well)
                int writesize = SyncEntity.getWriteSize((Class<? extends SyncEntity>)group.getType()) + 4;
                //amount of entities
                int amount = group.amount();
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
                        byte[] bytes = new byte[csize*writesize + 1];
                        //wrap it for easy writing
                        current = ByteBuffer.wrap(bytes);
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
            packet.items = Vars.control.items;
            packet.countdown = Vars.control.getWaveCountdown();
            packet.enemies = Vars.control.getEnemiesRemaining();
            packet.wave = Vars.control.getWave();
            packet.time = Timers.time();
            packet.timestamp = TimeUtils.millis();

            Net.send(packet, SendMode.udp);
        }

        if(Timers.get("serverBlockSync", blockSyncTime)){

            IntArray connections = Net.getConnections();

            for(int i = 0; i < connections.size; i ++){
                int id = connections.get(i);
                Player player = this.connections.get(id);
                if(player == null) continue;
                int x = Mathf.scl2(player.x, Vars.tilesize);
                int y = Mathf.scl2(player.y, Vars.tilesize);
                int w = 16;
                int h = 12;
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
                    Tile tile = Vars.world.tile(x + rx, y + ry);

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
