package mindustry.type.weapons;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * Note that this requires several things:
 * - A bullet with positive maxRange
 * - A bullet with positive damage
 * - Rotation
 * */
public class PointDefenseWeapon extends Weapon{
    public Color color = Color.white;
    public Effect beamEffect = Fx.pointBeam;

    public PointDefenseWeapon(String name){
        super(name);
    }

    public PointDefenseWeapon(){
    }

    {
        predictTarget = false;
        autoTarget = true;
        controllable = false;
        rotate = true;
        useAmmo = false;
        useAttackRange = false;
    }

    @Override
    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        return Groups.bullet.intersect(x - range, y - range, range*2, range*2).min(b -> b.team != unit.team && b.type().hittable, b -> b.dst2(x, y));
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return !(target.within(unit, range) && target.team() != unit.team && target instanceof Bullet bullet && bullet.type != null && bullet.type.hittable);
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
        if(!(mount.target instanceof Bullet target)) return;

        if(target.damage() > bullet.damage){
            target.damage(target.damage() - bullet.damage);
        }else{
            target.remove();
        }

        beamEffect.at(shootX, shootY, rotation, color, new Vec2().set(target));
        bullet.shootEffect.at(shootX, shootY, rotation, color);
        bullet.hitEffect.at(target.x, target.y, color);
        shootSound.at(shootX, shootY, Mathf.random(0.9f, 1.1f));
        mount.recoil = 1f;
        mount.heat = 1f;
    }
}
