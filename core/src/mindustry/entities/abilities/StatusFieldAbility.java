package mindustry.entities.abilities;

import arc.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;

public class StatusFieldAbility extends Ability{
    public StatusEffect effect;
    public float duration = 60, reload = 100, range = 20;
    public Effect applyEffect = Fx.heal;
    public Effect activeEffect = Fx.overdriveWave;

    protected float timer;

    StatusFieldAbility(){}

    public StatusFieldAbility(StatusEffect effect, float duration, float reload, float range){
        this.duration = duration;
        this.reload = reload;
        this.range = range;
        this.effect = effect;
    }

    @Override
    public String localized(){
        return Core.bundle.format("ability.statusfield", effect.emoji());
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;

        if(timer >= reload){
            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                other.apply(effect, duration);
            });

            activeEffect.at(unit);

            timer = 0f;
        }
    }
}
