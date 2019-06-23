package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.mindustry.world.consumers.ConsumeLiquidBase;
import io.anuke.mindustry.world.consumers.ConsumeType;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.mindustry.world.meta.values.ItemFilterValue;

/**
 * Extracts a random list of items from an input item and an input liquid.
 */
public class Separator extends Block{
    protected ItemStack[] results;
    protected float craftTime;
    protected float spinnerRadius = 2.5f;
    protected float spinnerLength = 1f;
    protected float spinnerThickness = 1f;
    protected float spinnerSpeed = 2f;

    protected Color color = Color.valueOf("858585");
    protected int liquidRegion;

    public Separator(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;

        liquidRegion = reg("-liquid");
    }

    @Override
    public void setStats(){
        if(consumes.has(ConsumeType.liquid)){
            ConsumeLiquidBase cons = consumes.get(ConsumeType.liquid);
            cons.timePeriod = craftTime;
        }

        super.setStats();

        stats.add(BlockStat.output, new ItemFilterValue(item -> {
            for(ItemStack i : results){
                if(item == i.item) return true;
            }
            return false;
        }));

        stats.add(BlockStat.productionTime, craftTime / 60f, StatUnit.seconds);
    }

    @Override
    public boolean canProduce(Tile tile){
        return tile.entity.items.total() < itemCapacity;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        GenericCrafterEntity entity = tile.entity();

        Draw.color(tile.entity.liquids.current().color);
        Draw.alpha(tile.entity.liquids.total() / liquidCapacity);
        Draw.rect(reg(liquidRegion), tile.drawx(), tile.drawy());

        Draw.color(color);
        Lines.stroke(spinnerThickness);
        Lines.spikes(tile.drawx(), tile.drawy(), spinnerRadius, spinnerLength, 3, entity.totalProgress * spinnerSpeed);
        Draw.reset();
    }

    @Override
    public void update(Tile tile){
        GenericCrafterEntity entity = tile.entity();

        entity.totalProgress += entity.warmup * entity.delta();

        if(entity.cons.valid()){
            entity.progress += getProgressIncrease(entity, craftTime);
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.02f);
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.02f);
        }

        if(entity.progress >= 1f){
            entity.progress = 0f;
            int sum = 0;
            for(ItemStack stack : results) sum += stack.amount;

            int i = Mathf.random(sum);
            int count = 0;
            Item item = null;

            //TODO guaranteed desync since items are random
            for(ItemStack stack : results){
                if(i >= count && i < count + stack.amount){
                    item = stack.item;
                    break;
                }
                count += stack.amount;
            }

            entity.cons.trigger();

            if(item != null && entity.items.get(item) < itemCapacity){
                offloadNear(tile, item);
            }
        }

        if(entity.timer.get(timerDump, dumpTime)){
            tryDump(tile);
        }
    }

    @Override
    public TileEntity newEntity(){
        return new GenericCrafterEntity();
    }
}
