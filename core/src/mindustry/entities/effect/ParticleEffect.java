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

    /** Particle color. */
    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    /** Number of particles created. */
    public int particles = 6;
    /** If true, the particle's length is set in a random range from 0 to length. */
    public boolean randLength = true;
    /** Gives the effect flipping compatability like casing effects. */
    public boolean casingFlip;
    public float cone = 180f, length = 20f, baseLength = 0f;
    /** Particle size/length/radius interpolation. */
    public Interp interp = Interp.linear;
    /** Particle size interpolation. Null to use interp. */
    public @Nullable Interp sizeInterp = null;
    /** Particle width interpolation. Null to use interp. */
    public @Nullable Interp colorInterp = null;
    /** Offset position of the particle. */
    public float offsetX, offsetY;
    /** Particle ligght properties. */
    public float lightScl = 2f, lightOpacity = 0.6f;
    /** Color of the light each particle emits. */
    public @Nullable Color lightColor;

    //region only

    /** Spin in degrees per tick. */
    public float spin = 0f;
    /** Controls the initial and final sprite sizes. */
    public float sizeFrom = 2f, sizeTo = 0f;
    /** Controls the amount of ticks the effect waits before changing size. */
    public float sizeChangeStart = 0f;
    /** Controls the amount of ticks the effect waits before changing width. */
    public float widthChangeStart = 0f;
    /** Controls the amount of ticks the effect waits before changing height. */
    public float heightChangeStart = 0f;
    /** Whether the rotation adds with the parent */
    public boolean useRotation = true;
    /** Rotation offset. */
    public float offset = 0;
    /** Sprite to draw. */
    public String region = "circle";
    /** Particle width and height properties as a ratio of its radius. Does nothing to line particles. */
    public float widthFrom = 1f, widthTo = 1f, heightFrom = 1f, heightTo = 1f;
    /** Particle width interpolation. Null to use sizeInterp. */
    public @Nullable Interp widthInterp = null;
    /** Particle height interpolation. Null to use sizeInterp. */
    public @Nullable Interp heightInterp = null;

    //line only
    public boolean line;
    public float strokeFrom = 2f, strokeTo = 0f, lenFrom = 4f, lenTo = 2f;
    public boolean cap = true;

    private @Nullable TextureRegion tex;

    @Override
    public void init(){
        clip = Math.max(clip, length + Math.max(sizeFrom, sizeTo));
        sizeChangeStart = Mathf.clamp(sizeChangeStart, 0f, lifetime);
        if(sizeInterp == null) sizeInterp = interp;
        if(widthInterp == null) widthInterp = sizeInterp;
        if(heightInterp == null) heightInterp = sizeInterp;
        if(colorInterp == null) colorInterp = interp;
    }

    @Override
    public void render(EffectContainer e){
        if(tex == null) tex = Core.atlas.find(region);

        float realRotation = (useRotation ? (casingFlip ? Math.abs(e.rotation) : e.rotation) : baseRotation);
        int flip = casingFlip ? -Mathf.sign(e.rotation) : 1;
        float rawfin = e.fin();
        float fin = e.fin(interp);
        float colFin = e.fin(colorInterp);
        float rad = sizeInterp.apply(sizeFrom, sizeTo, Mathf.curve(rawfin, sizeChangeStart / lifetime, 1f)) * 2;
        float width = rad * (widthInterp.apply(widthFrom, widthTo, Mathf.curve(rawfin, widthChangeStart / lifetime, 1f)) * 2);
        float height = rad * (heightInterp.apply(heightFrom, heightTo, Mathf.curve(rawfin, heightChangeStart / lifetime, 1f)) * 2);
        float ox = e.x + Angles.trnsx(realRotation, offsetX * flip, offsetY), oy = e.y + Angles.trnsy(realRotation, offsetX * flip, offsetY);

        Draw.color(colorFrom, colorTo, colFin);
        Color lightColor = this.lightColor == null ? Draw.getColor() : this.lightColor;

        if(line){
            Lines.stroke(sizeInterp.apply(strokeFrom, strokeTo, rawfin));
            float len = sizeInterp.apply(lenFrom, lenTo, rawfin);

            rand.setSeed(e.id);
            for(int i = 0; i < particles; i++){
                float l = length * fin + baseLength;
                rv.trns(realRotation + rand.range(cone), !randLength ? l : rand.random(l));
                float x = rv.x, y = rv.y;

                Lines.lineAngle(ox + x, oy + y, Mathf.angle(x, y), len, cap);
                Drawf.light(ox + x, oy + y, len * lightScl, lightColor, lightOpacity * Draw.getColorAlpha());
            }
        }else{
            rand.setSeed(e.id);
            for(int i = 0; i < particles; i++){
                float l = length * fin + baseLength;
                rv.trns(realRotation + rand.range(cone), !randLength ? l : rand.random(l));
                float x = rv.x, y = rv.y;

                Draw.rect(tex, ox + x, oy + y, width, height / tex.ratio(), realRotation + offset + e.time * spin);
                Drawf.light(ox + x, oy + y, rad * lightScl, lightColor, lightOpacity * Draw.getColorAlpha());
            }
        }
    }
}
