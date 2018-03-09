package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

/**A drill that uses liquid as fuel.*/
public class LiquidDrill extends Drill {
    protected Liquid inputLiquid = Liquid.water;
    protected float inputLiquidAmount = 0.1f; //per frame

    public LiquidDrill(String name){
        super(name);

        hasLiquids = true;
    }

    @Override
    public void update(Tile tile){
        float consume = Math.min(liquidCapacity, inputLiquidAmount * Timers.delta());

        if(tile.entity.liquid.amount >= consume){
            tile.entity.liquid.amount -= consume;
            super.update(tile);
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
    }
}
