package mindustry.entities.effect;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;

/** Effect that renders a basic shockwave. */
public class WaveEffect extends Effect{
    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    public float sizeFrom = 0f, sizeTo = 100f;
    public int sides = -1;
    public float rotation = 0f;
    public float strokeFrom = 2f, strokeTo = 0f;
    public Interp interp = Interp.linear;

    @Override
    public void init(){
        clip = Math.max(clip, Math.max(sizeFrom, sizeTo) + Math.max(strokeFrom, strokeTo));
    }

    @Override
    public void render(EffectContainer e){
        float fin = e.fin();
        float ifin = e.fin(interp);

        Draw.color(colorFrom, colorTo, ifin);
        Lines.stroke(interp.apply(strokeFrom, strokeTo, fin));

        float rad = interp.apply(sizeFrom, sizeTo, fin);
        Lines.poly(e.x, e.y, sides <= 0 ? Lines.circleVertices(rad) : sides, rad, rotation + e.rotation);
    }
}
