package mindustry.type;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class StatusEffect extends MappableContent{
    /** Damage dealt by the unit with the effect. */
    public float damageMultiplier = 1f;
    /** Unit armor multiplier. */
    public float armorMultiplier = 1f;
    /** Unit speed multiplier (buggy) */
    public float speedMultiplier = 1f;
    /** Damage per frame. */
    public float damage;
    /** If true, the effect never disappears. */
    public boolean permanent;
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
    public void update(Unitc unit, float time){
        if(damage > 0){
            unit.damageContinuous(damage);
        }else if(damage < 0){ //heal unit
            unit.heal(damage * Time.delta());
        }

        if(effect != Fx.none && Mathf.chance(Time.delta() * 0.15f)){
            effect.at(unit.getX() + Mathf.range(unit.bounds() / 2f), unit.getY() + Mathf.range(unit.bounds() / 2f));
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

    public void draw(Unitc unit){

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
    public StatusEntry getTransition(Unitc unit, StatusEffect to, float time, float newTime, StatusEntry result){
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
        void handle(Unitc unit, float time, float newTime, StatusEntry result);
    }
}
