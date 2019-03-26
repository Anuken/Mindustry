package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;

public class PowerDistributor extends PowerBlock{

    public PowerDistributor(String name){
        super(name);
        consumesPower = false;
        outputsPower = true;
    }

    @Override
    public void update(Tile tile){
        tile.entity.power.graph.update();
    }
}
