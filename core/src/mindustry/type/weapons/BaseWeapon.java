package mindustry.type.weapons;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class BaseWeapon implements Cloneable{
    /** displayed weapon region */
    public String name;
    /** whether weapon should appear in the stats of a unit with this weapon */
    public boolean display = true;
    /** Whether to show the sprite of the weapon in the database. */
    public boolean showStatSprite = true;
    /** whether to create a flipped copy of this weapon upon initialization. default: true */
    public boolean mirror = true;
    /** whether to flip the weapon's sprite when rendering. internal use only - do not set! */
    public boolean flipSprite = false;
    /** whether to draw the outline on top. */
    public boolean top = true;
    /** whether to rotate toward the target independently of unit */
    public boolean rotate = false;
    /** rotation at which this weapon starts at. TODO buggy!*/
    public float baseRotation = 0f;
    /** rotation speed of weapon when rotation is enabled, in degrees/t*/
    public float rotateSpeed = 20f;
    /** Cone in which the weapon can rotate relative to its mount. */
    public float rotationLimit = 361f;
    /** The half-radius of the cone in which shooting will start. */
    public float shootCone = 5f;
    /** offsets of weapon position on unit */
    public float x = 5f, y = 0f;
    /** projectile/effect offsets from center of weapon */
    public float shootX = 0f, shootY = 3f;
    /** radius of shadow drawn under the weapon; <0 to disable */
    public float shadow = -1f;
    /** draw Z offset relative to the default value */
    public float layerOffset = 0f;
    /** lerp speed for shoot warmup, only used for parts */
    public float shootWarmupSpeed = 0.1f;
    /** If true, shoot warmup is linear instead of a curve. */
    public boolean linearWarmup = false;
    /** visual weapon knockback. */
    public float recoil = 1.5f;
    /** time taken for weapon to return to starting position in ticks. uses reload time by default */
    public float recoilTime = -1f;
    /** power curve applied to visual recoil */
    public float recoilPow = 1.8f;
    /** displayed region (autoloaded) */
    public TextureRegion region;
    /** heat region, must be same size as region (optional) */
    public TextureRegion heatRegion;
    /** cell region, must be same size as region (optional) */
    public TextureRegion cellRegion;
    /** outline region to display if top is false */
    public TextureRegion outlineRegion;
    /** heat region tint */
    public Color heatColor = Pal.turretHeat;
    /** type of weapon mount to be used */
    public Func<BaseWeapon, BaseWeaponMount> mountType = BaseWeaponMount::new;
    /** extra animated parts */
    public Seq<DrawPart> parts = new Seq<>(DrawPart.class);

    public BaseWeapon(String name){
        this.name = name;
    }

    public BaseWeapon(){
        this("");
    }

    public boolean hasStats(UnitType u){
        return display;
    }

    public void draw(Unit unit, BaseWeaponMount mount){

        float
            rotation = unit.rotation - 90,
            realRecoil = Mathf.pow(mount.recoil, recoilPow) * recoil,
            weaponRotation  = rotation + (rotate ? mount.rotation : baseRotation),
            wx = unit.x + Angles.trnsx(rotation, x, y),
            wy = unit.y + Angles.trnsy(rotation, x, y);

        drawWeapon(unit, mount, wx, wy, weaponRotation);
    }

    public void drawWeapon(Unit unit, BaseWeaponMount mount, float wx, float wy, float weaponRotation){
        //apply layer offset, roll it back at the end
        float z = Draw.z();
        Draw.z(z + layerOffset);

        if(shadow > 0){
            Drawf.shadow(wx, wy, shadow);
        }

        if(top){
            drawOutline(unit, mount, wx, wy, weaponRotation);
        }

        if(parts.size > 0){
            drawPartsUnder(unit, mount, wx, wy, weaponRotation);
        }

        Draw.xscl = -Mathf.sign(flipSprite);

        //fix color
        unit.type.applyColor(unit);

        if(region.found()) Draw.rect(region, wx, wy, weaponRotation);

        if(cellRegion.found()){
            Draw.color(unit.type.cellColor(unit));
            Draw.rect(cellRegion, wx, wy, weaponRotation);
            Draw.color();
        }

        if(heatRegion.found() && mount.heat > 0){
            Draw.color(heatColor, mount.heat);
            Draw.blend(Blending.additive);
            Draw.rect(heatRegion, wx, wy, weaponRotation);
            Draw.blend();
            Draw.color();
        }

        Draw.xscl = 1f;

        if(parts.size > 0){
            drawPartsOver(unit, mount, wx, wy, weaponRotation);
        }

        Draw.z(z);
    }

    public void drawOutline(Unit unit, BaseWeaponMount mount, float wx, float wy, float weaponRotation){
        if(!outlineRegion.found()) return;

        Draw.xscl = -Mathf.sign(flipSprite);
        Draw.rect(outlineRegion, wx, wy, weaponRotation);
        Draw.xscl = 1f;
    }

    public void drawPartsUnder(Unit unit, BaseWeaponMount mount, float wx, float wy, float weaponRotation){
        DrawPart.params.set(mount.warmup, 0, 0, mount.heat, mount.recoil, 0, wx, wy, weaponRotation + 90);
        DrawPart.params.sideMultiplier = flipSprite ? -1 : 1;

        for(int i = 0; i < parts.size; i++){
            var part = parts.get(i);
            if(part.under){
                unit.type.applyColor(unit);
                part.draw(DrawPart.params);
            }
        }
    }

    public void drawPartsOver(Unit unit, BaseWeaponMount mount, float wx, float wy, float weaponRotation){
        //Params do not need to be set a second time.
        //TODO does it need an outline?
        for(int i = 0; i < parts.size; i++){
            var part = parts.get(i);
            if(!part.under){
                unit.type.applyColor(unit);
                part.draw(DrawPart.params);
            }
        }
    }

    public void update(Unit unit, BaseWeaponMount mount){
        mount.recoil = Mathf.approachDelta(mount.recoil, 0, 1 / recoilTime);
        float warmupTarget = mount.shoot ? 1f : 0f;
        if(linearWarmup){
            mount.warmup = Mathf.approachDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }else{
            mount.warmup = Mathf.lerpDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }

        updateRotation(unit, mount);
    }

    public void updateRotation(Unit unit, BaseWeaponMount mount){
        if(rotate && mount.rotate){
            float axisX = unit.x + Angles.trnsx(unit.rotation - 90,  x, y),
                axisY = unit.y + Angles.trnsy(unit.rotation - 90,  x, y);

            mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - unit.rotation;
            mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, rotateSpeed * Time.delta);
            if(rotationLimit < 360){
                float dst = Angles.angleDist(mount.rotation, baseRotation);
                if(dst > rotationLimit/2f){
                    mount.rotation = Angles.moveToward(mount.rotation, baseRotation, dst - rotationLimit/2f);
                }
            }
        }else{
            mount.rotation = baseRotation;
            mount.targetRotation = unit.angleTo(mount.aimX, mount.aimY);
        }
    }

    public BaseWeapon copy(){
        try{
            return (BaseWeapon)clone();
        }catch(CloneNotSupportedException excuseMe){
            throw new RuntimeException("how have you done this", excuseMe);
        }
    }
}
