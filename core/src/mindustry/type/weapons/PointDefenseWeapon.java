package mindustry.type.weapons;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class PointDefenseWeapon extends ReloadWeapon{
    public Color color = Color.white;
    public Effect beamEffect = Fx.pointBeam;
    public Effect hitEffect = Fx.hitBulletSmall;
    public float damage = 10;

    public PointDefenseWeapon(String name){
        super(name);
    }

    public PointDefenseWeapon(){
        super();
    }

    {
        autoTarget = true;
        controllable = false;
        rotate = true;
        targetInterval = 10f;
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
    protected void shoot(Unit unit, ReloadWeaponMount mount, float shootX, float shootY, float rotation){
        if(!(mount.target instanceof Bullet target)) return;

        // not sure whether it should multiply by the damageMultiplier of the unit
        float bulletDamage = damage * unit.damageMultiplier() * state.rules.unitDamage(unit.team);
        if(target.damage() > bulletDamage){
            target.damage(target.damage() - bulletDamage);
        }else{
            target.remove();
        }

        beamEffect.at(shootX, shootY, rotation, color, new Vec2().set(target));
        shootEffect.at(shootX, shootY, rotation, color);
        hitEffect.at(target.x, target.y, color);
        shootSound.at(shootX, shootY, Mathf.random(0.9f, 1.1f));
        mount.recoil = 1f;
        mount.heat = 1f;
    }
}
