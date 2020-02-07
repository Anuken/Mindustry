package mindustry.entities.def;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class HealthComp implements Entityc{
    static final float hitDuration = 9f;

    float health, maxHealth = 1f, hitTime;
    boolean dead;

    boolean isValid(){
        return !dead && isAdded();
    }

    float healthf(){
        return health / maxHealth;
    }

    @Override
    public void update(){
        hitTime -= Time.delta() / hitDuration;
    }

    float hitAlpha(){
        return hitTime / hitDuration;
    }

    void killed(){
        //implement by other components
    }

    void kill(){
        health = 0;
        dead = true;
    }

    void heal(){
        dead = false;
        health = maxHealth;
    }

    boolean damaged(){
        return health <= maxHealth - 0.0001f;
    }

    void damage(float amount){
        health -= amount;
        if(health <= 0 && !dead){
            dead = true;
            killed();
        }
    }

    void damage(float amount, boolean withEffect){
        float pre = hitTime;

        damage(amount);

        if(!withEffect){
            hitTime = pre;
        }
    }

    void damageContinuous(float amount){
        damage(amount * Time.delta(), hitTime <= -20 + hitDuration);
    }

    void clampHealth(){
        health = Mathf.clamp(health, 0, maxHealth);
    }

    void heal(float amount){
        health += amount;
        clampHealth();
    }
}
