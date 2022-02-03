package mindustry.entities.part;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;

public abstract class WeaponPart{
    public static final PartParams params = new PartParams();

    /** If true, turret shading is used. Don't touch this, it is set up in unit/block init()! */
    public boolean turretShading;
    /** If true, the layer is overridden to be under the weapon/turret itself. */
    public boolean under = false;

    public abstract void draw(PartParams params);
    public abstract void load(String name);
    public void getOutlines(Seq<TextureRegion> out){}

    /** Parameters for drawing a part in draw(). */
    public static class PartParams{
        //TODO document
        public float warmup, reload, smoothReload, heat;
        public float x, y, rotation;
        public int sideOverride = -1;

        public PartParams set(float warmup, float reload, float smoothReload, float heat, float x, float y, float rotation){
            this.warmup = warmup;
            this.reload = reload;
            this.heat = heat;
            this.smoothReload = smoothReload;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.sideOverride = -1;
            return this;
        }
    }

    public interface PartProgress{
        PartProgress

        reload = p -> p.reload,
        smoothReload = p -> p.smoothReload,
        warmup = p -> p.warmup,
        heat = p -> p.heat;

        float get(PartParams p);

        static PartProgress constant(float value){
            return p -> value;
        }

        default PartProgress inv(){
            return p -> 1f - get(p);
        }

        default PartProgress delay(float amount){
            return p -> Mathf.clamp((get(p) - amount) / (1f - amount));
        }

        default PartProgress shorten(float amount){
            return p -> Mathf.clamp(get(p) / (1f - amount));
        }

        default PartProgress blend(PartProgress other, float amount){
            return p -> Mathf.lerp(get(p), other.get(p), amount);
        }

        default PartProgress mul(PartProgress other){
            return p -> get(p) * other.get(p);
        }

        default PartProgress min(PartProgress other){
            return p -> Math.min(get(p), other.get(p));
        }

        default PartProgress sin(float scl, float mag){
            return p -> Mathf.clamp(get(p) + Mathf.sin(scl, mag));
        }

        default PartProgress absin(float scl, float mag){
            return p -> Mathf.clamp(get(p) + Mathf.absin(scl, mag));
        }

        default PartProgress apply(PartProgress other, PartFunc func){
            return p -> func.get(get(p), other.get(p));
        }

        default PartProgress add(float amount){
            return p -> Mathf.clamp(get(p) + amount);
        }

        default PartProgress curve(Interp interp){
            return p -> interp.apply(get(p));
        }
    }

    public interface PartFunc{
        float get(float a, float b);
    }
}
