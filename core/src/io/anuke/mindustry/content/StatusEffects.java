package io.anuke.mindustry.content;

import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.StatusController.TransitionResult;
import io.anuke.mindustry.entities.StatusEffect;
import io.anuke.mindustry.entities.Unit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class StatusEffects {
    public static final StatusEffect

    none = new StatusEffect(0),

    burning = new StatusEffect(4*60f){
        {
            oppositeScale = 0.5f;
        }

        @Override
        public TransitionResult getTransition(Unit unit, StatusEffect to, float time, float newTime, TransitionResult result){
            if(to == oiled){
                unit.damage(1f, false);
                Effects.effect(EnvironmentFx.burning, unit.x + Mathf.range(unit.getSize()/2f), unit.y + Mathf.range(unit.getSize()/2f));
                return result.set(this, Math.min(time + newTime, baseDuration + oiled.baseDuration));
            }

            return super.getTransition(unit, to, time, newTime, result);
        }

        @Override
        public void update(Unit unit, float time){
            unit.damage(0.04f * Timers.delta(), false);

            if(Mathf.chance(Timers.delta() * 0.2f)){
                Effects.effect(EnvironmentFx.burning, unit.x + Mathf.range(unit.getSize()/2f), unit.y + Mathf.range(unit.getSize()/2f));
            }

        }
    },

    freezing = new StatusEffect(5*60f){
        {
            oppositeScale = 0.4f;
        }

        @Override
        public void update(Unit unit, float time){
            unit.velocity.scl(0.7f);

            if(Mathf.chance(Timers.delta() * 0.15f)){
                Effects.effect(EnvironmentFx.freezing, unit.x + Mathf.range(unit.getSize()/2f), unit.y + Mathf.range(unit.getSize()/2f));
            }
        }
    },

    wet = new StatusEffect(3*60f){
        {
            oppositeScale = 0.5f;
        }

        @Override
        public void update(Unit unit, float time){
            if(Mathf.chance(Timers.delta() * 0.15f)){
                Effects.effect(EnvironmentFx.wet, unit.x + Mathf.range(unit.getSize()/2f), unit.y + Mathf.range(unit.getSize()/2f));
            }

            unit.velocity.scl(0.999f);
        }
    },

    melting = new StatusEffect(5*60f){
        {
            oppositeScale = 0.2f;
        }

        @Override
        public TransitionResult getTransition(Unit unit, StatusEffect to, float time, float newTime, TransitionResult result){
            if(to == oiled){
                return result.set(this, Math.min(time + newTime/2f, baseDuration));
            }

            return super.getTransition(unit, to, time, newTime, result);
        }

        @Override
        public void update(Unit unit, float time){
            unit.velocity.scl(0.8f);
            unit.damage(0.1f * Timers.delta(), false);

            if(Mathf.chance(Timers.delta() * 0.2f)){
                Effects.effect(EnvironmentFx.melting, unit.x + Mathf.range(unit.getSize()/2f), unit.y + Mathf.range(unit.getSize()/2f));
            }
        }
    },

    oiled = new StatusEffect(4*60f){
        @Override
        public void update(Unit unit, float time){
            if(Mathf.chance(Timers.delta() * 0.15f)){
                Effects.effect(EnvironmentFx.oily, unit.x + Mathf.range(unit.getSize()/2f), unit.y + Mathf.range(unit.getSize()/2f));
            }

            unit.velocity.scl(1.001f);
        }

        @Override
        public TransitionResult getTransition(Unit unit, StatusEffect to, float time, float newTime, TransitionResult result){
            if(to == melting || to == burning){
                return result.set(to, newTime + time);
            }

            return result.set(to, newTime);
        }
    };

    static{
        melting.setOpposites(wet, freezing);
        wet.setOpposites(burning);
        freezing.setOpposites(burning, melting);
        burning.setOpposites(wet, freezing);
    }
}
