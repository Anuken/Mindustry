package mindustry.entities.bullet;

public class EmptyBulletType extends BulletType{

    public EmptyBulletType(){
        hittable = collidesGround = collidesAir = collidesTiles = false;
        damage = speed = 0f;
        keepVelocity = false;
    }
}
