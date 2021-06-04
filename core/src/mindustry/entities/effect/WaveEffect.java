package mindustry.entities.effect;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/** Effect that renders a basic shockwave. */
public class WaveEffect extends Effect{
    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    public @Nullable Color lightColor;
    public float sizeFrom = 0f, sizeTo = 100f, lightScl = 3f, lightOpacity = 0.8f;
    public int sides = -1;
    public float rotation = 0f;
    public float strokeFrom = 2f, strokeTo = 0f;
    public Interp interp = Interp.linear;
    public Interp lightInterp = Interp.reverse;
    public float offsetX, offsetY;

    @Override
    public void init(){
        clip = Math.max(clip, Math.max(sizeFrom, sizeTo) + Math.max(strokeFrom, strokeTo));
    }

    @Override
    public void render(EffectContainer e){
        float fin = e.fin();
        float ifin = e.fin(interp);
        float ox = e.x + Angles.trnsx(e.rotation, offsetX, offsetY), oy = e.y + Angles.trnsy(e.rotation, offsetX, offsetY);

        Draw.color(colorFrom, colorTo, ifin);
        Lines.stroke(interp.apply(strokeFrom, strokeTo, fin));

        float rad = interp.apply(sizeFrom, sizeTo, fin);
        Lines.poly(ox, oy, sides <= 0 ? Lines.circleVertices(rad) : sides, rad, rotation + e.rotation);

        Drawf.light(ox, oy, rad * lightScl, lightColor == null ? Draw.getColor() : lightColor, lightOpacity * e.fin(lightInterp));
    }
}
