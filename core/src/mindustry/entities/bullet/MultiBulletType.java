package mindustry.entities.bullet;

import arc.util.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;

/** A fake bullet type that spawns multiple sub-bullets when "fired". */
public class MultiBulletType extends BulletType{
    public BulletType[] bullets = {};
    /** Amount of times the bullet array is repeated. */
    public int repeat = 1;

    public MultiBulletType(BulletType... bullets){
        this.bullets = bullets;
    }

    public MultiBulletType(int repeat, BulletType... bullets){
        this.repeat = repeat;
        this.bullets = bullets;
    }

    public MultiBulletType(){
    }

    @Override
    public float estimateDPS(){
        float sum = 0f;
        for(var b : bullets){
            sum += b.estimateDPS();
        }
        return sum;
    }

    @Override
    protected float calculateRange(){
        float max = 0f;
        for(var b : bullets){
            max = Math.max(max, b.calculateRange());
        }
        return max;
    }

    @Override
    public @Nullable Bullet create(
        @Nullable Entityc owner, @Nullable Entityc shooter, Team team, float x, float y, float angle, float damage, float velocityScl,
        float lifetimeScl, Object data, @Nullable Mover mover, float aimX, float aimY, @Nullable Teamc target
    ){
        angle += angleOffset;

        Bullet last = null;

        for(int i = 0; i < repeat; i++){
            for(var bullet : bullets){
                last = bullet.create(owner, shooter, team, x, y, angle, damage, velocityScl, lifetimeScl, data, mover, aimX, aimY, target);
            }
        }

        return last;
    }
}
