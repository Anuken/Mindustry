package mindustry.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

/**
 * Extracts a random list of items from an input item and an input liquid.
 */
public class Separator extends Block{
    protected @Nullable ConsumeItems consItems;

    public ItemStack[] results;
    public float craftTime;

    public DrawBlock drawer = new DrawDefault();

    public Separator(String name){
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;
        sync = true;
    }

    @Override
    public void setStats(){
        stats.timePeriod = craftTime;
        super.setStats();

        int[] sum = {0};
        for(var r : results) sum[0] += r.amount;

        stats.add(Stat.output, table -> {
            for(ItemStack stack : results){
                table.add(StatValues.displayItemPercent(stack.item, (int)((float)stack.amount / sum[0] * 100), true)).padRight(5);
            }
        });
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
    }

    @Override
    public void init(){
        super.init();
        consItems = findConsumer(c -> c instanceof ConsumeItems);
    }

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    public class SeparatorBuild extends Building{
        public float progress;
        public float totalProgress;
        public float warmup;
        public int seed;

        @Override
        public void created(){
            seed = Mathf.randomSeed(tile.pos(), 0, Integer.MAX_VALUE - 1);
        }

        @Override
        public boolean shouldAmbientSound(){
            return efficiency > 0;
        }

        @Override
        public boolean shouldConsume(){
            int total = items.total();
            //very inefficient way of allowing separators to ignore input buffer storage
            if(consItems != null){
                for(ItemStack stack : consItems.items){
                    total -= items.get(stack.item);
                }
            }
            return total < itemCapacity && enabled;
        }

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float progress(){
            return progress;
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }

        @Override
        public void updateTile(){
            totalProgress += warmup * delta();

            if(efficiency > 0){
                progress += getProgressIncrease(craftTime);
                warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.02f);
            }

            if(progress >= 1f){
                progress %= 1f;
                int sum = 0;
                for(ItemStack stack : results) sum += stack.amount;

                int i = Mathf.randomSeed(seed++, 0, sum - 1);
                int count = 0;
                Item item = null;

                //guaranteed desync since items are random - won't be fixed and probably isn't too important
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

            if(timer(timerDump, dumpTime / timeScale)){
                dump();
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return progress;
            return super.sense(sensor);
        }

        @Override
        public boolean canDump(Building to, Item item){
            return !consumesItem(item);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(warmup);
            write.i(seed);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            warmup = read.f();
            if(revision == 1) seed = read.i();
        }
    }
}
