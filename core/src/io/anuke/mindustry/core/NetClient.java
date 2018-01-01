package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.io.NetworkIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.net.Syncable;
import io.anuke.mindustry.net.Syncable.Interpolator;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BaseBulletType;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.modules.Module;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

public class NetClient extends Module {
    boolean connecting = false;
    boolean gotEntities = false;
    float playerSyncTime = 3;

    public NetClient(){

        Net.handle(Connect.class, packet -> {
            connecting = true;
            gotEntities = false;
            Gdx.app.postRunnable(() -> {
                Vars.ui.hideLoading();
                Vars.ui.showLoading("$text.connecting.data");
            });
        });

        Net.handle(Disconnect.class, packet -> {
            Gdx.app.postRunnable(() -> {
                Timers.runFor(3f, () -> {
                    Vars.ui.hideLoading();
                });

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

                GameState.set(State.playing);
                connecting = false;
                Vars.ui.hideLoading();
                Vars.ui.hideJoinGame();
            });
        });

        Net.handle(EntityDataPacket.class, data -> {

            Gdx.app.postRunnable(() -> {
                Timers.run(10f, () -> { //TODO hack. should only run once world data is recieved
                    Vars.control.playerGroup.remap(Vars.player, data.playerid);

                    for (Player player : data.players) {
                        if (player.id != data.playerid) {
                            player.add();
                        }
                    }

                    UCore.log("Recieved entities: " + Arrays.toString(data.players) + " player ID: " + data.playerid);
                    gotEntities = true;
                });
            });
        });

        Net.handle(SyncPacket.class, packet -> {
            if(!gotEntities) return;

            //TODO awful code
            for(int i = 0; i < packet.ids.length; i ++){
                int id = packet.ids[i];
                if(id != Vars.player.id){
                    Entity entity = null;
                    if(id >= packet.enemyStart){
                        entity = Vars.control.enemyGroup.getByID(id);
                    }else {
                        entity = Vars.control.playerGroup.getByID(id);
                    }

                    Syncable sync = ((Syncable)entity);

                    if(sync == null) continue;

                    //augh
                    ((Interpolator)sync.getInterpolator()).type.read(entity, packet.data[i]);
                }
            }
        });

        Net.handle(ShootPacket.class, packet -> {
            Player player = Vars.control.playerGroup.getByID(packet.playerid);

            Weapon weapon = Weapon.values()[packet.weaponid];
            weapon.shoot(player, packet.x, packet.y, packet.rotation);
        });

        Net.handle(PlacePacket.class, packet -> {
            Vars.control.input.placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false);
        });

        Net.handle(BreakPacket.class, packet -> {
            Vars.control.input.breakBlockInternal(packet.x, packet.y, false);
        });

        Net.handle(StateSyncPacket.class, packet -> {
            //TODO replace with arraycopy()
            for(int i = 0; i < packet.items.length; i ++){
                Vars.control.items[i] = packet.items[i];
            }
            Vars.control.setWaveData(packet.enemies, packet.wave, packet.countdown);

            Gdx.app.postRunnable(() -> {
                Vars.ui.updateItems();
            });
        });

        Net.handle(EnemySpawnPacket.class, spawn -> {
            Gdx.app.postRunnable(() -> {
                try{
                    Enemy enemy = ClassReflection.newInstance(spawn.type);
                    enemy.set(spawn.x, spawn.y);
                    enemy.tier = spawn.tier;
                    enemy.lane = spawn.lane;
                    enemy.id = spawn.id;
                    enemy.add();

                    Effects.effect(Fx.spawn, enemy);
                }catch (ReflectionException e){
                    throw new RuntimeException(e);
                }
            });
        });

        Net.handle(EnemyDeathPacket.class, spawn -> {
            Gdx.app.postRunnable(() -> {
                Enemy enemy = Vars.control.enemyGroup.getByID(spawn.id);
                if (enemy != null) enemy.onDeath();
            });
        });

        Net.handle(PathPacket.class, packet -> {
            Tile[] tiles = new Tile[packet.path.length];
            for(int i = 0; i < tiles.length; i ++){
                int c = packet.path[i];
                tiles[i] = Vars.world.tile(c % Vars.world.width(), c / Vars.world.width());
            }
            Vars.control.spawnpoints.get(packet.index).pathTiles = tiles;
        });

        Net.handle(BulletPacket.class, packet -> {
            //TODO shoot effects for enemies, clientside as well as serverside
            BulletType type = (BulletType) BaseBulletType.getByID(packet.type);
            Entity owner = Vars.control.enemyGroup.getByID(packet.owner);
            Bullet bullet = new Bullet(type, owner, packet.x, packet.y, packet.angle).add();
        });

        Net.handle(BlockDestroyPacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position % Vars.world.width(), packet.position / Vars.world.width());
            if(tile.entity != null){
                tile.entity.onDeath(true);
            }
        });

        Net.handle(BlockUpdatePacket.class, packet -> {
            Tile tile = Vars.world.tile(packet.position % Vars.world.width(), packet.position / Vars.world.width());
            if(tile.entity != null){
                tile.entity.health = packet.health;
            }
        });

        Net.handle(BlockSyncPacket.class, packet -> {
            //TODO implementation, load data...
            DataInputStream stream = new DataInputStream(packet.stream);

            try{
                while(stream.available() > 0){
                    int pos = stream.readInt();

                    Tile tile = Vars.world.tile(pos % Vars.world.width(), pos / Vars.world.width());

                    byte times = stream.readByte();

                    for(int i = 0; i < times; i ++){
                        tile.entity.timer.getTimes()[i] = stream.readFloat();
                    }

                    tile.entity.read(stream);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        });
    }

    public void update(){
        if(!Net.client()) return;

        if(!GameState.is(State.menu) && Net.active()){
            sync();
        }else if(!connecting){
            Net.disconnect();
        }
    }

    public void handleShoot(Weapon weapon, float x, float y, float angle){
        ShootPacket packet = new ShootPacket();
        packet.weaponid = (byte)weapon.ordinal();
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
            Net.send(packet, SendMode.tcp); //TODO udp instead?
        }
    }
}
