package mindustry.entities.bullet;

import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;

/** This class can only be used with PointDefenseBulletWeapon. Attempting to spawn it in outside of that weapon will lead to standard behavior. */
public class InterceptorBulletType extends BasicBulletType{

    public InterceptorBulletType(float speed, float damage){
        super(speed, damage);
    }

    public InterceptorBulletType(){
    }

    public InterceptorBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(b.data instanceof Bullet other){
            if(other.isAdded()){

                //check for an overlap between the two bullet trajectories; it is the responsibility of the creator to make sure the bullet is a valid target
                if(EntityCollisions.collide(
                    b.x, b.y,
                    b.hitSize, b.hitSize,
                    b.deltaX, b.deltaY,
                    other.x, other.y,
                    other.hitSize, other.hitSize,
                    other.deltaX, other.deltaY, Tmp.v1)){

                    b.set(Tmp.v1);

                    hit(b, b.x, b.y);
                    b.remove();

                    if(other.damage > damage){
                        other.damage -= b.damage;
                    }else{
                        other.remove();
                    }
                }
            }else{
                b.data = null;
            }
        }
    }
}
