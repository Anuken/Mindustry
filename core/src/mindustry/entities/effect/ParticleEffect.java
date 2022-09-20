package mindustry.entities.effect;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/** The most essential effect class. Can create particles in various shapes. */
public class ParticleEffect extends Effect{
    private static final Rand rand = new Rand();
    private static final Vec2 rv = new Vec2();

    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    public int particles = 6;
    public boolean randLength = true;
    /** Gives the effect flipping compatability like casing effects. */
    public boolean casingFlip;
    public float cone = 180f, length = 20f, baseLength = 0f;
    /** Particle size/length/radius interpolation. */
    public Interp interp = Interp.linear;
    /** Particle size interpolation. Null to use interp. */
    public @Nullable Interp sizeInterp = null;
    public float offsetX, offsetY;
    public float lightScl = 2f, lightOpacity = 0.6f;
    public @Nullable Color lightColor;

    //region only

    /** Spin in degrees per tick. */
    public float spin = 0f;
    /** Controls the initial and final sprite sizes. */
    public float sizeFrom = 2f, sizeTo = 0f;
    /** Whether the rotation adds with the parent */
    public boolean useRotation = true;
    /** Rotation offset. */
    public float offset = 0;
    /** Sprite to draw. */
    public String region = "circle";

    //line only
    public boolean line;
    public float strokeFrom = 2f, strokeTo = 0f, lenFrom = 4f, lenTo = 2f;

    private @Nullable TextureRegion tex;

    @Override
    public void init(){
        clip = Math.max(clip, length + Math.max(sizeFrom, sizeTo));
        if(sizeInterp == null) sizeInterp = interp;
    }

    @Override
    public void render(EffectContainer e){
        if(tex == null) tex = Core.atlas.find(region);

        float realRotation = (useRotation ? (casingFlip ? Math.abs(e.rotation) : e.rotation) : baseRotation);
        int flip = casingFlip ? -Mathf.sign(e.rotation) : 1;
        float rawfin = e.fin();
        float fin = e.fin(interp);
        float rad = sizeInterp.apply(sizeFrom, sizeTo, rawfin) * 2;
        float ox = e.x + Angles.trnsx(realRotation, offsetX * flip, offsetY), oy = e.y + Angles.trnsy(realRotation, offsetX * flip, offsetY);

        Draw.color(colorFrom, colorTo, fin);
        Color lightColor = this.lightColor == null ? Draw.getColor() : this.lightColor;

        if(line){
            Lines.stroke(sizeInterp.apply(strokeFrom, strokeTo, rawfin));
            float len = sizeInterp.apply(lenFrom, lenTo, rawfin);

            rand.setSeed(e.id);
            for(int i = 0; i < particles; i++){
                float l = length * fin + baseLength;
                rv.trns(realRotation + rand.range(cone), !randLength ? l : rand.random(l));
                float x = rv.x, y = rv.y;

                Lines.lineAngle(ox + x, oy + y, Mathf.angle(x, y), len);
                Drawf.light(ox + x, oy + y, len * lightScl, lightColor, lightOpacity * Draw.getColor().a);
            }
        }else{
            rand.setSeed(e.id);
            for(int i = 0; i < particles; i++){
                float l = length * fin + baseLength;
                rv.trns(realRotation + rand.range(cone), !randLength ? l : rand.random(l));
                float x = rv.x, y = rv.y;

                Draw.rect(tex, ox + x, oy + y, rad, rad, realRotation + offset + e.time * spin);
                Drawf.light(ox + x, oy + y, rad * lightScl, lightColor, lightOpacity * Draw.getColor().a);
            }
        }
    }
}
