package mindustry.entities.bullet;

import arc.graphics.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class LightningBulletType extends BulletType{
    protected Color lightningColor = Pal.lancerLaser;
    protected int lightningLength = 25, lightningLengthRand = 0;

    public LightningBulletType(){
        super(0.0001f, 1f);

        lifetime = 1;
        despawnEffect = Fx.none;
        hitEffect = Fx.hitLancer;
        keepVelocity = false;
        hittable = false;
    }

    @Override
    public float range(){
        return (lightningLength + lightningLengthRand/2f) * 6f;
    }

    @Override
    public void draw(Bullet b){
    }

    @Override
    public void init(Bullet b){
        Lightning.create(b, lightningColor, damage, b.x, b.y, b.rotation(), lightningLength + Mathf.random(lightningLengthRand));
    }
}
