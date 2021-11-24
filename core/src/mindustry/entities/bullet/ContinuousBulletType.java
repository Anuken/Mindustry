package mindustry.entities.bullet;

import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

/** Basic continuous bullet type that does not draw itself. Essentially abstract. */
public class ContinuousBulletType extends BulletType{
    public float length = 220f;
    public float shake = 0f;
    public float damageInterval = 5f;
    public boolean largeHit = false;

    {
        speed = 0f;
        despawnEffect = Fx.none;
        shootEffect = Fx.none;
        lifetime = 16f;
        impact = true;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
    }

    @Override
    public float continuousDamage(){
        return damage / damageInterval * 60f;
    }

    @Override
    public float estimateDPS(){
        //assume firing duration is about 100 by default, may not be accurate there's no way of knowing in this method
        //assume it pierces 3 blocks/units
        return damage * 100f / damageInterval * 3f;
    }

    @Override
    public float range(){
        return Math.max(length, maxRange);
    }

    @Override
    public void init(){
        super.init();

        drawSize = Math.max(drawSize, length*2f);
    }

    @Override
    public void update(Bullet b){

        //damage every 5 ticks
        if(b.timer(1, damageInterval)){
            Damage.collideLine(b, b.team, hitEffect, b.x, b.y, b.rotation(), length, largeHit);
        }

        if(shake > 0){
            Effect.shake(shake, shake, b);
        }
    }

}
