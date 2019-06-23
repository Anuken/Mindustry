package io.anuke.mindustry.entities.bullet;

public class BombBulletType extends BasicBulletType{

    public BombBulletType(float damage, float radius, String sprite){
        super(0.7f, 0, sprite);
        splashDamageRadius = radius;
        splashDamage = damage;
        collidesTiles = false;
        collides = false;
        bulletShrink = 0.7f;
        lifetime = 30f;
        drag = 0.05f;
        keepVelocity = false;
        collidesAir = false;
    }
}
