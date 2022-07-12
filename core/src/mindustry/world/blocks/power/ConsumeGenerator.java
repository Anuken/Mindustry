package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

/** A generator that just takes in certain items or liquids. */
public class ConsumeGenerator extends PowerGenerator{
    /** The time in number of ticks during which a single item will produce power. */
    public float itemDuration = 120f;

    public float warmupSpeed = 0.05f;
    public float effectChance = 0.01f;
    public Effect generateEffect = Fx.none, consumeEffect = Fx.none;
    public float generateEffectRange = 3f;

    public @Nullable LiquidStack outputLiquid;

    public @Nullable ConsumeItemFilter filterItem;
    public @Nullable ConsumeLiquidFilter filterLiquid;

    public ConsumeGenerator(String name){
        super(name);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(outputLiquid != null){
            addLiquidBar(outputLiquid.liquid);
        }
    }

    @Override
    public void init(){
        filterItem = findConsumer(c -> c instanceof ConsumeItemFilter);
        filterLiquid = findConsumer(c -> c instanceof ConsumeLiquidFilter);

        if(outputLiquid != null){
            outputsLiquid = true;
            hasLiquids = true;
        }

        //TODO hardcoded
        emitLight = true;
        lightRadius = 65f * size;
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(Stat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }

        if(outputLiquid != null){
            stats.add(Stat.output, StatValues.liquid(outputLiquid.liquid, outputLiquid.amount * 60f, true));
        }
    }

    public class ConsumeGeneratorBuild extends GeneratorBuild{
        public float warmup, totalTime, efficiencyMultiplier = 1f;

        @Override
        public void updateEfficiencyMultiplier(){
            if(filterItem != null){
                float m = filterItem.efficiencyMultiplier(this);
                if(m > 0) efficiencyMultiplier = m;
            }else if(filterLiquid != null){
                float m = filterLiquid.efficiencyMultiplier(this);
                if(m > 0) efficiencyMultiplier = m;
            }
        }

        @Override
        public void updateTile(){
            boolean valid = efficiency > 0;

            warmup = Mathf.lerpDelta(warmup, valid ? 1f : 0f, warmupSpeed);

            productionEfficiency = efficiency * efficiencyMultiplier;
            totalTime += warmup * Time.delta;

            //randomly produce the effect
            if(valid && Mathf.chanceDelta(effectChance)){
                generateEffect.at(x + Mathf.range(generateEffectRange), y + Mathf.range(generateEffectRange));
            }

            //take in items periodically
            if(hasItems && valid && generateTime <= 0f){
                consume();
                consumeEffect.at(x + Mathf.range(generateEffectRange), y + Mathf.range(generateEffectRange));
                generateTime = 1f;
            }

            if(outputLiquid != null){
                float added = Math.min(productionEfficiency * delta() * outputLiquid.amount, liquidCapacity - liquids.get(outputLiquid.liquid));
                liquids.add(outputLiquid.liquid, added);
                dumpLiquid(outputLiquid.liquid);
            }

            //generation time always goes down, but only at the end so consumeTriggerValid doesn't assume fake items
            generateTime -= delta() / itemDuration;
        }

        @Override
        public boolean consumeTriggerValid(){
            return generateTime > 0;
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalTime;
        }

        @Override
        public void drawLight(){
            //???
            drawer.drawLight(this);
            //TODO hard coded
            Drawf.light(x, y, (60f + Mathf.absin(10f, 5f)) * size, Color.orange, 0.5f * warmup);
        }
    }
}
