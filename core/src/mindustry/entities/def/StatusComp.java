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
import mindustry.world.blocks.*;

import java.io.*;

import static mindustry.Vars.content;

@Component
abstract class StatusComp implements Posc, Flyingc{
    private Array<StatusEntry> statuses = new Array<>();
    private Bits applied = new Bits(content.getBy(ContentType.status).size);

    @ReadOnly float speedMultiplier;
    @ReadOnly float damageMultiplier;
    @ReadOnly float armorMultiplier;

    /** @return damage taken based on status armor multipliers */
    float getShieldDamage(float amount){
        return amount * Mathf.clamp(1f - armorMultiplier / 100f);
    }

    void apply(StatusEffect effect, float duration){
        if(effect == StatusEffects.none || effect == null || isImmune(effect)) return; //don't apply empty or immune effects

        if(statuses.size > 0){
            //check for opposite effects
            for(StatusEntry entry : statuses){
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

    boolean isBoss(){
        return hasEffect(StatusEffects.boss);
    }

    boolean isImmune(StatusEffect effect){
        return false;
    }

    Color statusColor(){
        if(statuses.size == 0){
            return Tmp.c1.set(Color.white);
        }

        float r = 0f, g = 0f, b = 0f;
        for(StatusEntry entry : statuses){
            r += entry.effect.color.r;
            g += entry.effect.color.g;
            b += entry.effect.color.b;
        }
        return Tmp.c1.set(r / statuses.size, g / statuses.size, b / statuses.size, 1f);
    }

    @Override
    public void update(){
        Floor floor = floorOn();
        if(isGrounded() && floor.status != null){
            //apply effect
            apply(floor.status, floor.statusDuration);
        }

        applied.clear();
        speedMultiplier = damageMultiplier = armorMultiplier = 1f;

        if(statuses.isEmpty()) return;

        statuses.eachFilter(entry -> {
            entry.time = Math.max(entry.time - Time.delta(), 0);
            applied.set(entry.effect.id);

            if(entry.time <= 0){
                Pools.free(entry);
                return true;
            }else{
                speedMultiplier *= entry.effect.speedMultiplier;
                armorMultiplier *= entry.effect.armorMultiplier;
                damageMultiplier *= entry.effect.damageMultiplier;
                //TODO ugly casting
                entry.effect.update((Unitc)this, entry.time);
            }

            return false;
        });
    }

    boolean hasEffect(StatusEffect effect){
        return applied.get(effect.id);
    }

    void writeSave(DataOutput stream) throws IOException{
        stream.writeByte(statuses.size);
        for(StatusEntry entry : statuses){
            stream.writeByte(entry.effect.id);
            stream.writeFloat(entry.time);
        }
    }

    void readSave(DataInput stream, byte version) throws IOException{
        for(StatusEntry effect : statuses){
            Pools.free(effect);
        }

        statuses.clear();

        byte amount = stream.readByte();
        for(int i = 0; i < amount; i++){
            byte id = stream.readByte();
            float time = stream.readFloat();
            StatusEntry entry = Pools.obtain(StatusEntry.class, StatusEntry::new);
            entry.set(content.getByID(ContentType.status, id), time);
            statuses.add(entry);
        }
    }
}
