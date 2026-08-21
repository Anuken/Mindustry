package mindustry.world.blocks.heat;

import arc.math.*;

/** Basic interface for any block that produces heat.*/
public interface HeatBlock{
    float heat();
    /** @return heat as a fraction of max heat */
    float heatFrac();

    /** @return heatOutput scaled by timeScale */
    static float approachHeatOutput(float heatOutput, boolean scaleHeat, float timeScale){
        return heatOutput * (scaleHeat ? timeScale : 1f);
    }

    /** Smoothly approaches heat at a rate regardless of efficiency */
    static float approachHeat(float current, float target, float rate){
        return target < current ? target : Mathf.approach(current, target, Math.min(rate, Math.abs(target - current)));
    }
}
