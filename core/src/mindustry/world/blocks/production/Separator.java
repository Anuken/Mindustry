package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

/**
 * Extracts a random list of items from an input item and an input liquid.
 */
public class Separator extends Block{
    public @NonNull ItemStack[] results;
    public float craftTime;

    public int liquidRegion, spinnerRegion;
    public float spinnerSpeed = 3f;

    public Separator(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;

        liquidRegion = reg("-liquid");
        spinnerRegion = reg("-spinner");
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
    public boolean shouldConsume(){
        return tile.items.total() < itemCapacity;
    }

    @Override
    public void draw(){
        super.draw();

        Draw.color(tile.liquids.current().color);
        Draw.alpha(tile.liquids.total() / liquidCapacity);
        Draw.rect(reg(liquidRegion), x, y);

        Draw.reset();
        if(Core.atlas.isFound(reg(spinnerRegion))){
            Draw.rect(reg(spinnerRegion), x, y, totalProgress * spinnerSpeed);
        }
    }

    @Override
    public void updateTile(){
        totalProgress += warmup * delta();

        if(consValid()){
            progress += getProgressIncrease(entity, craftTime);
            warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);
        }else{
            warmup = Mathf.lerpDelta(warmup, 0f, 0.02f);
        }

        if(progress >= 1f){
            progress = 0f;
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

            consume();

            if(item != null && items.get(item) < itemCapacity){
                useContent(tile, item);
                offloadNear(tile, item);
            }
        }

        if(timer(timerDump, dumpTime)){
            tryDump(tile);
        }
    }
}
