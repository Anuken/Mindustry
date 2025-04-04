package mindustry.type.weapons;

import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

/** Fires a bullet to intercept enemy bullets. The fired bullet MUST be of type InterceptorBulletType. */
public class PointDefenseBulletWeapon extends Weapon{
    public float damageTargetWeight = 10f;

    public PointDefenseBulletWeapon(String name){
        super(name);
    }

    public PointDefenseBulletWeapon(){
    }

    {
        autoTarget = true;
        controllable = false;
        rotate = true;
        useAmmo = false;
        useAttackRange = false;
        targetInterval = targetSwitchInterval = 5f;
    }

    @Override
    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        return Groups.bullet.intersect(x - range, y - range, range*2, range*2).min(b -> b.team != unit.team && b.type().hittable && !(b.type.collidesAir && !b.type.collidesTiles), b -> b.dst2(x, y) - b.damage * damageTargetWeight);
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return !(target.within(unit, range) && target.team() != unit.team && target instanceof Bullet b && b.type != null && b.type.hittable);
    }

    @Override
    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
        super.handleBullet(unit, mount, bullet);

        if(mount.target instanceof Bullet b){
            bullet.data = b;
        }
    }

}
