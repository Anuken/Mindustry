package mindustry.entities.comp;

import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class HealthComp implements Entityc, Posc{
    static final float hitDuration = 9f;
    static final float recentDamageTime = 60f * 5f;

    float health;
    transient float hitTime;
    transient float maxHealth = 1f;
    transient boolean dead;
    transient float lastDamageTime = -60f * 5f; //annotated fields must be isolated
    transient float lastHealTime = -120f * 10f;

    boolean isValid(){
        return !dead && isAdded();
    }

    float healthf(){
        return health / maxHealth;
    }

    @Override
    public void update(){
        hitTime -= Time.delta / hitDuration;
    }

    void killed(){
        //implement by other components
    }

    void kill(){
        if(dead) return;

        health = Math.min(health, 0);
        dead = true;
        killed();
        remove();
    }

    boolean damaged(){
        return health < maxHealth - 0.001f;
    }

    /** Damage and pierce armor. */
    void damagePierce(float amount, boolean withEffect){
        damage(amount, withEffect);
    }

    /** Damage and pierce armor. */
    void damagePierce(float amount){
        damagePierce(amount, true);
    }

    /** Damage and multiply armor received. */
    void damageArmorMult(float amount, float armorMult, boolean withEffect){
        damage(amount, withEffect);
    }

    /** Damage and multiply armor received. */
    void damageArmorMult(float amount, float armorMult){
        damageArmorMult(amount, armorMult, true);
    }

    void damage(float amount){
        if(Float.isNaN(health)) health = 0f;

        lastDamageTime = Vars.state.time;
        health -= amount;
        hitTime = 1f;
        if(health <= 0 && !dead){
            kill();
        }
    }

    boolean wasRecentlyDamaged(){
        return lastDamageTime + recentDamageTime >= Vars.state.time;
    }

    boolean wasRecentlyDamaged(float duration){
        return lastDamageTime + duration >= Vars.state.time;
    }

    void damage(float amount, boolean withEffect){
        float pre = hitTime;

        damage(amount);

        if(!withEffect){
            hitTime = pre;
        }
    }

    void damageContinuous(float amount){
        damage(amount * Time.delta, hitTime <= -10 + hitDuration);
    }

    void damageContinuousPierce(float amount){
        damagePierce(amount * Time.delta, hitTime <= -20 + hitDuration);
    }

    void damageContinuousArmorMult(float amount, float armorMult){
        damageArmorMult(amount * Time.delta, armorMult, hitTime <= -20 + hitDuration);
    }

    void clampHealth(){
        health = Math.min(health, maxHealth);
        if(Float.isNaN(health)) health = 0f;
    }

    void recentlyHealed(){
        lastHealTime = Vars.state.time;
    }

    boolean wasRecentlyHealed(float duration){
        return lastHealTime + duration >= Vars.state.time;
    }

    void heal(){
        dead = false;
        health = maxHealth;
        recentlyHealed();
    }

    /** Heals by a flat amount. */
    void heal(float amount){
        health += amount;
        clampHealth();
        recentlyHealed();
    }

    /** Heals by a 0-1 fraction of max health. */
    void healFract(float amount){
        heal(amount * maxHealth);
    }
}
