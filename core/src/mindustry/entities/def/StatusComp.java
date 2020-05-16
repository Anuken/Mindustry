package mindustry.entities.def;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

import java.io.*;

import static mindustry.Vars.content;

@Component
abstract class StatusComp implements Posc, Flyingc{
    private Array<StatusEntry> statuses = new Array<>();
    private transient Bits applied = new Bits(content.getBy(ContentType.status).size);

    @ReadOnly transient float speedMultiplier, damageMultiplier, armorMultiplier;

    /** @return damage taken based on status armor multipliers */
    float getShieldDamage(float amount){
        return amount * Mathf.clamp(1f - armorMultiplier / 100f);
    }

    /** Apply a status effect for 1 tick (for permanent effects) **/
    void apply(StatusEffect effect){
        apply(effect, 1);
    }

    /** Adds a status effect to this unit. */
    void apply(StatusEffect effect, float duration){
        if(effect == StatusEffects.none || effect == null || isImmune(effect)) return; //don't apply empty or immune effects

        if(statuses.size > 0){
            //check for opposite effects
            for(int i = 0; i < statuses.size; i ++){
                StatusEntry entry = statuses.get(i);
                //extend effect
                if(entry.effect == effect){
                    entry.time = Math.max(entry.time, duration);
                    return;
                }else if(entry.effect.reactsWith(effect)){ //find opposite
                    StatusEntry.tmp.effect = entry.effect;
                    entry.effect.getTransition((Unitc)this, effect, entry.time, duration, StatusEntry.tmp);
                    entry.time = StatusEntry.tmp.time;

                    if(StatusEntry.tmp.effect != entry.effect){
                        entry.effect = StatusEntry.tmp.effect;
                    }

                    //stop looking when one is found
                    return;
                }
            }
        }

        //otherwise, no opposites found, add direct effect
        StatusEntry entry = Pools.obtain(StatusEntry.class, StatusEntry::new);
        entry.set(effect, duration);
        statuses.add(entry);
    }

    /** Removes a status effect. */
    void unapply(StatusEffect effect){
        statuses.remove(e -> {
            if(e.effect == effect){
                Pools.free(e);
                return true;
            }
            return false;
        });
    }

    boolean isBoss(){
        return hasEffect(StatusEffects.boss);
    }

    abstract boolean isImmune(StatusEffect effect);

    Color statusColor(){
        if(statuses.size == 0){
            return Tmp.c1.set(Color.white);
        }

        float r = 1f, g = 1f, b = 1f, total = 0f;
        for(StatusEntry entry : statuses){
            float intensity = entry.time < 10f ? entry.time/10f : 1f;
            r += entry.effect.color.r * intensity;
            g += entry.effect.color.g * intensity;
            b += entry.effect.color.b * intensity;
            total += intensity;
        }
        float count = statuses.size + total;
        return Tmp.c1.set(r / count, g / count, b / count, 1f);
    }

    @Override
    public void update(){
        Floor floor = floorOn();
        if(isGrounded()){
            //apply effect
            apply(floor.status, floor.statusDuration);
        }

        applied.clear();
        speedMultiplier = damageMultiplier = armorMultiplier = 1f;

        if(statuses.isEmpty()) return;

        int index = 0;

        while(index < statuses.size){
            StatusEntry entry = statuses.get(index++);

            entry.time = Math.max(entry.time - Time.delta(), 0);
            applied.set(entry.effect.id);

            if(entry.time <= 0 && !entry.effect.permanent){
                Pools.free(entry);
                index --;
                statuses.remove(index);
            }else{
                speedMultiplier *= entry.effect.speedMultiplier;
                armorMultiplier *= entry.effect.armorMultiplier;
                damageMultiplier *= entry.effect.damageMultiplier;
                //TODO ugly casting
                entry.effect.update((Unitc)this, entry.time);
            }
        }
    }

    public void draw(){
        for(StatusEntry e : statuses){
            e.effect.draw((Unitc)this);
        }
    }

    boolean hasEffect(StatusEffect effect){
        return applied.get(effect.id);
    }
}
