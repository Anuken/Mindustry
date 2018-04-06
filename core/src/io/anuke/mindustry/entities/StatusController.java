package io.anuke.mindustry.entities;

import io.anuke.mindustry.content.StatusEffects;
import io.anuke.ucore.core.Timers;

public class StatusController {
    private static final TransitionResult globalResult = new TransitionResult();

    private StatusEffect current = StatusEffects.none;
    private float time;

    public void handleApply(Unit unit, StatusEffect effect, float intensity){
        if(effect == StatusEffects.none) return; //don't apply empty effects

        float newTime = effect.baseDuration*intensity;

        if(effect == current){
            time = Math.max(time, newTime);
        }else {

            current.getTransition(unit, effect, time, newTime, globalResult);

            if (globalResult.result != current) {
                current.onTransition(unit, globalResult.result);
                time = globalResult.time;
                current = globalResult.result;
            }
        }
    }

    public void update(Unit unit){
        if(time > 0){
            time = Math.max(time - Timers.delta(), 0);
        }

        current.update(unit, time);
    }

    public void set(StatusEffect current, float time){
        this.current = current;
        this.time = time;
    }

    public StatusEffect current() {
        return current;
    }

    public float getTime() {
        return time;
    }

    public static class TransitionResult{
        public StatusEffect result;
        public float time;

        public TransitionResult set(StatusEffect effect, float time){
            this.result = effect;
            this.time = time;
            return this;
        }
    }
}
