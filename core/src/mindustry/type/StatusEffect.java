package mindustry.type;

import arc.struct.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.ctype.ContentType;
import mindustry.entities.*;
import mindustry.entities.Effects.*;
import mindustry.entities.type.*;
import mindustry.entities.units.*;

public class StatusEffect extends MappableContent{
    /** Damage dealt by the unit with the effect. */
    public float damageMultiplier = 1f;
    /** Unit armor multiplier. */
    public float armorMultiplier = 1f;
    /** Unit speed multiplier (buggy) */
    public float speedMultiplier = 1f;
    /** Damage per frame. */
    public float damage;
    /** Tint color of effect. */
    public Color color = Color.white.cpy();
    /** Effect that happens randomly on top of the affected unit. */
    public Effect effect = Fx.none;
    /** Transition handler map. */
    protected ObjectMap<StatusEffect, TransitionHandler> transitions = new ObjectMap<>();
    /** Called on init. */
    protected Runnable initblock = () -> {};

    public StatusEffect(String name){
        super(name);
    }

    @Override
    public void init(){
        initblock.run();
    }

    public void init(Runnable run){
        this.initblock = run;
    }

    /** Runs every tick on the affected unit while time is greater than 0. */
    public void update(Unit unit, float time){
        if(damage > 0){
            unit.damagePeriodic(damage);
        }else if(damage < 0){ //heal unit
            unit.healBy(damage * Time.delta());
        }

        if(effect != Fx.none && Mathf.chance(Time.delta() * 0.15f)){
            Effects.effect(effect, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
        }
    }

    protected void trans(StatusEffect effect, TransitionHandler handler){
        transitions.put(effect, handler);
    }

    protected void opposite(StatusEffect... effect){
        for(StatusEffect sup : effect){
            trans(sup, (unit, time, newTime, result) -> {
                time -= newTime * 0.5f;
                if(time > 0){
                    result.set(this, time);
                    return;
                }
                result.set(sup, newTime);
            });
        }
    }

    public boolean reactsWith(StatusEffect effect){
        return transitions.containsKey(effect);
    }

    /**
     * Called when transitioning between two status effects.
     * @param to The state to transition to
     * @param time The current status effect time
     * @param newTime The time that the new status effect will last
     */
    public StatusEntry getTransition(Unit unit, StatusEffect to, float time, float newTime, StatusEntry result){
        if(transitions.containsKey(to)){
            transitions.get(to).handle(unit, time, newTime, result);
            return result;
        }

        return result.set(to, newTime);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.status;
    }

    public interface TransitionHandler{
        void handle(Unit unit, float time, float newTime, StatusEntry result);
    }
}
