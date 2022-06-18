package mindustry.entities.bullet;

import mindustry.content.*;

/** Template class for a non-drawing bullet type that makes an explosion and disappears instantly. */
public class ExplosionBulletType extends BulletType{

    public ExplosionBulletType(float splashDamage, float splashDamageRadius){
        this.splashDamage = splashDamage;
        this.splashDamageRadius = splashDamageRadius;
        rangeOverride = Math.max(rangeOverride, splashDamageRadius * 2f / 3f);
    }

    public ExplosionBulletType(){
    }

    {
        hittable = false;
        lifetime = 1f;
        speed = 0f;
        rangeOverride = 20f;
        shootEffect = Fx.massiveExplosion;
        instantDisappear = true;
        scaledSplashDamage = true;
        killShooter = true;
        collides = false;
        keepVelocity = false;
    }
}
