package mindustry.world.blocks.production;

import arc.math.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

public class LiquidConverter extends GenericCrafter{

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
        if(!consumes.has(ConsumeType.liquid) || !(consumes.get(ConsumeType.liquid) instanceof ConsumeLiquid)){
            throw new RuntimeException("LiquidsConverters must have a ConsumeLiquid. Note that filters are not supported.");
        }

        ConsumeLiquid cl = consumes.get(ConsumeType.liquid);
        cl.update(false);
        outputLiquid.amount = cl.amount;
        super.init();
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
            ConsumeLiquid cl = consumes.get(ConsumeType.liquid);

            if(cons.valid()){
                if(Mathf.chanceDelta(updateEffectChance)){
                    updateEffect.at(x + Mathf.range(size * 4f), y + Mathf.range(size * 4));
                }

                warmup = Mathf.lerpDelta(warmup, 1f, 0.02f);
                float use = Math.min(cl.amount * edelta(), liquidCapacity - liquids.get(outputLiquid.liquid));
                float ratio = outputLiquid.amount / cl.amount;

                liquids.remove(cl.liquid, Math.min(use, liquids.get(cl.liquid)));

                progress += use / cl.amount;
                liquids.add(outputLiquid.liquid, use * ratio);
                if(progress >= craftTime){
                    consume();
                    progress %= craftTime;
                }
            }else{
                //warmup is still 1 even if not consuming
                warmup = Mathf.lerp(warmup, cons.canConsume() ? 1f : 0f, 0.02f);
            }

            dumpLiquid(outputLiquid.liquid);
        }
    }
}
