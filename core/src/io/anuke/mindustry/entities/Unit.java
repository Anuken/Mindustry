package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.game.Team;

public abstract class Unit extends SyncEntity {
    //total duration of hit effect
    public static final float hitDuration = 5f;

    public Team team = Team.blue;
    public Vector2 velocity = new Vector2();
    public float hitTime;

    public abstract float getMass();
}
