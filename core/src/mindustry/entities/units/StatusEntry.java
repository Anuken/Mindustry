package mindustry.entities.units;

import mindustry.type.*;

public class StatusEntry{
    public StatusEffect effect;
    public float time;
    //for interval damage
    public float damageTime;

    //all of these are for the dynamic effect only!
    public float damageMultiplier = 1f, healthMultiplier = 1f, speedMultiplier = 1f, reloadMultiplier = 1f, buildSpeedMultiplier = 1f, dragMultiplier = 1f, armorOverride = -1f;

    public StatusEntry set(StatusEffect effect, float time){
        this.effect = effect;
        this.time = time;
        return this;
    }
}
