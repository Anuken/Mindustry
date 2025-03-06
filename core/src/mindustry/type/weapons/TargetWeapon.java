package mindustry.type.weapons;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class TargetWeapon extends BaseWeapon{
    /** whether this weapon can be aimed manually by players */
    public boolean controllable = true;
    /** whether this weapon can be automatically aimed by the unit */
    public boolean aiControllable = true;
    /** whether to automatically target relevant units in update(); only works when controllable = false. */
    public boolean autoTarget = false;
    /** ticks to wait in-between targets */
    public float targetInterval = 40f, targetSwitchInterval = 70f;
    /** This weapon's range */
    public float range = 80;
    /** Whether to target air units */
    public boolean targetAir;
    /** Whether to target ground units or blocks */
    public boolean targetGround;

    public TargetWeapon(String name){
        super(name);
    }

    public TargetWeapon(){
        super();
    }

    {
        mountType = TargetWeaponMount::new;
    }

    public float range(){
        return range;
    }

    @Override
    public void update(Unit unit, BaseWeaponMount mount){
        updateReductions(unit, mount);
        updateTargeting(unit, (TargetWeaponMount)mount);
        updateRotation(unit, mount);
    }

    public void updateTargeting(Unit unit, TargetWeaponMount mount){
        if(!controllable && autoTarget){
            float
                mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
                mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y);

            if((mount.retarget -= Time.delta) <= 0f){
                mount.target = findTarget(unit, mountX, mountY, range(), targetAir, targetGround);
                mount.retarget = mount.target == null ? targetInterval : targetSwitchInterval;
            }

            if(mount.target != null && checkTarget(unit, mount.target, mountX, mountY, range())){
                mount.target = null;
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, range() + Math.abs(shootY) + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && unit.canShoot();

                mount.aimX = mount.target.x();
                mount.aimY = mount.target.y();
            }

            mount.shoot = mount.rotate = shoot;

            //note that shooting state is not affected, as these cannot be controlled
            //logic will return shooting as false even if these return true, which is fine
        }
    }

    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        return null;
    }

    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return false;
    }

    public static class TargetWeaponMount extends BaseWeaponMount{
        /** current target; used for autonomous weapons and AI */
        public @Nullable Teamc target;
        /** retarget counter */
        public float retarget = 0f;

        public TargetWeaponMount(BaseWeapon weapon){
            super(weapon);
        }
    }
}
