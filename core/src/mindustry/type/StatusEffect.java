package mindustry.type;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class StatusEffect extends UnlockableContent{
    /** Damage dealt by the unit with the effect. */
    public float damageMultiplier = 1f;
    /** Unit health multiplier. */
    public float healthMultiplier = 1f;
    /** Unit speed multiplier. */
    public float speedMultiplier = 1f;
    /** Unit reload multiplier. */
    public float reloadMultiplier = 1f;
    /** Unit build speed multiplier. */
    public float buildSpeedMultiplier = 1f;
    /** Unit drag multiplier. */
    public float dragMultiplier = 1f;
    /** Damage dealt upon transition to an affinity. */
    public float transitionDamage = 0f;
    /** Unit weapon(s) disabled. */
    public boolean disarm = false;
    /** Damage per frame. */
    public float damage;
    /** Chance of effect appearing. */
    public float effectChance = 0.15f;
    /** If true, the effect never disappears. */
    public boolean permanent;
    /** If true, this effect will only react with other effects and cannot be applied. */
    public boolean reactive;
    /** Tint color of effect. */
    public Color color = Color.white.cpy();
    /** Effect that happens randomly on top of the affected unit. */
    public Effect effect = Fx.none;
    /** Transition handler map. */
    protected ObjectMap<StatusEffect, TransitionHandler> transitions = new ObjectMap<>();
    /** Called on init. */
    protected Runnable initblock = () -> {};

    public ObjectSet<StatusEffect> affinities = new ObjectSet<>();
    public ObjectSet<StatusEffect> opposites = new ObjectSet<>();

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

    @Override
    public boolean isHidden(){
        return localizedName.equals(name);
    }

    @Override
    public void setStats(){
        if(damageMultiplier != 1) stats.addPercent(Stat.damageMultiplier, damageMultiplier);
        if(healthMultiplier != 1) stats.addPercent(Stat.healthMultiplier, healthMultiplier);
        if(speedMultiplier != 1) stats.addPercent(Stat.speedMultiplier, speedMultiplier);
        if(reloadMultiplier != 1) stats.addPercent(Stat.reloadMultiplier, reloadMultiplier);
        if(buildSpeedMultiplier != 1) stats.addPercent(Stat.buildSpeedMultiplier, buildSpeedMultiplier);
        if(damage > 0) stats.add(Stat.damage, damage * 60f, StatUnit.perSecond);

        boolean reacts = false;

        for(var e : opposites.asArray().sort()){
            stats.add(Stat.opposites, e.emoji() + "" + e);
        }

        if(reactive){
            var other = Vars.content.statusEffects().find(f -> f.affinities.contains(this));
            if(other != null && other.transitionDamage > 0){
                stats.add(Stat.reactive, other.emoji() + other + " / [accent]" + (int)other.transitionDamage + "[lightgray] " + Stat.damage.localized());
                reacts = true;
            }
        }

        //don't list affinities *and* reactions, as that would be redundant
        if(!reacts){
            for(var e : affinities.asArray().sort()){
                stats.add(Stat.affinities, e.emoji() + "" + e);
            }

            if(affinities.size > 0 && transitionDamage != 0){
                stats.add(Stat.affinities, "/ [accent]" + (int)transitionDamage + " " + Stat.damage.localized());
            }
        }

    }

    @Override
    public Cicon prefDatabaseIcon(){
        return Cicon.large;
    }

    @Override
    public boolean showUnlock(){
        return false;
    }

    /** Runs every tick on the affected unit while time is greater than 0. */
    public void update(Unit unit, float time){
        if(damage > 0){
            unit.damageContinuousPierce(damage);
        }else if(damage < 0){ //heal unit
            unit.heal(-1f * damage * Time.delta);
        }

        if(effect != Fx.none && Mathf.chanceDelta(effectChance)){
            Tmp.v1.rnd(unit.type.hitSize /2f);
            effect.at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y);
        }
    }

    protected void trans(StatusEffect effect, TransitionHandler handler){
        transitions.put(effect, handler);
        effect.transitions.put(this, handler);
    }

    protected void affinity(StatusEffect effect, TransitionHandler handler){
        affinities.add(effect);
        effect.affinities.add(this);
        trans(effect, handler);
    }

    protected void opposite(StatusEffect... effect){
        opposites.addAll(effect);
        for(StatusEffect sup : effect){
            sup.opposites.add(this);
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

    public void draw(Unit unit){

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
    public String toString(){
        return localizedName;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.status;
    }

    public interface TransitionHandler{
        void handle(Unit unit, float time, float newTime, StatusEntry result);
    }
}
