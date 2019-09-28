package io.anuke.mindustry.entities.units;

import io.anuke.arc.collection.Bits;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.type.*;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

/** Class for controlling status effects on an entity. */
public class Statuses implements Saveable{
    private static final StatusEntry globalResult = new StatusEntry();
    private static final Array<StatusEntry> removals = new Array<>();

    private Array<StatusEntry> statuses = new Array<>();
    private Bits applied = new Bits(content.getBy(ContentType.status).size);

    private float speedMultiplier;
    private float damageMultiplier;
    private float armorMultiplier;

    public void handleApply(Unit unit, StatusEffect effect, float duration){
        if(effect == StatusEffects.none || effect == null || unit.isImmune(effect)) return; //don't apply empty or immune effects

        if(statuses.size > 0){
            //check for opposite effects
            for(StatusEntry entry : statuses){
                //extend effect
                if(entry.effect == effect){
                    entry.time = Math.max(entry.time, duration);
                    return;
                }else if(entry.effect.reactsWith(effect)){ //find opposite
                    globalResult.effect = entry.effect;
                    entry.effect.getTransition(unit, effect, entry.time, duration, globalResult);
                    entry.time = globalResult.time;

                    if(globalResult.effect != entry.effect){
                        entry.effect = globalResult.effect;
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

    public Color getStatusColor(){
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

    public void clear(){
        statuses.clear();
    }

    public void update(Unit unit){
        applied.clear();
        speedMultiplier = damageMultiplier = armorMultiplier = 1f;

        if(statuses.size == 0) return;

        removals.clear();

        for(StatusEntry entry : statuses){
            entry.time = Math.max(entry.time - Time.delta(), 0);
            applied.set(entry.effect.id);

            if(entry.time <= 0){
                Pools.free(entry);
                removals.add(entry);
            }else{
                speedMultiplier *= entry.effect.speedMultiplier;
                armorMultiplier *= entry.effect.armorMultiplier;
                damageMultiplier *= entry.effect.damageMultiplier;
                entry.effect.update(unit, entry.time);
            }
        }

        if(removals.size > 0){
            statuses.removeAll(removals, true);
        }
    }

    public float getSpeedMultiplier(){
        return speedMultiplier;
    }

    public float getDamageMultiplier(){
        return damageMultiplier;
    }

    public float getArmorMultiplier(){
        return armorMultiplier;
    }

    public boolean hasEffect(StatusEffect effect){
        return applied.get(effect.id);
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeByte(statuses.size);
        for(StatusEntry entry : statuses){
            stream.writeByte(entry.effect.id);
            stream.writeFloat(entry.time);
        }
    }

    @Override
    public void readSave(DataInput stream, byte version) throws IOException{
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

    public static class StatusEntry{
        public StatusEffect effect;
        public float time;

        public StatusEntry set(StatusEffect effect, float time){
            this.effect = effect;
            this.time = time;
            return this;
        }
    }
}
