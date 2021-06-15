package mindustry.entities.effect;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/** The most essential effect class. Can create particles in various shapes. */
public class ParticleEffect extends Effect{
    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    public int particles = 6;
    public float cone = 180f, length = 20f, baseLength = 0f;
    /** Particle size/length/radius interpolation. */
    public Interp interp = Interp.linear;
    public float offsetX, offsetY;
    public float lightScl = 2f, lightOpacity = 0.6f;
    public @Nullable Color lightColor;

    //region only

    /** Spin in degrees per tick. */
    public float spin = 0f;
    /** Controls the initial and final sprite sizes. */
    public float sizeFrom = 2f, sizeTo = 0f;
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
    }

    @Override
    public void render(EffectContainer e){
        if(tex == null) tex = Core.atlas.find(region);

        float rawfin = e.fin();
        float fin = e.fin(interp);
        float rad = interp.apply(sizeFrom, sizeTo, rawfin) * 2;
        float ox = e.x + Angles.trnsx(e.rotation, offsetX, offsetY), oy = e.y + Angles.trnsy(e.rotation, offsetX, offsetY);

        Draw.color(colorFrom, colorTo, fin);
        Color lightColor = this.lightColor == null ? Draw.getColor() : this.lightColor;

        if(line){
            Lines.stroke(interp.apply(strokeFrom, strokeTo, rawfin));
            float len = interp.apply(lenFrom, lenTo, rawfin);

            Angles.randLenVectors(e.id, particles, length * fin + baseLength, e.rotation, cone, (x, y) -> {
                Lines.lineAngle(ox + x, oy + y, Mathf.angle(x, y), len);
                Drawf.light(ox + x, oy + y, len * lightScl, lightColor, lightOpacity* Draw.getColor().a);
            });
        }else{
            Angles.randLenVectors(e.id, particles, length * fin + baseLength, e.rotation, cone, (x, y) -> {
                Draw.rect(tex, ox + x, oy + y, rad, rad, e.rotation + offset + e.time * spin);
                Drawf.light(ox + x, oy + y, rad * lightScl, lightColor, lightOpacity * Draw.getColor().a);
            });
        }
    }
}
