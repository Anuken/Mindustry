package mindustry.entities.def;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.Bits;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

import java.io.*;

import static mindustry.Vars.content;

public class EntityComps{

    @Depends({HealthComp.class, VelComp.class, StatusComp.class})
    class UnitComp{

    }

    class OwnerComp{
        Entityc owner;
    }

    @Depends({TimedComp.class})
    class BulletComp{
        BulletType bullet;

        void init(){
            bullet.init();
        }
    }

    abstract class TimedComp extends EntityComp implements Scaled{
        float time, lifetime;

        void update(){
            time = Math.min(time + Time.delta(), lifetime);

            if(time >= lifetime){
                remove();
            }
        }

        @Override
        public float fin(){
            return time / lifetime;
        }
    }

    class HealthComp{
        float health, maxHealth;
        boolean dead;

        float healthf(){
            return health / maxHealth;
        }
    }

    abstract class PosComp implements Position{
        float x, y;

        void set(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    @Depends(PosComp.class)
    class VelComp{
        //transient fields act as imports from any other component clases; these are ignored by the generator
        transient float x, y;

        final Vec2 vel = new Vec2();

        void update(){
            x += vel.x;
            y += vel.y;
            vel.scl(0.9f);
        }
    }

    @Depends(PosComp.class)
    class HitboxComp{
        transient float x, y;

        float hitSize;

        boolean collides(Hitboxc other){
            return Intersector.overlapsRect(x - hitSize/2f, y - hitSize/2f, hitSize, hitSize,
            other.getX() - other.getHitSize()/2f, other.getY() - other.getHitSize()/2f, other.getHitSize(), other.getHitSize());
        }
    }

    @Depends(PosComp.class)
    class StatusComp{
        private Array<StatusEntry> statuses = new Array<>();
        private Bits applied = new Bits(content.getBy(ContentType.status).size);

        private float speedMultiplier;
        private float damageMultiplier;
        private float armorMultiplier;

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
                        //TODO unit cannot be null here
                        entry.effect.getTransition(null, effect, entry.time, duration, StatusEntry.tmp);
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

        boolean isImmune(StatusEffect effect){
            return false;
        }

        Color getStatusColor(){
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

        void update(){
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
                    //TODO unit can't be null
                    entry.effect.update(null, entry.time);
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

    @BaseComponent
    class EntityComp{
        int id;

        void init(){}

        void update(){}

        void remove(){}

        <T> T as(Class<T> type){
            return (T)this;
        }
    }
}
