package io.anuke.mindustry.content;

import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.StatusController.StatusEntry;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class StatusEffects implements ContentList{
    public static StatusEffect none, burning, freezing, wet, melting, tarred, overdrive, shielded, shocked;

    @Override
    public void load(){

        none = new StatusEffect(0);

        burning = new StatusEffect(4 * 60f){
            {
                oppositeScale = 0.5f;
            }

            @Override
            public StatusEntry getTransition(Unit unit, StatusEffect to, float time, float newTime, StatusEntry result){
                if(to == tarred){
                    unit.damage(1f);
                    Effects.effect(EnvironmentFx.burning, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
                    return result.set(this, Math.min(time + newTime, baseDuration + tarred.baseDuration));
                }

                return super.getTransition(unit, to, time, newTime, result);
            }

            @Override
            public void update(Unit unit, float time){
                unit.damagePeriodic(0.04f);

                if(Mathf.chance(Timers.delta() * 0.2f)){
                    Effects.effect(EnvironmentFx.burning, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
                }
            }
        };

        freezing = new StatusEffect(5 * 60f){
            {
                oppositeScale = 0.4f;
                speedMultiplier = 0.6f;
                armorMultiplier = 0.8f;
            }

            @Override
            public void update(Unit unit, float time){

                if(Mathf.chance(Timers.delta() * 0.15f)){
                    Effects.effect(EnvironmentFx.freezing, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
                }
            }
        };

        wet = new StatusEffect(3 * 60f){
            {
                oppositeScale = 0.5f;
                speedMultiplier = 0.9f;
            }

            @Override
            public StatusEntry getTransition(Unit unit, StatusEffect to, float time, float newTime, StatusEntry result){
                if(to == shocked){
                    //get shocked when wet
                    unit.damage(15f);
                    return result.set(this, time);
                }

                return super.getTransition(unit, to, time, newTime, result);
            }

            @Override
            public void update(Unit unit, float time){
                if(Mathf.chance(Timers.delta() * 0.15f)){
                    Effects.effect(EnvironmentFx.wet, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
                }
            }
        };

        melting = new StatusEffect(5 * 60f){
            {
                oppositeScale = 0.2f;
                speedMultiplier = 0.8f;
                armorMultiplier = 0.8f;
            }

            @Override
            public StatusEntry getTransition(Unit unit, StatusEffect to, float time, float newTime, StatusEntry result){
                if(to == tarred){
                    return result.set(this, Math.min(time + newTime / 2f, baseDuration));
                }

                return super.getTransition(unit, to, time, newTime, result);
            }

            @Override
            public void update(Unit unit, float time){
                unit.damagePeriodic(0.3f);

                if(Mathf.chance(Timers.delta() * 0.2f)){
                    Effects.effect(EnvironmentFx.melting, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
                }
            }
        };

        tarred = new StatusEffect(4 * 60f){
            {
                speedMultiplier = 0.6f;
            }

            @Override
            public void update(Unit unit, float time){
                if(Mathf.chance(Timers.delta() * 0.15f)){
                    Effects.effect(EnvironmentFx.oily, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
                }
            }

            @Override
            public StatusEntry getTransition(Unit unit, StatusEffect to, float time, float newTime, StatusEntry result){
                if(to == melting || to == burning){
                    return result.set(to, newTime + time);
                }

                return result.set(to, newTime);
            }
        };

        overdrive = new StatusEffect(60f*15){
            {
                armorMultiplier = 0.95f;
                speedMultiplier = 1.15f;
                damageMultiplier = 1.4f;
            }

            @Override
            public void update(Unit unit, float time){
                //idle regen boosted
                unit.health += 0.01f * Timers.delta();

                if(Mathf.chance(Timers.delta() * 0.25f)){
                    Effects.effect(EnvironmentFx.overdriven, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f), 0f, unit);
                }
            }
        };

        shielded = new StatusEffect(6f){
            {
                armorMultiplier = 3f;
            }
        };

        shocked = new StatusEffect(1f){
            {
                armorMultiplier = 3f;
            }
        };

        wet.setOpposites(shocked);
        melting.setOpposites(wet, freezing);
        wet.setOpposites(burning);
        freezing.setOpposites(burning, melting);
        burning.setOpposites(wet, freezing);
    }

    @Override
    public ContentType type(){
        return ContentType.status;
    }
}
