package mindustry.entities;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.bullet.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.net;

public class Weapons{
    private static final int[] one = {1};

    private WeaponMount[] mounts;
    private UnitDef lastDef;

    public void update(Unit unit){
        check(unit);

        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;

            for(int i : (weapon.mirror ? Mathf.signs : one)){
                i *= Mathf.sign(weapon.flipped);
            }
        }
    }

    public void draw(Unit unit){
        check(unit);

        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;

            for(int i : (weapon.mirror ? Mathf.signs : one)){
                i *= Mathf.sign(weapon.flipped);

                float rotation = unit.rotation - 90 + (weapon.rotate ? mount.rotation : 0);
                float trY = weapon.length - mount.reload / weapon.reload * weapon.recoil;
                float width = i > 0 ? -weapon.region.getWidth() : weapon.region.getWidth();

                Draw.rect(weapon.region,
                    unit.x + Angles.trnsx(rotation, weapon.width * i, trY),
                    unit.y + Angles.trnsy(rotation, weapon.width * i, trY),
                    width * Draw.scl,
                    weapon.region.getHeight() * Draw.scl,
                    rotation - 90);
            }
        }
    }

    //check mount validity
    private void check(Unit unit){
        if(mounts == null || mounts.length != unit.type().weapons.size || lastDef != unit.type()){
            mounts = new WeaponMount[unit.type().weapons.size];
            for(int i = 0; i < mounts.length; i++){
                mounts[i] = new WeaponMount(unit.type().weapons.get(i));
            }
            lastDef = unit.type();
        }
    }

    //region weapon code

    @Remote(targets = Loc.server, called = Loc.both, unreliable = true)
    public static void onPlayerShootWeapon(Player player, float x, float y, float rotation, boolean left){

        if(player == null) return;
        //clients do not see their own shoot events: they are simulated completely clientside to prevent laggy visuals
        //messing with the firerate or any other stats does not affect the server (take that, script kiddies!)
        if(net.client() && player == Vars.player){
            return;
        }

        shootDirect(player, x, y, rotation, left);
    }

    @Remote(targets = Loc.server, called = Loc.both, unreliable = true)
    public static void onGenericShootWeapon(ShooterTrait shooter, float x, float y, float rotation, boolean left){
        if(shooter == null) return;
        shootDirect(shooter, x, y, rotation, left);
    }

    public static void shootDirect(ShooterTrait shooter, float offsetX, float offsetY, float rotation, boolean left){
        float x = shooter.getX() + offsetX;
        float y = shooter.getY() + offsetY;
        float baseX = shooter.getX(), baseY = shooter.getY();

        Weapon weapon = shooter.getWeapon();
        weapon.shootSound.at(x, y, Mathf.random(0.8f, 1.0f));

        sequenceNum = 0;
        if(weapon.shotDelay > 0.01f){
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> {
                Time.run(sequenceNum * weapon.shotDelay, () -> weapon.bullet(shooter, x + shooter.getX() - baseX, y + shooter.getY() - baseY, f + Mathf.range(weapon.inaccuracy)));
                sequenceNum++;
            });
        }else{
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> weapon.bullet(shooter, x, y, f + Mathf.range(weapon.inaccuracy)));
        }

        BulletType ammo = weapon.bullet;

        Tmp.v1.trns(rotation + 180f, ammo.recoil);

        shooter.velocity().add(Tmp.v1);

        Tmp.v1.trns(rotation, 3f);
        boolean parentize = ammo.keepVelocity;

        Effects.shake(weapon.shake, weapon.shake, x, y);
        Effects.effect(weapon.ejectEffect, x, y, rotation * -Mathf.sign(left));
        Effects.effect(ammo.shootEffect, x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? shooter : null);
        Effects.effect(ammo.smokeEffect, x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? shooter : null);

        //reset timer for remote players
        shooter.getTimer().get(shooter.getShootTimer(left), weapon.reload);
    }

    public void load(){
        region = Core.atlas.find(name + "-equip", Core.atlas.find(name, Core.atlas.find("clear")));
    }

    public void update(ShooterTrait shooter, float pointerX, float pointerY){
        for(boolean left : Mathf.booleans){
            Tmp.v1.set(pointerX, pointerY).sub(shooter.getX(), shooter.getY());
            if(Tmp.v1.len() < minPlayerDist) Tmp.v1.setLength(minPlayerDist);

            float cx = Tmp.v1.x + shooter.getX(), cy = Tmp.v1.y + shooter.getY();

            float ang = Tmp.v1.angle();
            Tmp.v1.trns(ang - 90, width * Mathf.sign(left), length + Mathf.range(lengthRand));

            update(shooter, shooter.getX() + Tmp.v1.x, shooter.getY() + Tmp.v1.y, Angles.angle(shooter.getX() + Tmp.v1.x, shooter.getY() + Tmp.v1.y, cx, cy), left);
        }
    }

    public void update(ShooterTrait shooter, float mountX, float mountY, float angle, boolean left){
        if(shooter.getTimer().get(shooter.getShootTimer(left), reload)){
            if(alternate){
                shooter.getTimer().reset(shooter.getShootTimer(!left), reload / 2f);
            }

            shoot(shooter, mountX - shooter.getX(), mountY - shooter.getY(), angle, left);
        }
    }

    public void shoot(ShooterTrait p, float x, float y, float angle, boolean left){
        if(net.client()){
            //call it directly, don't invoke on server
            shootDirect(p, x, y, angle, left);
        }else{
            if(p instanceof Player){ //players need special weapon handling logic
                Call.onPlayerShootWeapon((Player)p, x, y, angle, left);
            }else{
                Call.onGenericShootWeapon(p, x, y, angle, left);
            }
        }
    }

    void bullet(ShooterTrait owner, float x, float y, float angle){
        if(owner == null) return;

        Tmp.v1.trns(angle, 3f);
        Bullet.create(bullet, owner, owner.getTeam(), x + Tmp.v1.x, y + Tmp.v1.y, angle, (1f - velocityRnd) + Mathf.random(velocityRnd));
    }

    //endregion

    private static class WeaponMount{
        /** reload in frames; 0 means ready to fire */
        float reload;
        /** rotation relative to the unit this mount is on */
        float rotation;
        /** weapon associated with this mount */
        Weapon weapon;

        public WeaponMount(Weapon weapon){
            this.weapon = weapon;
        }
    }
}
