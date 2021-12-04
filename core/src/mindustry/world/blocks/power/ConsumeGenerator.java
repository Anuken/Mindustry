package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

/**
 * A generator that just takes in certain items or liquids. Basically SingleTypeGenerator, but not unreliable garbage.
 */
public class ConsumeGenerator extends PowerGenerator{
    /** The time in number of ticks during which a single item will produce power. */
    public float itemDuration = 120f;

    public float effectChance = 0.01f;
    public Effect generateEffect = Fx.none;
    public float generateEffectRange = 3f;

    public @Nullable LiquidStack liquidOutput;

    public ConsumeGenerator(String name){
        super(name);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(liquidOutput != null){
            addLiquidBar(liquidOutput.liquid);
        }
    }

    @Override
    public void init(){
        if(liquidOutput != null){
            outputsLiquid = true;
            hasLiquids = true;
        }
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

        if(liquidOutput != null){
            stats.add(Stat.output, StatValues.liquid(liquidOutput.liquid, liquidOutput.amount * 60f, true));
        }
    }

    public class ConsumeGeneratorBuild extends GeneratorBuild{
        public float warmup, totalTime;

        @Override
        public void updateTile(){
            boolean valid = consValid();

            warmup = Mathf.lerpDelta(warmup, enabled && valid ? 1f : 0f, 0.05f);

            productionEfficiency = valid ? 1f : 0f;
            totalTime += warmup * Time.delta;

            //randomly produce the effect
            if(valid && Mathf.chanceDelta(effectChance)){
                generateEffect.at(x + Mathf.range(generateEffectRange), y + Mathf.range(generateEffectRange));
            }

            //take in items periodically
            if(hasItems && valid && generateTime <= 0f && items.any()){
                consume();
                generateTime = 1f;
            }

            if(liquidOutput != null){
                float added = Math.min(productionEfficiency * delta() * liquidOutput.amount, liquidCapacity - liquids.get(liquidOutput.liquid));
                liquids.add(liquidOutput.liquid, added);
                dumpLiquid(liquidOutput.liquid);
            }

            //generation time always goes down, but only at the end so consumeTriggerValid doesn't assume fake items
            generateTime -= Math.min(1f / itemDuration * delta(), generateTime);
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
            //TODO
            Drawf.light(team, x, y, (60f + Mathf.absin(10f, 5f)) * size, Color.orange, 0.5f * warmup);
        }
    }
}
