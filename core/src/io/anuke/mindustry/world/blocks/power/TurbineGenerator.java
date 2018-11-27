package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquid;

public class TurbineGenerator extends BurnerGenerator{

    public TurbineGenerator(String name){
        super(name);
        singleLiquid = false;

        consumes.require(ConsumeLiquid.class);
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return super.acceptLiquid(tile, source, liquid, amount) || liquid == consumes.liquid() && tile.entity.liquids.get(consumes.liquid()) < liquidCapacity;
    }
}
