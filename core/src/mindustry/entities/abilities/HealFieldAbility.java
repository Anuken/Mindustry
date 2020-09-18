package mindustry.entities.abilities;

import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class HealFieldAbility extends Ability{
    public float amount = 1, reload = 100, range = 60;
    public Effect healEffect = Fx.heal;
    public Effect activeEffect = Fx.healWave;

    protected float timer;
    protected boolean wasHealed = false;

    HealFieldAbility(){}

    public HealFieldAbility(float amount, float reload, float range){
        this.amount = amount;
        this.reload = reload;
        this.range = range;
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;

        if(timer >= reload){
            wasHealed = false;

            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                if(other.damaged()){
                    healEffect.at(unit);
                    wasHealed = true;
                }
                other.heal(amount);
            });

            if(wasHealed){
                activeEffect.at(unit);
            }

            timer = 0f;
        }
    }
}
