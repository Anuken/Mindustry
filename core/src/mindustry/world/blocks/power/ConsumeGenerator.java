package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
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
    public float baseLightRadius = 65f;

    public @Nullable LiquidStack outputLiquid;
    /** If true, this block explodes when outputLiquid exceeds capacity. */
    public boolean explodeOnFull = false;

    public @Nullable ConsumeItemFilter filterItem;
    public @Nullable ConsumeLiquidFilter filterLiquid;
    /** Multiplies the itemDuration for a given item. */
    public ObjectFloatMap<Item> itemDurationMultipliers = new ObjectFloatMap<>();

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

        //pass along the duration multipliers to the consumer, so it can display them properly
        if(filterItem instanceof ConsumeItemEfficiency eff){
            eff.itemDurationMultipliers = itemDurationMultipliers;
        }

        if(outputLiquid != null){
            outputsLiquid = true;
            hasLiquids = true;
        }

        if(explodeOnFull && outputLiquid != null && explosionPuddleLiquid == null){
            explosionPuddleLiquid = outputLiquid.liquid;
        }

        emitLight = true;
        lightRadius = baseLightRadius * size;
        super.init();
    }

    @Override
    public void setStats(){
        stats.timePeriod = itemDuration;
        super.setStats();

        if(hasItems){
            stats.add(Stat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }

        if(outputLiquid != null){
            stats.add(Stat.output, StatValues.liquid(outputLiquid.liquid, outputLiquid.amount * 60f, true));
        }
    }

    public class ConsumeGeneratorBuild extends GeneratorBuild{
        public float warmup, totalTime, efficiencyMultiplier = 1f, itemDurationMultiplier = 1;

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

            //make sure the multiplier doesn't change when there is nothing to consume while it's still running
            if(filterItem != null && valid && itemDurationMultipliers.size > 0 && filterItem.getConsumed(this) != null){
                itemDurationMultiplier = itemDurationMultipliers.get(filterItem.getConsumed(this), 1);
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

                if(explodeOnFull && liquids.get(outputLiquid.liquid) >= liquidCapacity - 0.01f){
                    kill();
                    Events.fire(new GeneratorPressureExplodeEvent(this));
                }
            }

            //generation time always goes down, but only at the end so consumeTriggerValid doesn't assume fake items
            generateTime -= delta() / (itemDuration * itemDurationMultiplier);
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
