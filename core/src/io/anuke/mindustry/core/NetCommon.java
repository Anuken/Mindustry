package io.anuke.mindustry.core;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Recipes;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.modules.Module;

import static io.anuke.mindustry.Vars.*;

public class NetCommon extends Module {

    public NetCommon(){

        Net.handle(ShootPacket.class, (packet) -> {
            Player player = playerGroup.getByID(packet.playerid);

            Weapon weapon = (Weapon) Upgrade.getByID(packet.weaponid);
            weapon.shoot(player, packet.x, packet.y, packet.rotation);
        });

        Net.handle(PlacePacket.class, (packet) -> {
            if(headless)
                world.placeBlock(packet.x, packet.y, Block.getByID(packet.block), packet.rotation);
            else
                control.input().placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false);

            Recipe recipe = Recipes.getByResult(Block.getByID(packet.block));
            if (recipe != null) state.inventory.removeItems(recipe.requirements);
        });

        Net.handle(BreakPacket.class, (packet) -> {
            if(headless)
                world.removeBlock(world.tile(packet.x, packet.y));
            else
                control.input().breakBlockInternal(packet.x, packet.y, false);
        });

        Net.handle(ChatPacket.class, (packet) -> {
            ui.chatfrag.addMessage(packet.text, colorizeName(packet.id, packet.name));
        });

        Net.handle(WeaponSwitchPacket.class, (packet) -> {
            Player player = playerGroup.getByID(packet.playerid);

            if (player == null) return;

            player.weaponLeft = (Weapon) Upgrade.getByID(packet.left);
            player.weaponRight = (Weapon) Upgrade.getByID(packet.right);
        });

        Net.handle(BlockTapPacket.class, (packet) -> {
            Tile tile = world.tile(packet.position);
            tile.block().tapped(tile);
        });

        Net.handle(BlockConfigPacket.class, (packet) -> {
            Tile tile = world.tile(packet.position);
            if (tile != null) tile.block().configure(tile, packet.data);
        });

        Net.handle(PlayerDeathPacket.class, (packet) -> {
            Player player = playerGroup.getByID(packet.id);
            if(player == null) return;

            player.doRespawn();
        });
    }

    public void sendMessage(String message){
        ChatPacket packet = new ChatPacket();
        packet.name = null;
        packet.text = message;
        Net.send(packet, SendMode.tcp);
        if(!headless) ui.chatfrag.addMessage(message, null);
    }

    public String colorizeName(int id, String name){
        Player player = playerGroup.getByID(id);
        if(name == null || player == null) return null;
        return "[#" + player.color.toString().toUpperCase() + "]" + name;
    }
}
