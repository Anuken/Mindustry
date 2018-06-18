package io.anuke.mindustry.world.blocks.production;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.ucore.core.Timers;

public class LiquidMixer extends LiquidBlock{
    protected Liquid inputLiquid = Liquids.none;
    protected Liquid outputLiquid = Liquids.none;
    protected Item inputItem = null;
    protected float liquidPerItem = 50f;
    protected float powerUse = 0f;

    public LiquidMixer(String name) {
        super(name);
        hasItems = true;
        hasPower = true;
        rotate = false;
        liquidRegion = name() + "-liquid";
        solid = true;
    }

    @Override
    public void update(Tile tile){
        float used = Math.min(Timers.delta() * powerUse, tile.entity.power.amount);

        tryDumpLiquid(tile);

        if(tile.entity.power.amount > used) tile.entity.power.amount -= used;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return item == inputItem && tile.entity.items.getItem(item) < itemCapacity;
    }

    @Override
    public float handleAuxLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        LiquidMixerEntity entity = tile.entity();

        if(liquid == inputLiquid && tile.entity.items.hasItem(inputItem, (int)((entity.accumulator + amount)/amount)) &&
                tile.entity.power.amount >= powerUse){

            amount = Math.min(liquidCapacity - tile.entity.liquids.amount, amount);

            entity.accumulator += amount;
            int items = (int)(entity.accumulator / liquidPerItem);
            entity.items.removeItem(inputItem, items);
            entity.accumulator %= liquidPerItem;
            entity.liquids.liquid = outputLiquid;
            entity.liquids.amount += amount;
            return amount;
        }else{
            return 0;
        }
    }

    @Override
    public TileEntity getEntity() {
        return new LiquidMixerEntity();
    }

    static class LiquidMixerEntity extends TileEntity {
        float accumulator;
    }
}
