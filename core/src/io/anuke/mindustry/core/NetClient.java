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
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.io.NetworkIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.Syncable;
import io.anuke.mindustry.net.Syncable.Interpolator;
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
import io.anuke.ucore.modules.Module;

import java.io.DataInputStream;
import java.io.IOException;

public class NetClient extends Module {
    public static final Color[] colorArray = {Color.ORANGE, Color.SCARLET, Color.LIME, Color.PURPLE,
            Color.GOLD, Color.PINK, Color.SKY, Color.GOLD, Color.VIOLET,
            Color.GREEN, Color.CORAL, Color.CYAN, Color.CHARTREUSE};
    boolean connecting = false;
    boolean gotData = false;
    boolean kicked = false;
    IntSet requests = new IntSet();
    float playerSyncTime = 2;
    float dataTimeout = 60*10;

    public NetClient(){

        Net.handle(Connect.class, packet -> {
            requests.clear();
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
            });
        });

        Net.handle(SyncPacket.class, packet -> {
            if(!gotData) return;

            //TODO awful code
            for(int i = 0; i < packet.ids.length; i ++){
                int id = packet.ids[i];
                if(id != Vars.player.id){
                    Entity entity;
                    if(i >= packet.enemyStart){
                        entity = Vars.control.enemyGroup.getByID(id);
                    }else {
                        entity = Vars.control.playerGroup.getByID(id);
                    }

                    Syncable sync = ((Syncable)entity);

                    if(sync == null){
                        Gdx.app.error("Mindustry", "Unknown entity ID: " + id + " " + (i >= packet.enemyStart ? "(enemy)" : "(player)"));
                        if(!requests.contains(id)){
                            requests.add(id);
                            Gdx.app.error("Mindustry", "Sending entity request: " + id);
                            EntityRequestPacket req = new EntityRequestPacket();
                            req.id = id;
                            Net.send(req, SendMode.tcp);
                        }
                        continue;
                    }

                    //augh
                    ((Interpolator)sync.getInterpolator()).type.read(entity, packet.data[i]);
                }
            }
        });

        Net.handle(ShootPacket.class, packet -> {
            Player player = Vars.control.playerGroup.getByID(packet.playerid);

            Weapon weapon = (Weapon) Upgrade.getByID(packet.weaponid);
            Gdx.app.postRunnable(() -> weapon.shoot(player, packet.x, packet.y, packet.rotation));
        });

        Net.handle(PlacePacket.class, packet -> {
            Gdx.app.postRunnable(() -> Vars.control.input.placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false));
        });

        Net.handle(BreakPacket.class, packet -> {
            Gdx.app.postRunnable(() -> Vars.control.input.breakBlockInternal(packet.x, packet.y, false));
        });

        Net.handle(StateSyncPacket.class, packet -> {
            System.arraycopy(packet.items, 0, Vars.control.items, 0, packet.items.length);

            Vars.control.setWaveData(packet.enemies, packet.wave, packet.countdown);

            Timers.resetTime(packet.time + (float)(TimeUtils.timeSinceMillis(packet.timestamp) / 1000.0 * 60.0));

            Gdx.app.postRunnable(Vars.ui.hudfrag::updateItems);
        });

        Net.handle(EnemySpawnPacket.class, spawn -> {
            Gdx.app.postRunnable(() -> {
                //duplicates.
                if(Vars.control.enemyGroup.getByID(spawn.id) != null) return;

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
                    while (stream.available() > 0) {
                        int pos = stream.readInt();

                        //TODO what if there's no entity? new code
                        Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());

                        byte times = stream.readByte();

                        for (int i = 0; i < times; i++) {
                            tile.entity.timer.getTimes()[i] = stream.readFloat();
                        }

                        tile.entity.read(stream);
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

        Net.handle(Player.class, player -> {
            Gdx.app.postRunnable(() -> {
                //duplicates.
                if(Vars.control.enemyGroup.getByID(player.id) != null) return;
                player.getInterpolator().last.set(player.x, player.y);
                player.getInterpolator().target.set(player.x, player.y);
                player.add();
            });
        });

        Net.handle(ChatPacket.class, packet -> Gdx.app.postRunnable(() -> Vars.ui.chatfrag.addMessage(packet.text, Vars.netClient.colorizeName(packet.id, packet.name))));

        Net.handle(KickPacket.class, packet -> {
            kicked = true;
            Net.disconnect();
            GameState.set(State.menu);
            Gdx.app.postRunnable(() -> Vars.ui.showError("$text.server.kicked." + KickReason.values()[packet.reason].name()));
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
            PositionPacket packet = new PositionPacket();
            packet.data = Vars.player.getInterpolator().type.write(Vars.player);
            Net.send(packet, SendMode.udp);
        }

        if(Timers.get("updatePing", 60)){
            Net.updatePing();
        }
    }
}
