package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;

public class NetEvents {

    public static void friendlyFireChange(boolean enabled){
        state.friendlyFire = enabled;

        if(Net.server()) netCommon.sendMessage(enabled ? "[accent]Friendly fire enabled." : "[accent]Friendly fire disabled.");
    }

    public static void handleGameOver(){
        Net.send(Pools.obtain(GameOverPacket.class), SendMode.tcp);
    }

    public static void handleUnitDeath(Unit entity){
        EntityDeathPacket packet = Pools.obtain(EntityDeathPacket.class);
        packet.id = entity.id;
        packet.group = (byte)entity.getGroup().getID();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBlockDestroyed(TileEntity entity){
        BlockDestroyPacket packet = Pools.obtain(BlockDestroyPacket.class);
        packet.position = entity.tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBlockDamaged(TileEntity entity){
        BlockUpdatePacket packet = Pools.obtain(BlockUpdatePacket.class);
        packet.health = (int)entity.health;
        packet.position = entity.tile.packedPosition();
        Net.send(packet, SendMode.udp);
    }

    public static void handleBlockConfig(Tile tile, byte data){
        BlockConfigPacket packet = Pools.obtain(BlockConfigPacket.class);
        packet.data = data;
        packet.position = tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBlockTap(Tile tile){
        BlockTapPacket packet = Pools.obtain(BlockTapPacket.class);
        packet.position = tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleWeaponSwitch(){
        WeaponSwitchPacket packet = Pools.obtain(WeaponSwitchPacket.class);
        packet.weapon = Vars.player.weapon.id;
        packet.playerid = Vars.player.id;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleUpgrade(Weapon weapon){
        UpgradePacket packet = Pools.obtain(UpgradePacket.class);
        packet.id = weapon.id;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleSendMessage(String message){
        ChatPacket packet = Pools.obtain(ChatPacket.class);
        packet.text = message;
        packet.name = player.name;
        packet.id = player.id;
        Net.send(packet, SendMode.tcp);

        if(Net.server() && !headless){
            ui.chatfrag.addMessage(message, netCommon.colorizeName(player.id, player.name));
        }
    }

    public static void handleShoot(SyncEntity entity, float x, float y, float angle, short data){
        EntityShootPacket packet = Pools.obtain(EntityShootPacket.class);
        packet.groupid = (byte)entity.getGroup().getID();
        packet.x = x;
        packet.y = y;
        packet.data = data;
        packet.rotation = angle;
        packet.entityid = entity.id;
        Net.send(packet, SendMode.udp);
    }

    public static void handlePlace(int x, int y, Block block, int rotation){
        PlacePacket packet = Pools.obtain(PlacePacket.class);
        packet.x = (short)x;
        packet.y = (short)y;
        packet.rotation = (byte)rotation;
        packet.playerid = Vars.player.id;
        packet.block = block.id;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBreak(int x, int y){
        BreakPacket packet = Pools.obtain(BreakPacket.class);
        packet.x = (short)x;
        packet.y = (short)y;
        Net.send(packet, SendMode.tcp);
    }

    public static void adminSet(Player player, boolean admin){
        player.isAdmin = admin;

        if(Net.client()){
            ui.listfrag.rebuild();
        }
    }

    public static void handleAdministerRequest(Player target, AdminAction action){
        AdministerRequestPacket packet = Pools.obtain(AdministerRequestPacket.class);
        packet.id = target.id;
        packet.action = action;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleTraceRequest(Player target){
        if(Net.client()) {
            handleAdministerRequest(target, AdminAction.trace);
        }else{
            ui.traces.show(target, netServer.admins.getTraceByID(target.uuid));
        }
    }
}
