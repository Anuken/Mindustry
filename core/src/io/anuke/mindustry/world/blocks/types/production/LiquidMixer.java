package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Timers;

public class LiquidMixer extends LiquidBlock{
    protected Liquid inputLiquid = Liquid.none;
    protected Liquid outputLiquid = Liquid.none;
    protected Item inputItem = null;
    protected float liquidPerItem = 50f;
    protected float powerUse = 0f;

    public LiquidMixer(String name) {
        super(name);
        hasInventory = true;
        hasPower = true;
        rotate = false;
        liquidRegion = name() + "-liquid";
        solid = true;
    }

    @Override
    public void update(Tile tile){
        if(tile.entity.liquid.amount > 0.001f){
            tryDumpLiquid(tile);
        }

        tile.entity.power.amount -= Math.min(Timers.delta() * powerUse, tile.entity.power.amount);
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        LiquidMixerEntity entity = tile.entity();
        entity.accumulator += amount;
        int items = (int)(entity.accumulator / liquidPerItem);
        entity.inventory.removeItem(inputItem, items);
        entity.accumulator %= liquidPerItem;
        entity.liquid.liquid = outputLiquid;
        entity.liquid.amount += amount;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return item == inputItem && tile.entity.inventory.getItem(item) < itemCapacity;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return liquid == inputLiquid && tile.entity.liquid.amount + amount <= liquidCapacity &&
                tile.entity.inventory.hasItem(inputItem, (int)((tile.<LiquidMixerEntity>entity().accumulator + amount)/amount)) &&
                tile.entity.power.amount >= powerUse;
    }

    @Override
    public TileEntity getEntity() {
        return new LiquidMixerEntity();
    }

    static class LiquidMixerEntity extends TileEntity {
        float accumulator;
    }
}
