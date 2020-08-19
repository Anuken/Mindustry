package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

/**
 * Extracts a random list of items from an input item and an input liquid.
 */
public class Separator extends Block{
    public @NonNull ItemStack[] results;
    public float craftTime;

    public @Load("@-liquid") TextureRegion liquidRegion;
    public @Load("@-spinner") TextureRegion spinnerRegion;
    public float spinnerSpeed = 3f;

    public Separator(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;
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

    public class SeparatorBuild extends Building{
        public float progress;
        public float totalProgress;
        public float warmup;

        @Override
        public boolean shouldIdleSound(){
            return cons.valid();
        }

        @Override
        public boolean shouldConsume(){
            int total = items.total();
            //very inefficient way of allowing separators to ignore input buffer storage
            if(consumes.has(ConsumeType.item) && consumes.get(ConsumeType.item) instanceof ConsumeItems){
                ConsumeItems c = consumes.get(ConsumeType.item);
                for(ItemStack stack : c.items){
                    total -= items.get(stack.item);
                }
            }
            return total < itemCapacity && enabled;
        }

        @Override
        public void draw(){
            super.draw();

            Draw.color(liquids.current().color);
            Draw.alpha(liquids.total() / liquidCapacity);
            Draw.rect(liquidRegion, x, y);

            Draw.reset();
            if(Core.atlas.isFound(spinnerRegion)){
                Draw.rect(spinnerRegion, x, y, totalProgress * spinnerSpeed);
            }
        }

        @Override
        public void updateTile(){
            totalProgress += warmup * delta();

            if(consValid()){
                progress += getProgressIncrease(craftTime);
                warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.02f);
            }

            if(progress >= 1f){
                progress %= 1f;
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
                    offload(item);
                }
            }

            if(timer(timerDump, dumpTime)){
                dump();
            }
        }

        @Override
        public boolean canDump(Building to, Item item){
            return !consumes.itemFilters.get(item.id);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            warmup = read.f();
        }
    }
}
