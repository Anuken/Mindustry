package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquidFilter;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public abstract class ItemLiquidGenerator extends ItemGenerator {
    protected float minLiquidEfficiency = 0.2f;
    protected float powerPerLiquid = 0.13f;
    /**Maximum liquid used per frame.*/
    protected float maxLiquidGenerate = 0.4f;

    public ItemLiquidGenerator(String name) {
        super(name);
        hasLiquids = true;
        liquidCapacity = 10f;

        consumes.add(new ConsumeLiquidFilter(liquid -> getLiquidEfficiency(liquid) >= minLiquidEfficiency, 0.001f)).update(false);
    }

    @Override
    public void update(Tile tile){
        ItemGeneratorEntity entity = tile.entity();

        //liquid takes priority over solids
        if(entity.liquids.currentAmount() >= 0.001f){
            float powerPerLiquid = getLiquidEfficiency(entity.liquids.current())*this.powerPerLiquid;
            float used = Math.min(entity.liquids.currentAmount(), maxLiquidGenerate * Timers.delta());
            used = Math.min(used, (powerCapacity - entity.power.amount)/powerPerLiquid);

            entity.liquids.remove(entity.liquids.current(), used);
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

                if(Mathf.chance(Timers.delta() * 0.06 * Mathf.clamp(entity.explosiveness - 0.25f))){
                    entity.damage(Mathf.random(8f));
                    Effects.effect(explodeEffect, tile.worldx() + Mathf.range(size * tilesize/2f), tile.worldy() + Mathf.range(size * tilesize/2f));
                }
            }

            if (entity.generateTime <= 0f && entity.items.total() > 0) {
                Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
                Item item = entity.items.take();
                entity.efficiency = getItemEfficiency(item);
                entity.explosiveness = item.explosiveness;
                entity.generateTime = 1f;
            }
        }

        distributePower(tile);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        TileEntity entity = tile.entity();

        Draw.color(entity.liquids.current().color);
        Draw.alpha(entity.liquids.currentAmount() / liquidCapacity);
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
