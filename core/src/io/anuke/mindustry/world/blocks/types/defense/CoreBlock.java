package io.anuke.mindustry.world.blocks.types.defense;

import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.debug;
import static io.anuke.mindustry.Vars.state;

public class CoreBlock extends Block {

    public CoreBlock(String name) {
        super(name);

        health = 800;
        solid = true;
        destructible = true;
        width = 3;
        height = 3;
    }

    @Override
    public int handleDamage(Tile tile, int amount){
        return debug ? 0 : amount;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(Net.server() || !Net.active()) state.inventory.addItem(item, 1);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return true;
    }
}
