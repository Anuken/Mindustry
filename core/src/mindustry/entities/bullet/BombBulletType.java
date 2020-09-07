package mindustry.entities.bullet;

import mindustry.gen.*;

public class BombBulletType extends BasicBulletType{

    public BombBulletType(float damage, float radius, String sprite){
        super(0.7f, 0, sprite);
        splashDamageRadius = radius;
        splashDamage = damage;
        collidesTiles = false;
        collides = false;
        shrinkY = 0.7f;
        lifetime = 30f;
        drag = 0.05f;
        keepVelocity = false;
        collidesAir = false;
        hitSound = Sounds.explosion;
    }

    public BombBulletType(float damage, float radius){
        this(damage, radius, "shell");
    }

    public BombBulletType(){
        this(1f, 1f, "shell");
    }
}
