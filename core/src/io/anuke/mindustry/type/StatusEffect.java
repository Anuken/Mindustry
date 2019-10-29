package io.anuke.mindustry.type;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.func.Prov;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.entities.units.Statuses.StatusEntry;
import io.anuke.mindustry.ctype.Content;

public class StatusEffect extends Content{
    public float damageMultiplier = 1f; //damage dealt
    public float armorMultiplier = 1f; //armor points
    public float speedMultiplier = 1f; //speed
    public Color color = Color.white.cpy(); //tint color

    /** Transition handler map. */
    private ObjectMap<StatusEffect, TransitionHandler> transitions = new ObjectMap<>();
    /**
     * Transition initializer array. Since provided effects are only available after init(), this handles putting things
     * in the transitions map.
     */
    private Array<Object[]> transInit = new Array<>();

    /** Damage per frame. */
    protected float damage;
    /** Effect that happens randomly on top of the affected unit. */
    protected Effect effect = Fx.none;

    @SuppressWarnings("unchecked")
    @Override
    public void init(){
        for(Object[] pair : transInit){
            Prov<StatusEffect> sup = (Prov<StatusEffect>)pair[0];
            TransitionHandler handler = (TransitionHandler)pair[1];
            transitions.put(sup.get(), handler);
        }
        transInit.clear();
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

    protected void trans(Prov<StatusEffect> effect, TransitionHandler handler){
        transInit.add(new Object[]{effect, handler});
    }

    @SuppressWarnings("unchecked")
    protected void opposite(Prov... effect){
        for(Prov<StatusEffect> sup : effect){
            trans(sup, (unit, time, newTime, result) -> {
                time -= newTime * 0.5f;
                if(time > 0){
                    result.set(this, time);
                    return;
                }
                result.set(sup.get(), newTime);
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
