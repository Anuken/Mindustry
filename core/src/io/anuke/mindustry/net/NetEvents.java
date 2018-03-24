package io.anuke.mindustry.net;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.Entity;

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

    public static void handleBullet(BulletType type, Entity owner, float x, float y, float angle, short damage){
        BulletPacket packet = new BulletPacket();
        packet.x = x;
        packet.y = y;
        packet.angle = angle;
        packet.damage = damage;
        packet.owner = owner.id;
        packet.type = type.id;
        Net.send(packet, SendMode.udp);
    }

    public static void handleEnemyDeath(Enemy enemy){
        EnemyDeathPacket packet = new EnemyDeathPacket();
        packet.id = enemy.id;
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

    public static void handlePlayerDeath(){
        PlayerDeathPacket packet = new PlayerDeathPacket();
        packet.id = Vars.player.id;
        Net.send(packet, SendMode.tcp);
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

    public static void handleShoot(Weapon weapon, float x, float y, float angle){
        ShootPacket packet = new ShootPacket();
        packet.weaponid = weapon.id;
        packet.x = x;
        packet.y = y;
        packet.rotation = angle;
        packet.playerid = Vars.player.id;
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

    public static void handleTransfer(Tile tile, byte rotation, Item item){
        ItemTransferPacket packet = new ItemTransferPacket();
        packet.position = tile.packedPosition();
        packet.rotation = rotation;
        packet.itemid = (byte)item.id;
        Net.send(packet, SendMode.udp);
    }

    public static void handleItemSet(Tile tile, Item item, byte amount){
        ItemSetPacket packet = new ItemSetPacket();
        packet.position = tile.packedPosition();
        packet.itemid = (byte)item.id;
        packet.amount = amount;
        Net.send(packet, SendMode.udp);
    }

    public static void handleOffload(Tile tile, Item item){
        ItemOffloadPacket packet = new ItemOffloadPacket();
        packet.position = tile.packedPosition();
        packet.itemid = (byte)item.id;
        Net.send(packet, SendMode.udp);
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
