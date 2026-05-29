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
    public void load(String name){}
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
        life = p -> p.life,
        /** Current unscaled value of Time.time. */
        time = p -> Time.time;

        float get(PartParams p);

        static PartProgress constant(float value){
            return p -> value;
        }

        default float getClamp(PartParams p){
            return getClamp(p, true);
        }

        default float getClamp(PartParams p, boolean clamp){
            return clamp ? Mathf.clamp(get(p)) : get(p);
        }

        default PartProgress inv(){
            return CompatFix.inv(this);
        }

        default PartProgress slope(){
            return CompatFix.slope(this);
        }

        default PartProgress clamp(){
            return CompatFix.clamp(this);
        }

        default PartProgress add(float amount){
            return CompatFix.add(this, amount);
        }

        default PartProgress add(PartProgress other){
            return CompatFix.add(this, other);
        }

        default PartProgress delay(float amount){
            return CompatFix.delay(this, amount);
        }

        default PartProgress curve(float offset, float duration){
            return CompatFix.curve(this, offset, duration);
        }

        default PartProgress sustain(float offset, float grow, float sustain){
            return CompatFix.sustain(this, offset, grow, sustain);
        }

        default PartProgress shorten(float amount){
            return CompatFix.shorten(this, amount);
        }

        default PartProgress compress(float start, float end){
            return CompatFix.compress(this, start, end);
        }

        default PartProgress blend(PartProgress other, float amount){
            return CompatFix.blend(this, other, amount);
        }

        default PartProgress mul(PartProgress other){
            return CompatFix.mul(this, other);
        }

        default PartProgress mul(float amount){
            return CompatFix.mul(this, amount);
        }

        default PartProgress min(PartProgress other){
            return CompatFix.min(this, other);
        }

        default PartProgress sin(float offset, float scl, float mag){
            return CompatFix.sin(this, offset, scl, mag);
        }

        default PartProgress sin(float scl, float mag){
            return CompatFix.sin(this, scl, mag);
        }

        default PartProgress absin(float scl, float mag){
            return CompatFix.absin(this, scl, mag);
        }

        default PartProgress mod(float amount){
            return CompatFix.mod(this, amount);
        }

        default PartProgress loop(float time){
            return CompatFix.loop(this, time);
        }

        default PartProgress apply(PartProgress other, PartFunc func){
            return CompatFix.apply(this, other, func);
        }

        default PartProgress curve(Interp interp){
            return CompatFix.curve(this, interp);
        }
    }

    public interface PartFunc{
        float get(float a, float b);
    }

    /** RoboVM chokes on lambdas referencing self in default methods in interfaces, so they have to be moved into a separate class. */
    private static class CompatFix{

        static PartProgress inv(PartProgress self){
            return p -> 1f - self.get(p);
        }

        static PartProgress slope(PartProgress self){
            return p -> Mathf.slope(self.get(p));
        }

        static PartProgress clamp(PartProgress self){
            return p -> Mathf.clamp(self.get(p));
        }

        static PartProgress add(PartProgress self, float amount){
            return p -> self.get(p) + amount;
        }

        static PartProgress add(PartProgress self, PartProgress other){
            return p -> self.get(p) + other.get(p);
        }

        static PartProgress delay(PartProgress self, float amount){
            return p -> (self.get(p) - amount) / (1f - amount);
        }

        static PartProgress curve(PartProgress self, float offset, float duration){
            return p -> (self.get(p) - offset) / duration;
        }

        static PartProgress sustain(PartProgress self, float offset, float grow, float sustain){
            return p -> {
                float val = self.get(p) - offset;
                return Math.min(Math.max(val, 0f) / grow, (grow + sustain + grow - val) / grow);
            };
        }

        static PartProgress shorten(PartProgress self, float amount){
            return p -> self.get(p) / (1f - amount);
        }

        static PartProgress compress(PartProgress self, float start, float end){
            return p -> Mathf.curve(self.get(p), start, end);
        }

        static PartProgress blend(PartProgress self, PartProgress other, float amount){
            return p -> Mathf.lerp(self.get(p), other.get(p), amount);
        }

        static PartProgress mul(PartProgress self, PartProgress other){
            return p -> self.get(p) * other.get(p);
        }

        static PartProgress mul(PartProgress self, float amount){
            return p -> self.get(p) * amount;
        }

        static PartProgress min(PartProgress self, PartProgress other){
            return p -> Math.min(self.get(p), other.get(p));
        }

        static PartProgress sin(PartProgress self, float offset, float scl, float mag){
            return p -> self.get(p) + Mathf.sin(Time.time + offset, scl, mag);
        }

        static PartProgress sin(PartProgress self, float scl, float mag){
            return p -> self.get(p) + Mathf.sin(scl, mag);
        }

        static PartProgress absin(PartProgress self, float scl, float mag){
            return p -> self.get(p) + Mathf.absin(scl, mag);
        }

        static PartProgress mod(PartProgress self, float amount){
            return p -> Mathf.mod(self.get(p), amount);
        }

        static PartProgress loop(PartProgress self, float time){
            return p -> Mathf.mod(self.get(p)/time, 1);
        }

        static PartProgress apply(PartProgress self, PartProgress other, PartFunc func){
            return p -> func.get(self.get(p), other.get(p));
        }

        static PartProgress curve(PartProgress self, Interp interp){
            return p -> interp.apply(self.get(p));
        }
    }
}
