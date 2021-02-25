package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class Weapon implements Cloneable{
    /** displayed weapon region */
    public String name = "";
    /** bullet shot */
    public BulletType bullet;
    /** shell ejection effect */
    public Effect ejectEffect = Fx.none;
    /** whether to create a flipped copy of this weapon upon initialization. default: true */
    public boolean mirror = true;
    /** whether to flip the weapon's sprite when rendering */
    public boolean flipSprite = false;
    /** whether to shoot the weapons in different arms one after another, rather than all at once; only valid when mirror = true */
    public boolean alternate = true;
    /** whether to rotate toward the target independently of unit */
    public boolean rotate = false;
    /** whether to draw the outline on top. */
    public boolean top = true;
    /** whether to hold the bullet in place while firing */
    public boolean continuous;
    /** rotation speed of weapon when rotation is enabled, in degrees/t*/
    public float rotateSpeed = 20f;
    /** weapon reload in frames */
    public float reload;
    /** amount of shots per fire */
    public int shots = 1;
    /** spacing in degrees between multiple shots, if applicable */
    public float spacing = 0;
    /** inaccuracy of degrees of each shot */
    public float inaccuracy = 0f;
    /** intensity and duration of each shot's screen shake */
    public float shake = 0f;
    /** visual weapon knockback. */
    public float recoil = 1.5f;
    /** ticks recoil takes to return to normal position */
    public float restitutionTime = -1;
    /** projectile/effect offsets from center of weapon */
    public float shootX = 0f, shootY = 3f;
    /** offsets of weapon position on unit */
    public float x = 5f, y = 0f;
    /** random spread on the X axis */
    public float xRand = 0f;
    /** radius of shadow drawn under the weapon; <0 to disable */
    public float shadow = -1f;
    /** fraction of velocity that is random */
    public float velocityRnd = 0f;
    /** delay in ticks between shots */
    public float firstShotDelay = 0;
    /** delay in ticks between shots */
    public float shotDelay = 0;
    /** The half-radius of the cone in which shooting will start. */
    public float shootCone = 5f;
    /** ticks to cool down the heat region */
    public float cooldownTime = 20f;
    /** random sound pitch range */
    public float soundPitchMin = 0.8f, soundPitchMax = 1f;
    /** whether shooter rotation is ignored when shooting. */
    public boolean ignoreRotation = false;
    /** min velocity required for this weapon to shoot */
    public float minShootVelocity = -1f;
    /** internal value used for alternation - do not change! */
    public int otherSide = -1;
    /** sound used for shooting */
    public Sound shootSound = Sounds.pew;
    /** sound used for weapons that have a delay */
    public Sound chargeSound = Sounds.none;
    /** sound played when there is nothing to shoot */
    public Sound noAmmoSound = Sounds.noammo;
    /** displayed region (autoloaded) */
    public TextureRegion region;
    /** heat region, must be same size as region (optional) */
    public TextureRegion heatRegion;
    /** outline region to display if top is false */
    public TextureRegion outlineRegion;
    /** heat region tint */
    public Color heatColor = Pal.turretHeat;
    /** status effect applied when shooting */
    public StatusEffect shootStatus = StatusEffects.none;
    /** status effect duration when shot */
    public float shootStatusDuration = 60f * 5f;

    public Weapon(String name){
        this.name = name;
    }

    public Weapon(){
        this("");
    }

    public Weapon copy(){
        try{
            return (Weapon)clone();
        }catch(CloneNotSupportedException suck){
            throw new RuntimeException("very good language design", suck);
        }
    }

    public void load(){
        region = Core.atlas.find(name, Core.atlas.find("clear"));
        heatRegion = Core.atlas.find(name + "-heat");
        outlineRegion = Core.atlas.find(name + "-outline");
    }

}
