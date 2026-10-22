package mindustry.world.consumers;

import arc.math.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/** Consumer for blocks which receive heat from adjacent heat emitters. */
public class ConsumeHeat extends Consume{
    /** Base heat requirement for 100% efficiency. */
    public float requirement;
    /** After heat meets the requirement, excess heat will be scaled by this number. */
    public float overheatScale = 1f;
    /** Maximum possible efficiency after overheat. */
    public float maxEfficiency = 4f;

    public ConsumeHeat(float requirement){
        this.requirement = requirement;
    }

    public ConsumeHeat overheatScale(float overheatScale){
        this.overheatScale = overheatScale;
        return this;
    }

    public ConsumeHeat maxEfficiency(float maxEfficiency){
        this.maxEfficiency = maxEfficiency;
        return this;
    }

    @Override
    public void apply(Block block){
        block.hasHeat = true;
        block.consHeat = this;
    }

    /** Only gates on whether any heat is present. The amount of heat scales efficiency in {@link #efficiencyScale(Building)}, like it always has. */
    @Override
    public float efficiency(Building build){
        return build.heatModule.input > 0f ? 1f : 0f;
    }

    /** @return input heat as a fraction of the requirement, in [0, 1]. */
    public float fraction(Building build){
        return requirement <= 0f ? 1f : Mathf.clamp(build.heatModule.input / requirement);
    }

    /** @return efficiency multiplier from the amount of heat, including overheat. Heat requirements are bypassed when cheating. */
    public float efficiencyScale(Building build){
        if(requirement <= 0f) return 1f;

        float heat = build.heatModule.input;
        float scale = Math.min(fraction(build) + Math.max(heat - requirement, 0f) / requirement * overheatScale, maxEfficiency);
        return build.cheating() ? Math.min(Math.max(scale, 1f), maxEfficiency) : scale;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, requirement, StatUnit.heatUnits);
        if(!optional && maxEfficiency > 0f) stats.add(Stat.maxEfficiency, (int)(maxEfficiency * 100f), StatUnit.percent);
    }
}
