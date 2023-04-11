package mindustry.entities.part;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

public abstract class DrawPart{
    public static final PartParams params = new PartParams();

    /** If true, turret shading is used. Don't touch this, it is set up in unit/block init()! */
    public boolean turretShading;
    /** If true, the layer is overridden to be under the weapon/turret itself. */
    public boolean under = false;
    /** For units, this is the index of the weapon this part gets its progress for. */
    public int weaponIndex = 0;
    /** Which recoil counter to use. < 0 to use base recoil.  */
    public int recoilIndex = -1;

    public abstract void draw(PartParams params);
    public abstract void load(String name);
    public void getOutlines(Seq<TextureRegion> out){}

    /** Parameters for drawing a part in draw(). */
    public static class PartParams{
        //TODO document
        public float warmup, reload, smoothReload, heat, recoil, life, charge;
        public float x, y, rotation;
        public int sideOverride = -1, sideMultiplier = 1;

        public PartParams set(float warmup, float reload, float smoothReload, float heat, float recoil, float charge, float x, float y, float rotation){
            this.warmup = warmup;
            this.reload = reload;
            this.heat = heat;
            this.recoil = recoil;
            this.smoothReload = smoothReload;
            this.charge = charge;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.sideOverride = -1;
            this.life = 0f;
            this.sideMultiplier = 1;
            return this;
        }

        public PartParams setRecoil(float recoils){
            this.recoil = recoils;
            return this;
        }
    }

    public static class PartMove{
        public PartProgress progress = PartProgress.warmup;
        public float x, y, gx, gy, rot;

        public PartMove(PartProgress progress, float x, float y, float gx, float gy, float rot){
            this.progress = progress;
            this.x = x;
            this.y = y;
            this.gx = gx;
            this.gy = gy;
            this.rot = rot;
        }
        public PartMove(PartProgress progress, float x, float y, float rot){
            this(progress, x, y, 0, 0, rot);
        }

        public PartMove(){
        }
    }

    public interface PartProgress{
        /** Reload of the weapon - 1 right after shooting, 0 when ready to fire*/
        PartProgress
        reload = p -> p.reload,
        /** Reload, but smoothed out, so there is no sudden jump between 0-1. */
        smoothReload = p -> p.smoothReload,
        /** Weapon warmup, 0 when not firing, 1 when actively shooting. Not equivalent to heat. */
        warmup = p -> p.warmup,
        /** Weapon charge, 0 when beginning to charge, 1 when finished */
        charge = p -> p.charge,
        /** Weapon recoil with no curve applied. */
        recoil = p -> p.recoil,
        /** Weapon heat, 1 when just fired, 0, when it has cooled down (duration depends on weapon) */
        heat = p -> p.heat,
        /** Lifetime fraction, 0 to 1. Only for missiles. */
        life = p -> p.life;

        float get(PartParams p);

        static PartProgress constant(float value){
            return p -> value;
        }

        default float getClamp(PartParams p){
            return Mathf.clamp(get(p));
        }

        default PartProgress inv(){
            return p -> 1f - get(p);
        }

        default PartProgress slope(){
            return p -> Mathf.slope(get(p));
        }

        default PartProgress clamp(){
            return p -> Mathf.clamp(get(p));
        }

        default PartProgress add(float amount){
            return p -> get(p) + amount;
        }

        default PartProgress add(PartProgress other){
            return p -> get(p) + other.get(p);
        }

        default PartProgress delay(float amount){
            return p -> (get(p) - amount) / (1f - amount);
        }

        default PartProgress curve(float offset, float duration){
            return p -> (get(p) - offset) / duration;
        }

        default PartProgress sustain(float offset, float grow, float sustain){
            return p -> {
                float val = get(p) - offset;
                return Math.min(Math.max(val, 0f) / grow, (grow + sustain + grow - val) / grow);
            };
        }

        default PartProgress shorten(float amount){
            return p -> get(p) / (1f - amount);
        }

        default PartProgress compress(float start, float end){
            return p -> Mathf.curve(get(p), start, end);
        }

        default PartProgress blend(PartProgress other, float amount){
            return p -> Mathf.lerp(get(p), other.get(p), amount);
        }

        default PartProgress mul(PartProgress other){
            return p -> get(p) * other.get(p);
        }

        default PartProgress mul(float amount){
            return p -> get(p) * amount;
        }

        default PartProgress min(PartProgress other){
            return p -> Math.min(get(p), other.get(p));
        }

        default PartProgress sin(float offset, float scl, float mag){
            return p -> get(p) + Mathf.sin(Time.time + offset, scl, mag);
        }

        default PartProgress sin(float scl, float mag){
            return p -> get(p) + Mathf.sin(scl, mag);
        }

        default PartProgress absin(float scl, float mag){
            return p -> get(p) + Mathf.absin(scl, mag);
        }

        default PartProgress apply(PartProgress other, PartFunc func){
            return p -> func.get(get(p), other.get(p));
        }

        default PartProgress curve(Interp interp){
            return p -> interp.apply(get(p));
        }
    }

    public interface PartFunc{
        float get(float a, float b);
    }
}
