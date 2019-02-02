package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeItem;
import io.anuke.mindustry.world.consumers.ConsumeLiquid;

public class DifferentialGenerator extends TurbineGenerator{

    public DifferentialGenerator(String name){
        super(name);

        consumes.require(ConsumeItem.class);
        consumes.require(ConsumeLiquid.class);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return hasItems && consumes.item() == item && tile.entity.items.total() < itemCapacity;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return hasLiquids && consumes.liquid() == liquid && tile.entity.liquids.get(liquid) < liquidCapacity;
    }
}
