package mindustry.type.weapons;

import arc.audio.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class ReloadWeapon extends TargetWeapon{
    /** whether to shoot the weapons in different arms one after another, rather than all at once; only valid when mirror = true */
    public boolean alternate = true;
    /** weapon reload in frames */
    public float reload = 1;
    /** lerp speed for reload. Only used for parts */
    public float smoothReloadSpeed = 0.15f;
    /** minimum weapon warmup before firing (this is not linear, do NOT use 1!) */
    public float minWarmup = 0f;
    /** min velocity required for this weapon to shoot */
    public float minShootVelocity = -1f;
    /** whether this weapon is always shooting, regardless of targets ore cone */
    public boolean alwaysShooting = false;
    /** sound used for shooting */
    public Sound shootSound = Sounds.pew;
    public @Nullable Effect shootEffect = Fx.shootSmall;
    public @Nullable Effect smokeEffect = Fx.shootSmallSmoke;
    /** internal value used for alternation - do not change! */
    public int otherSide = -1;

    public ReloadWeapon(String name){
        super(name);
    }

    public ReloadWeapon(){
        super();
    }

    {
        mountType = ReloadWeaponMount::new;
    }

    @Override
    public void update(Unit unit, BaseWeaponMount mount){
        super.update(unit, mount);
        updateShooting(unit, (ReloadWeaponMount)mount);
    }

    public void updateShooting(Unit unit, ReloadWeaponMount mount){
        float lastReload = mount.reload;
        mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
        mount.smoothReload = Mathf.lerpDelta(mount.smoothReload, mount.reload / reload, smoothReloadSpeed);

        float
            mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
            mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
            weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
            bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY),
            bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY),
            shootAngle = bulletRotation(unit, mount, bulletX, bulletY);

        if(alwaysShooting) mount.shoot = true;

        boolean wasFlipped = mount.side;
        if(otherSide != -1 && alternate && mount.side == flipSprite && mount.reload <= reload / 2f && lastReload > reload / 2f){
            ReloadWeaponMount other = (ReloadWeaponMount)unit.mounts[otherSide];
            other.side = !other.side;
            mount.side = !mount.side;
        }

        if( //TODO move to separate method?
            mount.shoot && //must be shooting
            unit.canShoot() && //must be able to shoot
            (!alternate || wasFlipped == flipSprite) &&
            mount.warmup >= minWarmup && //must be warmed up
            unit.vel.len() >= minShootVelocity && //check velocity requirements
            unit.vel.len() >= minShootVelocity && //check velocity requirements
            mount.reload <= 0.0001f && //reload has to be 0
            (alwaysShooting || Angles.within(rotate ? mount.rotation : unit.rotation + baseRotation, mount.targetRotation, shootCone))
        ){
            shoot(unit, mount, bulletX, bulletY, shootAngle);

            mount.reload = reload;
        }
    }

    protected float bulletRotation(Unit unit, ReloadWeaponMount mount, float bulletX, float bulletY){
        return rotate ? unit.rotation + mount.rotation : Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY) + (unit.rotation - unit.angleTo(mount.aimX, mount.aimY)) + baseRotation;
    }

    protected void shoot(Unit unit, ReloadWeaponMount mount, float shootX, float shootY, float rotation){
    }

    @Override
    public void setPartParams(Unit unit, BaseWeaponMount m, float wx, float wy, float weaponRotation){
        ReloadWeaponMount mount = (ReloadWeaponMount)m;
        DrawPart.params.set(mount.warmup, mount.reload / reload, mount.smoothReload, mount.heat, mount.recoil, 0, wx, wy, weaponRotation + 90);
        DrawPart.params.sideMultiplier = flipSprite ? -1 : 1;
    }

    public static class ReloadWeaponMount extends TargetWeaponMount{
        /** reload in ticks; 0 means ready to fire */
        public float reload;
        /** lerps to reload time */
        public float smoothReload;
        /** total bullets fired from this mount */
        public int totalShots;
        /** extra state for alternating weapons */
        public boolean side;

        public ReloadWeaponMount(BaseWeapon weapon){
            super(weapon);
        }
    }
}
