package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeItemFilter;
import io.anuke.mindustry.world.consumers.ConsumeLiquidFilter;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.tilesize;

/**
 * Power generation block which can use items, liquids or both as input sources for power production.
 * Liquids will take priority over items.
 */
public class ItemLiquidGenerator extends PowerGenerator{

    protected float minItemEfficiency = 0.2f;
    /** The time in number of ticks during which a single item will produce power. */
    protected float itemDuration = 70f;

    protected float minLiquidEfficiency = 0.2f;
    /** Maximum liquid used per frame. */
    protected float maxLiquidGenerate = 0.4f;

    protected Effects.Effect generateEffect = BlockFx.generatespark;
    protected Effects.Effect explodeEffect = BlockFx.generatespark;
    protected Color heatColor = Color.valueOf("ff9b59");
    protected TextureRegion topRegion;

    public enum InputType{
        ItemsOnly,
        LiquidsOnly,
        LiquidsAndItems
    }

    public ItemLiquidGenerator(InputType inputType, String name){
        super(name);
        this.hasItems = inputType != InputType.LiquidsOnly;
        this.hasLiquids = inputType != InputType.ItemsOnly;

        if(hasItems){
            itemCapacity = 20;
            consumes.add(new ConsumeItemFilter(item -> getItemEfficiency(item) >= minItemEfficiency)).update(false).optional(true);
        }

        if(hasLiquids){
            liquidCapacity = 10f;
            consumes.add(new ConsumeLiquidFilter(liquid -> getLiquidEfficiency(liquid) >= minLiquidEfficiency, 0.001f, true)).update(false).optional(true);
        }
    }

    @Override
    public void load(){
        super.load();
        if(hasItems){
            topRegion = Draw.region(name + "-top");
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        if(hasItems){
            bars.replace(new BlockBar(BarType.inventory, true, tile -> (float) tile.entity.items.total() / itemCapacity));
        }
    }


    @Override
    public void update(Tile tile){
        ItemLiquidGeneratorEntity entity = tile.entity();

        // Note: Do not use this delta when calculating the amount of power or the power efficiency, but use it for resource consumption if necessary.
        //       Power amount is delta'd by PowerGraph class already.
        float calculationDelta = entity.delta();

        if(!entity.cons.valid()){
            entity.productionEfficiency = 0.0f;
            return;
        }

        Liquid liquid = null;
        for(Liquid other : content.liquids()){
            if(hasLiquids && entity.liquids.get(other) >= 0.001f && getLiquidEfficiency(other) >= minLiquidEfficiency){
                liquid = other;
                break;
            }
        }
        //liquid takes priority over solids
        if(hasLiquids && liquid != null && entity.liquids.get(liquid) >= 0.001f){
            float baseLiquidEfficiency = getLiquidEfficiency(liquid);
            float maximumPossible = maxLiquidGenerate * calculationDelta;
            float used = Math.min(entity.liquids.get(liquid) * calculationDelta, maximumPossible);

            entity.liquids.remove(liquid, used);

            // Note: 0.5 = 100%. PowerGraph will multiply this efficiency by two on its own.
            entity.productionEfficiency = Mathf.clamp(baseLiquidEfficiency * used / maximumPossible);

            if(used > 0.001f && Mathf.chance(0.05 * entity.delta())){
                Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
            }
        }else if(hasItems){
            // No liquids accepted or none supplied, try using items if accepted
            if(entity.generateTime <= 0f && entity.items.total() > 0){
                Effects.effect(generateEffect, tile.worldx() + Mathf.range(3f), tile.worldy() + Mathf.range(3f));
                Item item = entity.items.take();
                entity.productionEfficiency = getItemEfficiency(item);
                entity.explosiveness = item.explosiveness;
                entity.generateTime = 1f;
            }

            if(entity.generateTime > 0f){
                entity.generateTime -= Math.min(1f / itemDuration * entity.delta(), entity.generateTime);

                if(Mathf.chance(entity.delta() * 0.06 * Mathf.clamp(entity.explosiveness - 0.25f))){
                    //this block is run last so that in the event of a block destruction, no code relies on the block type
                    entity.damage(Mathf.random(8f));
                    Effects.effect(explodeEffect, tile.worldx() + Mathf.range(size * tilesize / 2f), tile.worldy() + Mathf.range(size * tilesize / 2f));
                }
            }else{
                entity.productionEfficiency = 0.0f;
            }
        }

        super.update(tile);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return hasItems && getItemEfficiency(item) >= minItemEfficiency && tile.entity.items.total() < itemCapacity;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return hasLiquids && getLiquidEfficiency(liquid) >= minLiquidEfficiency && tile.entity.liquids.get(liquid) < liquidCapacity;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        GeneratorEntity entity = tile.entity();

        if(hasItems){
            if(entity.generateTime > 0){
                Draw.color(heatColor);
                float alpha = (entity.items.total() > 0 ? 1f : Mathf.clamp(entity.generateTime));
                alpha = alpha * 0.7f + Mathf.absin(Timers.time(), 12f, 0.3f) * alpha;
                Draw.alpha(alpha);
                Draw.rect(topRegion, tile.drawx(), tile.drawy());
                Draw.reset();
            }
        }

        if(hasLiquids){
            Draw.color(entity.liquids.current().color);
            Draw.alpha(entity.liquids.currentAmount() / liquidCapacity);
            drawLiquidCenter(tile);
            Draw.color();
        }
    }

    public void drawLiquidCenter(Tile tile){
        Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
    }

    protected float getItemEfficiency(Item item){
        return 0.0f;
    }

    protected float getLiquidEfficiency(Liquid liquid){
        return 0.0f;
    }

    @Override
    public TileEntity newEntity(){
        return new ItemLiquidGeneratorEntity();
    }

    public static class ItemLiquidGeneratorEntity extends GeneratorEntity{
        public float explosiveness;
    }
}
