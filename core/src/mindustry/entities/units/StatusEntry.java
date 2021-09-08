package mindustry.entities.units;

import mindustry.type.*;

public class StatusEntry{
    public StatusEffect effect;
    public float time;

    public StatusEntry set(StatusEffect effect, float time){
        this.effect = effect;
        this.time = time;
        return this;
    }
}
