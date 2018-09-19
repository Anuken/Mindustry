package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.meta.PowerType;

public class PowerDistributor extends PowerBlock{

    public PowerDistributor(String name){
        super(name);
        powerType = PowerType.producer;
    }

    @Override
    public void update(Tile tile){
        tile.entity.power.graph.update();
    }
}
