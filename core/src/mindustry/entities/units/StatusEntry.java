package mindustry.entities.units;

import mindustry.type.*;

public class StatusEntry{
    public StatusEffect effect;
    public float time;
    public int level;

    public StatusEntry set(StatusEffect effect, float time){
        return set(effect, time, 1);
    }

    public StatusEntry set(StatusEffect effect, float time, int level){
        this.effect = effect;
        this.time = time;
        this.level = level;
        return this;
    }
}
