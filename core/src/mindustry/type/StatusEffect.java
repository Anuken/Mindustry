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
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
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
    /** Should the effect be given a parent */
    public boolean parentizeEffect;
    /** If true, the effect never disappears. */
    public boolean permanent;
    /** If true, this effect will only react with other effects and cannot be applied. */
    public boolean reactive;
    /** Whether to show this effect in the database. */
    public boolean show = true;
    /** Tint color of effect. */
    public Color color = Color.white.cpy();
    /** Effect that happens randomly on top of the affected unit. */
    public Effect effect = Fx.none;
    /** Affinity & opposite values for stat displays. */
    public ObjectSet<StatusEffect> affinities = new ObjectSet<>(), opposites = new ObjectSet<>();
    /** Set to false to disable outline generation. */
    public boolean outline = true;
    /** Transition handler map. */
    protected ObjectMap<StatusEffect, TransitionHandler> transitions = new ObjectMap<>();
    /** Called on init. */
    protected Runnable initblock = () -> {};

    public StatusEffect(String name){
        super(name);
    }

    @Override
    public void init(){
        if(initblock != null){
            initblock.run();
        }
    }

    public void init(Runnable run){
        this.initblock = run;
    }

    @Override
    public boolean isHidden(){
        return localizedName.equals(name) || !show;
    }

    @Override
    public void setStats(){
        if(damageMultiplier != 1) stats.addPercent(Stat.damageMultiplier, damageMultiplier);
        if(healthMultiplier != 1) stats.addPercent(Stat.healthMultiplier, healthMultiplier);
        if(speedMultiplier != 1) stats.addPercent(Stat.speedMultiplier, speedMultiplier);
        if(reloadMultiplier != 1) stats.addPercent(Stat.reloadMultiplier, reloadMultiplier);
        if(buildSpeedMultiplier != 1) stats.addPercent(Stat.buildSpeedMultiplier, buildSpeedMultiplier);
        if(damage > 0) stats.add(Stat.damage, damage * 60f, StatUnit.perSecond);
        if(damage < 0) stats.add(Stat.healing, -damage * 60f, StatUnit.perSecond);

        boolean reacts = false;

        for(var e : opposites.toSeq().sort()){
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
            for(var e : affinities.toSeq().sort()){
                stats.add(Stat.affinities, e.emoji() + "" + e);
            }

            if(affinities.size > 0 && transitionDamage != 0){
                stats.add(Stat.affinities, "/ [accent]" + (int)transitionDamage + " " + Stat.damage.localized());
            }
        }

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
            Tmp.v1.rnd(Mathf.range(unit.type.hitSize/2f));
            effect.at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, 0, color, parentizeEffect ? unit : null);
        }
    }

    protected void trans(StatusEffect effect, TransitionHandler handler){
        transitions.put(effect, handler);
    }

    protected void affinity(StatusEffect effect, TransitionHandler handler){
        affinities.add(effect);
        effect.affinities.add(this);
        trans(effect, handler);
    }

    protected void opposite(StatusEffect... effect){
        for(var other : effect){
            handleOpposite(other);
            other.handleOpposite(this);
        }
    }

    protected void handleOpposite(StatusEffect other){
        opposites.add(other);
        trans(other, (unit, result, time) -> {
            result.time -= time * 0.5f;
            if(result.time <= 0f){
                result.time = time;
                result.effect = other;
            }
        });
    }

    public void draw(Unit unit, float time){
        draw(unit); //Backwards compatibility
    }

    public void draw(Unit unit){

    }

    public boolean reactsWith(StatusEffect effect){
        return transitions.containsKey(effect);
    }

    /**
     * Called when transitioning between two status effects.
     * @param to The state to transition to
     * @param time The applies status effect time
     * @return whether a reaction occurred
     */
    public boolean applyTransition(Unit unit, StatusEffect to, StatusEntry entry, float time){
        var trans = transitions.get(to);
        if(trans != null){
            trans.handle(unit, entry, time);
            return true;
        }
        return false;
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        if(outline){
            makeOutline(PageType.ui, packer, uiIcon, true, Pal.gray, 3);
        }
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
        void handle(Unit unit, StatusEntry current, float time);
    }
}
