package mindustry.entities.bullet;

import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class LightningBulletType extends BulletType{

    public LightningBulletType(){
        damage = 1f;
        speed = 0f;
        lifetime = 1;
        despawnEffect = Fx.none;
        hitEffect = Fx.hitLancer;
        keepVelocity = false;
        hittable = false;
        //for stats
        status = StatusEffects.shocked;
        lightningLength = 25;
        lightningLengthRand = 0;
        lightningColor = Pal.lancerLaser;
    }

    @Override
    protected float calculateRange(){
        return (lightningLength + lightningLengthRand/2f) * 6f;
    }

    @Override
    public float estimateDPS(){
        return super.estimateDPS() * Math.max(lightningLength / 10f, 1);
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        Lightning.create(b, lightningColor, damage, b.x, b.y, b.rotation(), lightningLength + Mathf.random(lightningLengthRand));
    }
}
