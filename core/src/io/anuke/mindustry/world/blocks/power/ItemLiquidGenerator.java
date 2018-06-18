package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public abstract class ItemLiquidGenerator extends ItemGenerator {
    protected float minLiquidEfficiency = 0.2f;
    protected float powerPerLiquid = 0.13f;
    /**Maximum liquid used per frame.*/
    protected float maxLiquidGenerate = 0.4f;

    public ItemLiquidGenerator(String name) {
        super(name);
        hasLiquids = true;
        liquidCapacity = 10f;
    }

    @Override
    public void update(Tile tile){
        ItemGeneratorEntity entity = tile.entity();

        //liquid takes priority over solids
        if(entity.liquids.amount >= 0.001f){
            float powerPerLiquid = getLiquidEfficiency(entity.liquids.liquid)*this.powerPerLiquid;
            float used = Math.min(entity.liquids.amount, maxLiquidGenerate * Timers.delta());
            used = Math.min(used, (powerCapacity - entity.power.amount)/powerPerLiquid);

            entity.liquids.amount -= used;
            entity.power.amount += used * powerPerLiquid;

            if(used > 0.001f && Mathf.chance(0.05 * Timers.delta())){
                Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
            }
        }else {

            float maxPower = Math.min(powerCapacity - entity.power.amount, powerOutput * Timers.delta()) * entity.efficiency;
            float mfract = maxPower / (powerOutput);

            if (entity.generateTime > 0f) {
                entity.generateTime -= 1f / itemDuration * mfract;
                entity.power.amount += maxPower;
                entity.generateTime = Mathf.clamp(entity.generateTime);
            }

            if (entity.generateTime <= 0f && entity.items.totalItems() > 0) {
                Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
                for (int i = 0; i < entity.items.items.length; i++) {
                    if (entity.items.items[i] > 0) {
                        entity.items.items[i]--;
                        entity.efficiency = getItemEfficiency(Item.getByID(i));
                        break;
                    }
                }
                entity.generateTime = 1f;
            }
        }

        distributePower(tile);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        TileEntity entity = tile.entity();

        Draw.color(entity.liquids.liquid.color);
        Draw.alpha(entity.liquids.amount / liquidCapacity);
        drawLiquidCenter(tile);
        Draw.color();
    }

    public void drawLiquidCenter(Tile tile){
        Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return getLiquidEfficiency(liquid) >= minLiquidEfficiency && super.acceptLiquid(tile, source, liquid, amount);
    }

    protected abstract float getLiquidEfficiency(Liquid liquid);
}
