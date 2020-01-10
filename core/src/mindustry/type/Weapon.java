package mindustry.type;

import arc.*;
import mindustry.annotations.Annotations.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.Effects.*;
import mindustry.entities.bullet.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.entities.type.Bullet;
import mindustry.gen.*;

import static mindustry.Vars.net;

public class Weapon{
    public String name;

    /** minimum cursor distance from player, fixes 'cross-eyed' shooting. */
    protected static float minPlayerDist = 20f;
    //temporary only
    protected static int sequenceNum = 0;

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
    /** shoot barrel y offset */
    public float length = 3f;
    /** shoot barrel x offset. */
    public float width = 4f;
    /** fraction of velocity that is random */
    public float velocityRnd = 0f;
    /** randomization of shot length */
    public float lengthRand = 0f;
    /** delay in ticks between shots */
    public float shotDelay = 0;
    /** whether shooter rotation is ignored when shooting. */
    public boolean ignoreRotation = false;
    /** if turnCursor is false for a mech, how far away will the weapon target. */
    public float targetDistance = 1f;
    /** sound used for shooting */
    public Sound shootSound = Sounds.pew;
    /** displayed region (autoloaded) */
    public TextureRegion region;

    protected Weapon(String name){
        this.name = name;
    }

    public Weapon(){
        //no region
        this.name = "";
    }

}
