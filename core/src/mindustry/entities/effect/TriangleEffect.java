package mindustry.entities.effect;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/** Creates a triangle with heavily customizable parameters. */
public class TriangleEffect extends Effect{
    private static final Rand rand = new Rand();
    private static final int spinI

    /** Triangle color. */
    public Color colorFrom = Color.white.cpy(), colorTo = Color.white.cpy();
    /** Gives the effect flipping compatability like casing effects. */
    public boolean flippable;
    /** Triangle position interpolation. Also used as a fallback.*/
    public Interp interp = Interp.linear;
    /** Width interpolation. Null to use interp. */
    public @Nullable Interp widthInterp = null;
    /** Height interpolation. Null to use interp. */
    public @Nullable Interp heightInterp = null;
    /** Triangle color interpolation. Null to use interp. */
    public @Nullable Interp colorInterp = null;
    /** Controls the starting and ending positions of the triangle. */
    public float startX = 0, startY = 0, endX = 0, endY = 0;
    /** Triangle light properties. */
    public float lightScl = 8f, lightOpacityFrom = 0.6f, lightOpacityTo = 0f;
    /** Color of the light the triangle emits. */
    public @Nullable Color lightColor;

    /** Controls the initial and final positions of each point on the triangle. */
    public float widthFrom = 4f, widthTo = 0f, heightFrom = 4f, heightTo = 4f;
    /** Whether the rotation adds with the parent */
    public boolean useRotation = true;
    /** Rotation offset. */
    public float offset = 0;

    @Override
    public void init(){
        if(widthInterp == null) widthInterp = interp;
        if(heightInterp == null) heightInterp = interp;
        if(colorInterp == null) colorInterp = interp
    }

    @Override
    public void render(EffectContainer e){
        float realRotation = (useRotation ? (flippable ? Math.abs(e.rotation) : e.rotation) : baseRotation);
        int flip = flippable ? -Mathf.sign(e.rotation) : 1;
        float rawfin = e.fin();
        float colFin = e.fin(interp);
        float width = widthInterp.apply(widthFrom, widthTo, Mathf.curve(rawfin, 0, 1f));
        float height = heightInterp.apply(heightFrom, heightTo, Mathf.curve(rawfin, 0, 1f));
        float lightOpac = colorInterp.apply(lightOpacityFrom, lightOpacityTo, Mathf.curve(rawfin, 0, 1f));

        float cx = interp.apply(startX, endX, Mathf.curve(rawfin, 0, 1f));
        float cy = interp.apply(startY, endY, Mathf.curve(rawfin, 0, 1f));
        Vec2 pos = new Vec2(cx, cy);
        pos.rotate(realRotation);

        Draw.color(colorFrom, colorTo, colFin);
        Color lightColor = this.lightColor == null ? Draw.getColor() : this.lightColor;
        
        Drawf.tri(pos.x + e.x, pos.y + e.y, width, height, realRotation + offset + e.time * spin);
        Drawf.light(pos.x + e.x, pos.y + e.y, lightScl, lightColor, lightOpac * Draw.getColorAlpha());
    }
}
