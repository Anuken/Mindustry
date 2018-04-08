package io.anuke.mindustry.net;

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

    public static void handleFriendlyFireChange(boolean enabled){
        FriendlyFireChangePacket packet = new FriendlyFireChangePacket();
        packet.enabled = enabled;

        netCommon.sendMessage(enabled ? "[accent]Friendly fire enabled." : "[accent]Friendly fire disabled.");

        Net.send(packet, SendMode.tcp);
    }

    public static void handleGameOver(){
        Net.send(new GameOverPacket(), SendMode.tcp);
    }

    public static void handleUnitDeath(Unit entity){
        EntityDeathPacket packet = new EntityDeathPacket();
        packet.id = entity.id;
        packet.group = (byte)entity.getGroup().getID();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBlockDestroyed(TileEntity entity){
        BlockDestroyPacket packet = new BlockDestroyPacket();
        packet.position = entity.tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBlockDamaged(TileEntity entity){
        BlockUpdatePacket packet = new BlockUpdatePacket();
        packet.health = (int)entity.health;
        packet.position = entity.tile.packedPosition();
        Net.send(packet, SendMode.udp);
    }

    public static void handleBlockConfig(Tile tile, byte data){
        BlockConfigPacket packet = new BlockConfigPacket();
        packet.data = data;
        packet.position = tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBlockTap(Tile tile){
        BlockTapPacket packet = new BlockTapPacket();
        packet.position = tile.packedPosition();
        Net.send(packet, SendMode.tcp);
    }

    public static void handleWeaponSwitch(){
        WeaponSwitchPacket packet = new WeaponSwitchPacket();
        packet.left = Vars.player.weaponLeft.id;
        packet.right = Vars.player.weaponRight.id;
        packet.playerid = Vars.player.id;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleUpgrade(Weapon weapon){
        UpgradePacket packet = new UpgradePacket();
        packet.id = weapon.id;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleSendMessage(String message){
        ChatPacket packet = new ChatPacket();
        packet.text = message;
        packet.name = player.name;
        packet.id = player.id;
        Net.send(packet, SendMode.tcp);

        if(Net.server() && !headless){
            ui.chatfrag.addMessage(message, netCommon.colorizeName(player.id, player.name));
        }
    }

    public static void handleShoot(SyncEntity entity, float x, float y, float angle, short data){
        EntityShootPacket packet = new EntityShootPacket();
        packet.groupid = (byte)entity.getGroup().getID();
        packet.x = x;
        packet.y = y;
        packet.data = data;
        packet.rotation = angle;
        packet.entityid = entity.id;
        Net.send(packet, SendMode.udp);
    }

    public static void handlePlace(int x, int y, Block block, int rotation){
        PlacePacket packet = new PlacePacket();
        packet.x = (short)x;
        packet.y = (short)y;
        packet.rotation = (byte)rotation;
        packet.playerid = Vars.player.id;
        packet.block = block.id;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleBreak(int x, int y){
        BreakPacket packet = new BreakPacket();
        packet.x = (short)x;
        packet.y = (short)y;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleAdminSet(Player player, boolean admin){
        PlayerAdminPacket packet = new PlayerAdminPacket();
        packet.admin = admin;
        packet.id = player.id;
        player.isAdmin = admin;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleAdministerRequest(Player target, AdminAction action){
        AdministerRequestPacket packet = new AdministerRequestPacket();
        packet.id = target.id;
        packet.action = action;
        Net.send(packet, SendMode.tcp);
    }

    public static void handleTraceRequest(Player target){
        if(Net.client()) {
            handleAdministerRequest(target, AdminAction.trace);
        }else{
            ui.traces.show(target, netServer.admins.getTrace(Net.getConnection(target.clientid).address));
        }
    }
}
