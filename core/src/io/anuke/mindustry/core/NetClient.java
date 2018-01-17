package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.NetworkIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BaseBulletType;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.ui;

public class NetClient extends Module {
    public static final Color[] colorArray = {Color.ORANGE, Color.SCARLET, Color.LIME, Color.PURPLE,
            Color.GOLD, Color.PINK, Color.SKY, Color.GOLD, Color.VIOLET,
            Color.GREEN, Color.CORAL, Color.CYAN, Color.CHARTREUSE};
    boolean connecting = false;
    boolean gotData = false;
    boolean kicked = false;
    IntSet requests = new IntSet();
    IntSet recieved = new IntSet();
    float playerSyncTime = 2;
    float dataTimeout = 60*15; //13 seconds timeout

    public NetClient(){

        Net.handle(Connect.class, packet -> {
            Net.setClientLoaded(false);
            requests.clear();
            recieved.clear();
            connecting = true;
            gotData = false;
            kicked = false;

            Gdx.app.postRunnable(() -> {
                Vars.ui.loadfrag.hide();
                Vars.ui.loadfrag.show("$text.connecting.data");

                Entities.clear();

                ConnectPacket c = new ConnectPacket();
                c.name = Vars.player.name;
                c.android = Vars.android;
                Net.send(c, SendMode.tcp);
            });

            Timers.runTask(dataTimeout, () -> {
                if(!gotData){
                    Gdx.app.error("Mindustry", "Failed to load data!");
                    Vars.ui.loadfrag.hide();
                    Net.disconnect();
                }
            });
        });

        Net.handle(Disconnect.class, packet -> {
            if(kicked) return;

            Gdx.app.postRunnable(() -> {
                Timers.runFor(3f, Vars.ui.loadfrag::hide);

                GameState.set(State.menu);

                Vars.ui.showError("$text.disconnect");
                connecting = false;
            });
        });

        Net.handle(WorldData.class, data -> {
            Gdx.app.postRunnable(() -> {
                UCore.log("Recieved world data: " + data.stream.available() + " bytes.");
                NetworkIO.load(data.stream);
                Vars.player.set(Vars.control.core.worldx(), Vars.control.core.worldy() - Vars.tilesize*2);

                connecting = false;
                Vars.ui.loadfrag.hide();
                Vars.ui.join.hide();
                gotData = true;

                Net.send(new ConnectConfirmPacket(), SendMode.tcp);
                GameState.set(State.playing);
                Net.setClientLoaded(true);
            });
        });

        Net.handle(SyncPacket.class, packet -> {
            if(!gotData) return;

            ByteBuffer data = ByteBuffer.wrap(packet.data);

            byte groupid = data.get();

            EntityGroup<?> group = Entities.getGroup(groupid);

            while(data.position() < data.capacity()){
                int id = data.getInt();

                SyncEntity entity = (SyncEntity) group.getByID(id);

                if (entity == null || id == Vars.player.id) {
                    if (!requests.contains(id) && id != Vars.player.id) {
                        UCore.log("Requesting entity " + id, "group " + group.getType());
                        requests.add(id);
                        EntityRequestPacket req = new EntityRequestPacket();
                        req.id = id;
                        Net.send(req, SendMode.tcp);
                    }
                    data.position(data.position() + SyncEntity.getWriteSize((Class<? extends SyncEntity>) group.getType()));
                } else {
                    entity.read(data);
                }

            }
        });

        Net.handle(ShootPacket.class, packet -> {
            Player player = Vars.control.playerGroup.getByID(packet.playerid);

            Weapon weapon = (Weapon) Upgrade.getByID(packet.weaponid);
            Gdx.app.postRunnable(() -> weapon.shoot(player, packet.x, packet.y, packet.rotation));
        });

        Net.handle(PlacePacket.class, packet -> {
            Gdx.app.postRunnable(() ->{
                Recipe recipe = Recipes.getByResult(Block.getByID(packet.block));
                if(recipe != null) Vars.control.removeItems(recipe.requirements);
                Vars.control.input.placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false);
            });
        });

        Net.handle(BreakPacket.class, packet -> {
            Gdx.app.postRunnable(() -> Vars.control.input.breakBlockInternal(packet.x, packet.y, false));
        });

        Net.handle(StateSyncPacket.class, packet -> {
            Gdx.app.postRunnable(() -> {

                System.arraycopy(packet.items, 0, Vars.control.items, 0, packet.items.length);

                Vars.control.setWaveData(packet.enemies, packet.wave, packet.countdown);

                Timers.resetTime(packet.time + (float) (TimeUtils.timeSinceMillis(packet.timestamp) / 1000.0 * 60.0));

                Gdx.app.postRunnable(Vars.ui.hudfrag::updateItems);
            });
        });

        Net.handle(EnemySpawnPacket.class, spawn -> {
            Gdx.app.postRunnable(() -> {
                //duplicates.
                if(Vars.control.enemyGroup.getByID(spawn.id) != null ||
                        recieved.contains(spawn.id)) return;

                recieved.add(spawn.id);

                Enemy enemy = new Enemy(EnemyType.getByID(spawn.type));
                enemy.set(spawn.x, spawn.y);
                enemy.tier = spawn.tier;
                enemy.lane = spawn.lane;
                enemy.id = spawn.id;
                enemy.health = spawn.health;
                enemy.add();

                Effects.effect(Fx.spawn, enemy);
            });
        });

        Net.handle(EnemyDeathPacket.class, spawn -> {
            Gdx.app.postRunnable(() -> {
                Enemy enemy = Vars.control.enemyGroup.getByID(spawn.id);
                if (enemy != null) enemy.onDeath();
            });
        });

        Net.handle(BulletPacket.class, packet -> {
            //TODO shoot effects for enemies, clientside as well as serverside
            BulletType type = (BulletType) BaseBulletType.getByID(packet.type);
            Gdx.app.postRunnable(() -> {
                Entity owner = Vars.control.enemyGroup.getByID(packet.owner);
                new Bullet(type, owner, packet.x, packet.y, packet.angle).add();
            });
        });

        Net.handle(BlockDestroyPacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position % Vars.world.width(), packet.position / Vars.world.width());
            if(tile != null && tile.entity != null){
                Gdx.app.postRunnable(() ->{
                    if(tile.entity != null) tile.entity.onDeath(true);
                });
            }
        });

        Net.handle(BlockUpdatePacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position % Vars.world.width(), packet.position / Vars.world.width());
            if(tile != null && tile.entity != null){
                tile.entity.health = packet.health;
            }
        });

        Net.handle(BlockSyncPacket.class, packet -> {
            if(!gotData) return;

            DataInputStream stream = new DataInputStream(packet.stream);

            Gdx.app.postRunnable(() -> {
                try {

                    float time = stream.readFloat();
                    float elapsed = Timers.time() - time;

                    while (stream.available() > 0) {
                        int pos = stream.readInt();

                        //TODO what if there's no entity? new code
                        Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());

                        byte times = stream.readByte();

                        for (int i = 0; i < times; i++) {
                            tile.entity.timer.getTimes()[i] = stream.readFloat();
                        }

                        short data = stream.readShort();
                        tile.setPackedData(data);

                        tile.entity.readNetwork(stream, elapsed);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e){
                    e.printStackTrace();
                    //do nothing else...
                    //TODO fix
                }
            });

        });

        Net.handle(DisconnectPacket.class, packet -> {
            Player player = Vars.control.playerGroup.getByID(packet.playerid);

            if(player != null){
                Gdx.app.postRunnable(player::remove);
            }
        });

        Net.handle(PlayerSpawnPacket.class, packet -> {
            Gdx.app.postRunnable(() -> {
                //duplicates.
                if(Vars.control.enemyGroup.getByID(packet.id) != null ||
                        recieved.contains(packet.id)) return;
                recieved.add(packet.id);

                Player player = new Player();
                player.x = packet.x;
                player.y = packet.y;
                player.isAndroid = packet.android;
                player.name = packet.name;
                player.id = packet.id;
                player.weaponLeft = (Weapon) Upgrade.getByID(packet.weaponleft);
                player.weaponRight = (Weapon) Upgrade.getByID(packet.weaponright);

                player.interpolator.last.set(player.x, player.y);
                player.interpolator.target.set(player.x, player.y);
                player.add();
            });
        });

        Net.handle(ChatPacket.class, packet -> Gdx.app.postRunnable(() -> Vars.ui.chatfrag.addMessage(packet.text, Vars.netClient.colorizeName(packet.id, packet.name))));

        Net.handle(KickPacket.class, packet -> {
            kicked = true;
            Net.disconnect();
            GameState.set(State.menu);
            Gdx.app.postRunnable(() -> Vars.ui.showError("$text.server.kicked." + packet.reason.name()));
        });

        Net.handle(WeaponSwitchPacket.class, packet -> {
            Gdx.app.postRunnable(() -> {
                Player player = Vars.control.playerGroup.getByID(packet.playerid);

                if(player == null) return;

                player.weaponLeft = (Weapon)Upgrade.getByID(packet.left);
                player.weaponRight = (Weapon)Upgrade.getByID(packet.right);
            });
        });

        Net.handle(BlockTapPacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position);
            if(tile != null) tile.block().tapped(tile);
        });

        Net.handle(BlockConfigPacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position);
            if(tile != null) tile.block().configure(tile, packet.data);
        });

        Net.handle(GameOverPacket.class, packet -> {
            kicked = true;
            Gdx.app.postRunnable(ui.restart::show);
        });

        Net.handle(FriendlyFireChangePacket.class, packet -> Vars.control.setFriendlyFire(packet.enabled));
    }

    @Override
    public void update(){
        if(!Net.client() || !Net.active()) return;

        if(!GameState.is(State.menu) && Net.active()){
            if(gotData) sync();
        }else if(!connecting){
            Net.disconnect();
        }
    }

    public void beginConnecting(){
        connecting = true;
    }

    public void disconnectQuietly(){
        kicked = true;
        Net.disconnect();
    }

    public String colorizeName(int id, String name){
        return name == null ? null : "[#" + colorArray[id % colorArray.length].toString().toUpperCase() + "]" + name;
    }

    public void handleBlockConfig(Tile tile, byte data){
        BlockConfigPacket packet = new BlockConfigPacket();
        packet.data = data;
        packet.position = tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public void handleBlockTap(Tile tile){
        BlockTapPacket packet = new BlockTapPacket();
        packet.position = tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public void handleWeaponSwitch(){
        WeaponSwitchPacket packet = new WeaponSwitchPacket();
        packet.left = Vars.player.weaponLeft.id;
        packet.right = Vars.player.weaponRight.id;
        packet.playerid = Vars.player.id;
        Net.send(packet, SendMode.tcp);
    }

    public void handleUpgrade(Weapon weapon){
        UpgradePacket packet = new UpgradePacket();
        packet.id = weapon.id;
        Net.send(packet, SendMode.tcp);
    }

    public void handleSendMessage(String message){
        ChatPacket packet = new ChatPacket();
        packet.text = message;
        packet.name = Vars.player.name;
        packet.id = Vars.player.id;
        Net.send(packet, SendMode.tcp);

        Vars.ui.chatfrag.addMessage(packet.text, colorizeName(Vars.player.id, Vars.player.name));
    }

    public void handleShoot(Weapon weapon, float x, float y, float angle){
        ShootPacket packet = new ShootPacket();
        packet.weaponid = weapon.id;
        packet.x = x;
        packet.y = y;
        packet.rotation = angle;
        Net.send(packet, SendMode.udp);
    }

    public void handlePlace(int x, int y, Block block, int rotation){
        PlacePacket packet = new PlacePacket();
        packet.x = (short)x;
        packet.y = (short)y;
        packet.rotation = (byte)rotation;
        packet.playerid = Vars.player.id;
        packet.block = block.id;
        Net.send(packet, SendMode.tcp);
    }

    public void handleBreak(int x, int y){
        BreakPacket packet = new BreakPacket();
        packet.x = (short)x;
        packet.y = (short)y;
        Net.send(packet, SendMode.tcp);
    }

    void sync(){
        if(Timers.get("syncPlayer", playerSyncTime)){
            byte[] bytes = new byte[Vars.player.getWriteSize()];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            Vars.player.write(buffer);

            PositionPacket packet = new PositionPacket();
            packet.data = bytes;
            Net.send(packet, SendMode.udp);
        }

        if(Timers.get("updatePing", 60)){
            Net.updatePing();
        }
    }
}
