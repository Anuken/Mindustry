package mindustry.entities.bullet;

public class EmptyBulletType extends BulletType{

    public EmptyBulletType(){
        hittable = collidesGround = collidesAir = collidesTiles = false;
        speed = 0f;
        keepVelocity = false;
    }
}
