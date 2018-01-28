package io.anuke.mindustry.core;

import com.badlogic.gdx.graphics.Color;
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
    public static final Color[] colorArray = {Color.ORANGE, Color.SCARLET, Color.LIME, Color.PURPLE,
            Color.GOLD, Color.PINK, Color.SKY, Color.GOLD, Color.VIOLET,
            Color.GREEN, Color.CORAL, Color.CYAN, Color.CHARTREUSE};

    public NetCommon(){

        Net.handleServer(ShootPacket.class, (id, packet) -> {
            Player player = playerGroup.getByID(packet.playerid);

            Weapon weapon = (Weapon) Upgrade.getByID(packet.weaponid);
            weapon.shoot(player, packet.x, packet.y, packet.rotation);
        });

        Net.handleServer(PlacePacket.class, (id, packet) -> {
            control.input().placeBlockInternal(packet.x, packet.y, Block.getByID(packet.block), packet.rotation, true, false);

            Recipe recipe = Recipes.getByResult(Block.getByID(packet.block));
            if (recipe != null) state.inventory.removeItems(recipe.requirements);
        });

        Net.handleServer(BreakPacket.class, (id, packet) -> {
            control.input().breakBlockInternal(packet.x, packet.y, false);
        });

        Net.handleServer(ChatPacket.class, (id, packet) -> {
            ui.chatfrag.addMessage(packet.text, colorizeName(packet.id, packet.name));
        });

        Net.handleServer(WeaponSwitchPacket.class, (id, packet) -> {
            Player player = playerGroup.getByID(packet.playerid);

            if (player == null) return;

            player.weaponLeft = (Weapon) Upgrade.getByID(packet.left);
            player.weaponRight = (Weapon) Upgrade.getByID(packet.right);
        });

        Net.handleServer(BlockTapPacket.class, (id, packet) -> {
            Tile tile = world.tile(packet.position);
            tile.block().tapped(tile);
        });

        Net.handleServer(BlockConfigPacket.class, (id, packet) -> {
            Tile tile = world.tile(packet.position);
            if (tile != null) tile.block().configure(tile, packet.data);
        });

        Net.handleServer(PlayerDeathPacket.class, (id, packet) -> {
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
        ui.chatfrag.addMessage(message, null);
    }

    public String colorizeName(int id, String name){
        return name == null ? null : "[#" + colorArray[id % colorArray.length].toString().toUpperCase() + "]" + name;
    }
}
