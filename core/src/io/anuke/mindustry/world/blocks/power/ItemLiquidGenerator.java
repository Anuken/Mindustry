package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquidFilter;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.tilesize;

public abstract class ItemLiquidGenerator extends ItemGenerator{
    protected float minLiquidEfficiency = 0.2f;
    protected float liquidPowerMultiplier = 1.3f; // A liquid with 100% flammability will be 30% more efficient than an item with 100% flammability.
    /**Maximum liquid used per frame.*/
    protected float maxLiquidGenerate = 0.4f;

    public ItemLiquidGenerator(String name){
        super(name);
        hasLiquids = true;
        liquidCapacity = 10f;

        consumes.add(new ConsumeLiquidFilter(liquid -> getLiquidEfficiency(liquid) >= minLiquidEfficiency, 0.001f, true)).update(false).optional(true);
    }

    @Override
    public void init(){
        super.init();
    }

    @Override
    public void update(Tile tile){
        ItemGeneratorEntity entity = tile.entity();

        entity.power.graph.update();

        Liquid liquid = null;
        for(Liquid other : content.liquids()){
            if(entity.liquids.get(other) >= 0.001f && getLiquidEfficiency(other) >= minLiquidEfficiency){
                liquid = other;
                break;
            }
        }

        // Note: Do not use this delta when calculating the amount of power or the power efficiency, but use it for resource consumption if necessary.
        //       Power amount is delta'd by PowerGraph class already.
        float calculationDelta = entity.delta();

        if(!entity.cons.valid()){
            entity.productionEfficiency = 0.0f;
            return;
        }
        //liquid takes priority over solids
        if(liquid != null && entity.liquids.get(liquid) >= 0.001f){
            float baseLiquidEfficiency = getLiquidEfficiency(liquid) * this.liquidPowerMultiplier;
            float maximumPossible = maxLiquidGenerate * calculationDelta;
            float used = Math.min(entity.liquids.get(liquid) * calculationDelta, maximumPossible);

            entity.liquids.remove(liquid, used);

            // Note: 1 Item with 100% Flammability = 100% efficiency. This means 100% is not max but rather a reference point for this generator.
            entity.productionEfficiency = baseLiquidEfficiency * used / maximumPossible;

            if(used > 0.001f && Mathf.chance(0.05 * entity.delta())){
                Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
            }
        }else{

            if(entity.generateTime <= 0f && entity.items.total() > 0){
                Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
                Item item = entity.items.take();
                entity.productionEfficiency = getItemEfficiency(item);
                entity.explosiveness = item.explosiveness;
                entity.generateTime = 1f;
            }

            if(entity.generateTime > 0f){
                entity.generateTime -= 1f / itemDuration * entity.delta();
                entity.generateTime = Mathf.clamp(entity.generateTime);

                if(Mathf.chance(entity.delta() * 0.06 * Mathf.clamp(entity.explosiveness - 0.25f))){
                    entity.damage(Mathf.random(8f));
                    Effects.effect(explodeEffect, tile.worldx() + Mathf.range(size * tilesize / 2f), tile.worldy() + Mathf.range(size * tilesize / 2f));
                }
            }else{
                entity.productionEfficiency = 0.0f;
            }
        }
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

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return getLiquidEfficiency(liquid) >= minLiquidEfficiency && tile.entity.liquids.get(liquid) < liquidCapacity;
    }

    public void drawLiquidCenter(Tile tile){
        Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
    }

    protected abstract float getLiquidEfficiency(Liquid liquid);
}
