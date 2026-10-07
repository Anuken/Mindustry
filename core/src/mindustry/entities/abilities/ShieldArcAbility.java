package mindustry.entities.abilities;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/** Projects a polygonal shield */
public class ShieldArcAbility extends BaseShieldAbility{
    /** Angle of shield arc. */
    public float angle = 80f;
    /** Width of shield line. */
    public float width = 6f;

    /** Whether to draw the arc line. */
    public boolean drawArc = true;
    /** If not null, will be drawn on top. */
    public @Nullable String region;
    /** If true, sprite position will be influenced by x/y. */
    public boolean offsetRegion = false;

    public ShieldArcAbility(){
        unitShield = false; //arc does not protect the unit itself
    }

    @Override
    public float shieldBounds(){
        return radius + width;
    }

    @Override
    public boolean isBulletInside(Bullet b){
        return isPosInside(b, b.deltaX, b.deltaY);
    }

    @Override
    public boolean isUnitInside(Unit unit){
        return isPosInside(unit, unit.deltaX, unit.deltaY);
    }

    public boolean isPosInside(Posc p, float dx, float dy){
        return !(p.within(paramPos, radius - width) && paramPos.within(p.getX() - dx, p.getY() - dy, radius - width)) &&
        (Tmp.v1.set(p).add(dx, dy).within(paramPos, radius + width) || p.within(paramPos, radius + width)) &&
        (Angles.within(paramPos.angleTo(p), paramUnit.rotation + angleOffset, angle / 2f) || Angles.within(paramPos.angleTo(p.getX() + dx, p.getY() + dy), paramUnit.rotation + angleOffset, angle / 2f));
    }

    @Override
    public @Nullable Vec2 intersectLaser(Unit unit, float x1, float y1, float x2, float y2, float damage){
        if(!active(unit)) return null;

        Tmp.v1.set(x, y).rotate(unit.rotation - 90f).add(unit);
        float cx = Tmp.v1.x, cy = Tmp.v1.y;
        float rot = unit.rotation + angleOffset, half = angle / 2f;
        float inner = Math.max(radius - width, 0f), outer = radius + width;
        float dx = x2 - x1, dy = y2 - y1, a = dx * dx + dy * dy;
        float fx = x1 - cx, fy = y1 - cy, start = fx * fx + fy * fy;

        if(inBand(cx, cy, x1, y1, rot)){
            return laserHit.set(x1, y1);
        }

        float best = Float.MAX_VALUE;

        //crossings of the outer and inner arcs, as fractions along the segment
        if(a > 0f){
            float b = 2f * (fx * dx + fy * dy);
            for(int i = 0; i < 2; i++){
                float r = i == 0 ? outer : inner;
                float disc = b * b - 4f * a * (start - r * r);
                if(disc < 0f) continue;

                float sqrt = Mathf.sqrt(disc);
                for(int s : Mathf.signs){
                    float t = (-b + sqrt * s) / (2f * a);
                    if(t >= 0f && t <= 1f && t < best && inSpan(cx, cy, x1 + dx * t, y1 + dy * t, rot, half)){
                        best = t;
                    }
                }
            }
        }

        //crossings of the radial end edges
        if(angle < 360f){
            for(int s : Mathf.signs){
                float ex = Angles.trnsx(rot + half * s, 1f), ey = Angles.trnsy(rot + half * s, 1f);
                if(Intersector.intersectSegments(x1, y1, x2, y2, cx + ex * inner, cy + ey * inner, cx + ex * outer, cy + ey * outer, Tmp.v2)){
                    best = Math.min(best, Tmp.v2.dst(x1, y1) / Mathf.sqrt(a));
                }
            }
        }

        return best == Float.MAX_VALUE ? null : laserHit.set(x1 + dx * best, y1 + dy * best);
    }

    @Override
    public float absorbExplosion(Unit unit, float ex, float ey, float damage){
        if(!active(unit)) return 0f;

        Tmp.v1.set(x, y).rotate(unit.rotation - 90f).add(unit);
        if(!inBand(Tmp.v1.x, Tmp.v1.y, ex, ey, unit.rotation + angleOffset)) return 0f;

        return absorb(unit, ex, ey, damage, false);
    }

    public boolean inBand(float cx, float cy, float px, float py, float rotation){
        float dst2 = Mathf.dst2(cx, cy, px, py), inner = Math.max(radius - width, 0f), outer = radius + width;
        return dst2 >= inner * inner && dst2 <= outer * outer && inSpan(cx, cy, px, py, rotation, angle / 2f);
    }

    public boolean inSpan(float cx, float cy, float px, float py, float rotation, float half){
        return angle >= 360f || Angles.within(Angles.angle(cx, cy, px, py), rotation, half);
    }

    @Override
    public void draw(Unit unit){
        checkRadius();

        if(getShield(unit) > 0 || widthScale > 0.001f){
            Draw.z(Layer.shields);
            Draw.color(shieldColor(unit), Color.white, Mathf.clamp(alpha));
            Vec2 pos = shieldPos(unit, paramPos);

            if(!Vars.renderer.animateSurfaces && (region != null || drawArc)){
                Draw.alpha(0.4f);
            }

            if(region != null){
                Vec2 rp = offsetRegion ? pos : Tmp.v1.set(unit);
                Draw.yscl = widthScale;
                Draw.rect(region, rp.x, rp.y, unit.rotation - 90);
                Draw.yscl = 1f;
            }

            if(drawArc){
                Lines.stroke(width * widthScale);
                Lines.arc(pos.x, pos.y, radius, angle / 360f, unit.rotation + angleOffset - angle / 2f);
            }
            Draw.reset();
        }
    }
}