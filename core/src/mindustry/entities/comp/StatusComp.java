package mindustry.entities.comp;

import arc.graphics.*;
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

import static mindustry.Vars.*;

@Component
abstract class StatusComp implements Posc, Flyingc{
    private Seq<StatusEntry> statuses = new Seq<>();
    private transient Bits applied = new Bits(content.getBy(ContentType.status).size);

    //these are considered read-only
    transient float speedMultiplier = 1, damageMultiplier = 1, healthMultiplier = 1, reloadMultiplier = 1, buildSpeedMultiplier = 1, dragMultiplier = 1;
    transient boolean disarmed = false;

    @Import UnitType type;

    /** Apply a status effect for 1 tick (for permanent effects) **/
    void apply(StatusEffect effect){
        apply(effect, 1);
    }

    /** Adds a status effect to this unit. */
    void apply(StatusEffect effect, float duration){
        if(effect == StatusEffects.none || effect == null || isImmune(effect)) return; //don't apply empty or immune effects

        //unlock status effects regardless of whether they were applied to friendly units
        if(state.isCampaign()){
            effect.unlock();
        }

        if(statuses.size > 0){
            //check for opposite effects
            for(int i = 0; i < statuses.size; i ++){
                StatusEntry entry = statuses.get(i);
                //extend effect
                if(entry.effect == effect){
                    entry.time = Math.max(entry.time, duration);
                    return;
                }else if(entry.effect.applyTransition(self(), effect, entry, duration)){ //find reaction
                    //TODO effect may react with multiple other effects
                    //stop looking when one is found
                    return;
                }
            }
        }

        if(!effect.reactive){
            //otherwise, no opposites found, add direct effect
            StatusEntry entry = Pools.obtain(StatusEntry.class, StatusEntry::new);
            entry.set(effect, duration);
            statuses.add(entry);
        }
    }

    float getDuration(StatusEffect effect){
        var entry = statuses.find(e -> e.effect == effect);
        return entry == null ? 0 : entry.time;
    }

    void clearStatuses(){
        statuses.clear();
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
        if(isGrounded() && !type.hovering){
            //apply effect
            apply(floor.status, floor.statusDuration);
        }

        applied.clear();
        speedMultiplier = damageMultiplier = healthMultiplier = reloadMultiplier = buildSpeedMultiplier = dragMultiplier = 1f;
        disarmed = false;

        if(statuses.isEmpty()) return;

        int index = 0;

        while(index < statuses.size){
            StatusEntry entry = statuses.get(index++);

            entry.time = Math.max(entry.time - Time.delta, 0);

            if(entry.effect == null || (entry.time <= 0 && !entry.effect.permanent)){
                Pools.free(entry);
                index --;
                statuses.remove(index);
            }else{
                applied.set(entry.effect.id);

                speedMultiplier *= entry.effect.speedMultiplier;
                healthMultiplier *= entry.effect.healthMultiplier;
                damageMultiplier *= entry.effect.damageMultiplier;
                reloadMultiplier *= entry.effect.reloadMultiplier;
                buildSpeedMultiplier *= entry.effect.buildSpeedMultiplier;
                dragMultiplier *= entry.effect.dragMultiplier;

                disarmed |= entry.effect.disarm;

                entry.effect.update(self(), entry.time);
            }
        }
    }

    public Bits statusBits(){
        return applied;
    }

    public void draw(){
        for(StatusEntry e : statuses){
            e.effect.draw(self(), e.time);
        }
    }

    boolean hasEffect(StatusEffect effect){
        return applied.get(effect.id);
    }
}
