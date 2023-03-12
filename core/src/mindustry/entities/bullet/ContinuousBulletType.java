package mindustry.entities.bullet;

import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

/** Basic continuous (line) bullet type that does not draw itself. Essentially abstract. */
public class ContinuousBulletType extends BulletType{
    public float length = 220f;
    public float shake = 0f;
    public float damageInterval = 5f;
    public boolean largeHit = false;
    public boolean continuous = true;

    {
        removeAfterPierce = false;
        pierceCap = -1;
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
        if(!continuous) return -1f;
        return damage / damageInterval * 60f;
    }

    @Override
    public float estimateDPS(){
        if(!continuous) return super.estimateDPS();
        //assume firing duration is about 100 by default, may not be accurate there's no way of knowing in this method
        //assume it pierces 3 blocks/units
        return damage * 100f / damageInterval * 3f;
    }

    @Override
    protected float calculateRange(){
        return Math.max(length, maxRange);
    }

    @Override
    public void init(){
        super.init();

        drawSize = Math.max(drawSize, length*2f);
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        if(!continuous){
            applyDamage(b);
        }
    }

    @Override
    public void update(Bullet b){
        if(!continuous) return;

        //damage every 5 ticks
        if(b.timer(1, damageInterval)){
            applyDamage(b);
        }

        if(shake > 0){
            Effect.shake(shake, shake, b);
        }
    }

    public void applyDamage(Bullet b){
        Damage.collideLine(b, b.team, hitEffect, b.x, b.y, b.rotation(), currentLength(b), largeHit, laserAbsorb, pierceCap);
    }

    public float currentLength(Bullet b){
        return length;
    }

}
