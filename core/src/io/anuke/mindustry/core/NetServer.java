package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.io.NetworkIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.modules.Module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

public class NetServer extends Module{
    IntMap<Player> connections = new IntMap<>();
    float serverSyncTime = 4, itemSyncTime = 10, blockSyncTime = 120;

    public NetServer(){

        Net.handleServer(Connect.class, packet -> {
            UCore.log("Sending world data to client (ID="+packet.id+"/"+packet.addressTCP+")");

            WorldData data = new WorldData();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            NetworkIO.write(stream);

            UCore.log("Packed " + stream.size() + " uncompressed bytes of data.");

            //TODO compress and uncompress when sending
            data.stream = new ByteArrayInputStream(stream.toByteArray());

            Net.sendStream(packet.id, data);

            Gdx.app.postRunnable(() -> {
                Vars.ui.showInfo("$text.server.connected");

                EntityDataPacket dp = new EntityDataPacket();

                Player player = new Player();
                player.clientid = packet.id;
                player.set(Vars.player.x, Vars.player.y);
                player.placerot = Vars.player.placerot;
                player.add();
                connections.put(packet.id, player);

                dp.playerid = player.id;
                dp.players = Vars.control.playerGroup.all().toArray(Player.class);

                UCore.log("Sending entities: " + Arrays.toString(dp.players));

                //TODO send pathfind positions
                //TODO new denser format
                //TODO save enemy nodes

                Net.sendTo(packet.id, dp, SendMode.tcp);
            });
        });

        Net.handleServer(Disconnect.class, packet -> {
            Gdx.app.postRunnable(() -> Vars.ui.showInfo("$text.server.disconnected"));
        });

        Net.handleServer(PositionPacket.class, pos -> {
            Player player = connections.get(Net.getLastConnection());
            player.getInterpolator().type.read(player, pos.data);
        });

        Net.handleServer(ShootPacket.class, packet -> {
            Player player = connections.get(Net.getLastConnection());

            Weapon weapon = Weapon.values()[packet.weaponid];
            weapon.shoot(player, packet.x, packet.y, packet.rotation);
            packet.playerid = player.id;

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.udp);
        });

        Net.handleServer(PlacePacket.class, packet -> {
            Vars.control.input.placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false);
            packet.playerid = connections.get(Net.getLastConnection()).id;

            Recipe recipe = Recipe.getByResult(Block.getByID(packet.block));
            if(recipe != null){
                for(ItemStack stack : recipe.requirements){
                    Vars.control.removeItem(stack);
                }
            }

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.tcp);
        });

        Net.handleServer(BreakPacket.class, packet -> {
            Vars.control.input.breakBlockInternal(packet.x, packet.y, false);
            packet.playerid = connections.get(Net.getLastConnection()).id;

            Net.sendExcept(Net.getLastConnection(), packet, SendMode.tcp);
        });
    }

    //TODO decide whether to use effects
    public void sendEffect(Effect effect, Color color, float x, float y, float rotation){
        EffectPacket packet = new EffectPacket();
        packet.id = effect.id;
        packet.color = Color.rgba8888(color);
        packet.x = x;
        packet.y = y;
        packet.rotation = rotation;
        Net.send(packet, SendMode.udp);
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

    public void handlePathFound(int index, Tile[] tiles){
        PathPacket packet = new PathPacket();
        int[] out = new int[tiles.length];

        for(int p = 0; p < out.length; p ++){
            out[p] = tiles[p].packedPosition();
        }

        packet.path = out;
        packet.index = (byte)index;
        Net.send(packet, SendMode.tcp);
    }

    public void handleEnemySpawn(Enemy enemy){
        EnemySpawnPacket packet = new EnemySpawnPacket();
        packet.type = enemy.getClass();
        packet.lane = (byte)enemy.lane;
        packet.tier = (byte)enemy.tier;
        packet.x = enemy.x;
        packet.y = enemy.y;
        packet.id = enemy.id;
        Net.send(packet, SendMode.tcp);
    }

    public void handleEnemyDeath(Enemy enemy){
        EnemyDeathPacket packet = new EnemyDeathPacket();
        packet.id = enemy.id;
        Net.send(packet, SendMode.tcp);
    }

    public void update(){
        if(!Net.server()) return;

        if(!GameState.is(State.menu) && Net.active()){
            sync();
        }else{
            Net.closeServer();
        }
    }

    void sync(){

        if(Timers.get("serverSync", serverSyncTime)){
            SyncPacket packet = new SyncPacket();
            int amount = Vars.control.playerGroup.amount() + Vars.control.enemyGroup.amount();
            packet.ids = new int[amount];
            packet.data = new float[amount][0];

            int index = 0;

            for(Player player : Vars.control.playerGroup.all()){
                float[] out = player.getInterpolator().type.write(player);
                packet.data[index] = out;
                packet.ids[index] = player.id;

                index ++;
            }

            packet.enemyStart = index;

            for(Enemy enemy : Vars.control.enemyGroup.all()){
                float[] out = enemy.getInterpolator().type.write(enemy);
                packet.data[index] = out;
                packet.ids[index] = enemy.id;

                index ++;
            }

            Net.send(packet, SendMode.udp);
        }

        if(Timers.get("serverItemSync", itemSyncTime)){
            StateSyncPacket packet = new StateSyncPacket();
            packet.items = Vars.control.items;
            packet.countdown = Vars.control.getWaveCountdown();
            packet.enemies = Vars.control.getEnemiesRemaining();
            packet.wave = Vars.control.getWave();

            Net.send(packet, SendMode.udp);
        }

        if(Timers.get("serverBlockSync", blockSyncTime)){
            BlockSyncPacket packet = new BlockSyncPacket();

            //TODO
        }
    }

    public void sendBlockSync(int client){
        BlockSyncPacket packet = new BlockSyncPacket();

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(bs);

        //TODO

        packet.stream = new ByteArrayInputStream(bs.toByteArray());
    }
}
