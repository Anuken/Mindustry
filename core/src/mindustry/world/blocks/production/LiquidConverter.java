package mindustry.world.blocks.production;

import arc.math.*;
import arc.util.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

public class LiquidConverter extends GenericCrafter{
    protected @Nullable ConsumeLiquid consumer;

    public LiquidConverter(String name){
        super(name);
        hasLiquids = true;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void init(){
        super.init();

        consumer = findConsumer(b -> b instanceof ConsumeLiquid);
        if(consumer == null) throw new RuntimeException("LiquidConverters must have a ConsumeLiquid.");
        consumer.update = false;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);
        stats.add(Stat.output, outputLiquid.liquid, outputLiquid.amount * 60f, true);
    }

    public class LiquidConverterBuild extends GenericCrafterBuild{
        @Override
        public void drawLight(){
            if(hasLiquids && drawLiquidLight && outputLiquid.liquid.lightColor.a > 0.001f){
                drawLiquidLight(outputLiquid.liquid, liquids.get(outputLiquid.liquid));
            }
        }

        @Override
        public void updateTile(){
            if(consValid()){
                if(Mathf.chanceDelta(updateEffectChance)){
                    updateEffect.at(x + Mathf.range(size * 4f), y + Mathf.range(size * 4));
                }

                warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);
                float use = Math.min(consumer.amount * edelta(), liquidCapacity - liquids.get(outputLiquid.liquid));
                float ratio = outputLiquid.amount / consumer.amount;

                liquids.remove(consumer.liquid, Math.min(use, liquids.get(consumer.liquid)));

                progress += use / consumer.amount;
                liquids.add(outputLiquid.liquid, use * ratio);
                if(progress >= craftTime){
                    consume();
                    progress %= craftTime;
                }
            }else{
                //warmup is still 1 even if not consuming
                warmup = Mathf.lerp(warmup, canConsume() ? 1f : 0f, 0.02f);
            }

            dumpLiquid(outputLiquid.liquid);
        }
    }
}
