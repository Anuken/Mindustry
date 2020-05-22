package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.util.ArcAnnotate.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class Weapon{
    /** displayed weapon region */
    public String name;
    /** bullet shot */
    public @NonNull BulletType bullet;
    /** shell ejection effect */
    public Effect ejectEffect = Fx.none;
    /** whether to mirror the weapon (draw two of them, which is the default) */
    public boolean mirror = true;
    /** whether to flip the weapon's position/side on the ship (only valid when mirror is false) */
    public boolean flipped = false;
    /** whether to shoot the weapons in different arms one after another, rather than all at once; only valid when mirror = true */
    public boolean alternate = false;
    /** whether to rotate toward the target independently of unit */
    public boolean rotate = false;
    /** rotation speed of weapon when rotation is enabled, in degrees/t*/
    public float rotateSpeed = 20f;
    /** weapon reload in frames */
    public float reload;
    /** amount of shots per fire */
    public int shots = 1;
    /** spacing in degrees between multiple shots, if applicable */
    public float spacing = 12f;
    /** inaccuracy of degrees of each shot */
    public float inaccuracy = 0f;
    /** intensity and duration of each shot's screen shake */
    public float shake = 0f;
    /** visual weapon knockback. */
    public float recoil = 1.5f;
    /** projectile/effect offsets from center of weapon */
    public float shootX = 0f, shootY = 3f;
    /** offsets of weapon position on unit */
    public float x = 5f, y = 0f;
    /** fraction of velocity that is random */
    public float velocityRnd = 0f;
    /** randomization of shot length */
    public float lengthRand = 0f;
    /** delay in ticks between shots */
    public float shotDelay = 0;
    /** The half-radius of the cone in which shooting will start. */
    public float shootCone = 1.5f;
    /** whether shooter rotation is ignored when shooting. */
    public boolean ignoreRotation = false;
    /** sound used for shooting */
    public Sound shootSound = Sounds.pew;
    /** displayed region (autoloaded) */
    public TextureRegion region;

    public Weapon(String name){
        this.name = name;
    }

    public Weapon(){
        this("");
    }

    public void load(){
        region = Core.atlas.find(name, Core.atlas.find("clear"));
    }

}
