package io.anuke.mindustry.entities.traits;

import io.anuke.arc.math.Mathf;

public interface HealthTrait{

    void health(float health);

    float health();

    float maxHealth();

    boolean isDead();

    void setDead(boolean dead);

    default void onHit(SolidTrait entity){
    }

    default void onDeath(){
    }

    default boolean damaged(){
        return health() < maxHealth() - 0.0001f;
    }

    default void damage(float amount){
        health(health() - amount);
        if(health() <= 0 && !isDead()){
            onDeath();
            setDead(true);
        }
    }

    default void clampHealth(){
        health(Mathf.clamp(health(), 0, maxHealth()));
    }

    default float healthf(){
        return health() / maxHealth();
    }

    default void healBy(float amount){
        health(health() + amount);
        clampHealth();
    }

    default void heal(){
        health(maxHealth());
        setDead(false);
    }
}
