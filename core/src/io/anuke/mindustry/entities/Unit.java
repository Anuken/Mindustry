package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.game.Team;

public abstract class Unit extends SyncEntity {
    //total duration of hit effect
    public static final float hitDuration = 9f;

    public StatusController status = new StatusController();
    public Team team = Team.blue;
    public Vector2 velocity = new Vector2();
    public float hitTime;

    @Override
    public void damage(float amount){
        super.damage(amount);
        hitTime = hitDuration;
    }

    public void damage(float amount, boolean withEffect){
        if(withEffect){
            damage(amount);
        }else{
            super.damage(amount);
        }
    }

    public abstract float getMass();
    public abstract boolean isFlying();
    public abstract float getSize();
}
