package mindustry.entities.effect;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;

/** The most essential effect class. Can create particles in various shapes. */
public class ParticleEffect extends Effect{
    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    public int particles = 6;
    public float cone = 180f, length = 20f, baseLength = 0f;
    public Interp interp = Interp.linear;

    //region only
    public float sizeFrom = 2f, sizeTo = 0f;
    public float offset = 0;
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

        Draw.color(colorFrom, colorTo, fin);

        if(line){
            Lines.stroke(interp.apply(strokeFrom, strokeTo, rawfin));
            float len = interp.apply(lenFrom, lenTo, rawfin);

            Angles.randLenVectors(e.id, particles, length * fin + baseLength, e.rotation, cone, (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), len);
            });
        }else{
            Angles.randLenVectors(e.id, particles, length * fin + baseLength, e.rotation, cone, (x, y) -> {
                Draw.rect(tex, e.x + x, e.y + y, rad, rad, e.rotation + offset);
            });
        }
    }
}
