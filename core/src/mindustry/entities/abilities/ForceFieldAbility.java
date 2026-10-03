package mindustry.entities.abilities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

public class ForceFieldAbility extends BaseShieldAbility{
    /** Sides of shield polygon. */
    public int sides = 6;
    /** Rotation of shield. */
    public float rotation = 0f;
    /** Whether the shield should follow the unit s rotation. */
    public boolean followUnitRot = false;

    public ForceFieldAbility(){
    }

    public ForceFieldAbility(float radius, float regen, float max, float cooldown){
        this.radius = radius;
        this.regen = regen;
        this.max = max;
        this.cooldown = cooldown;
    }

    public ForceFieldAbility(float radius, float regen, float max, float cooldown, int sides, float rotation){
        this.radius = radius;
        this.regen = regen;
        this.max = max;
        this.cooldown = cooldown;
        this.sides = sides;
        this.rotation = rotation;
    }

    @Override
    public float shieldBounds(){
        return realRad > 0f ? realRad : radius;
    }

    @Override
    public boolean isBulletInside(Bullet b){
        Vec2 pos = shieldPos(paramUnit, paramPos);
        return Intersector.isInRegularPolygon(sides, pos.x, pos.y, realRad, rotation + angleOffset + (followUnitRot ? paramUnit.rotation : 0f), b.x(), b.y());
    }

    @Override
    public boolean isUnitInside(Unit unit){
        Vec2 pos = shieldPos(paramUnit, paramPos);
        return Intersector.isInRegularPolygon(sides, pos.x, pos.y, realRad, rotation + angleOffset + (followUnitRot ? paramUnit.rotation : 0f), unit.x(), unit.y());
    }

    @Override
    public @Nullable Vec2 intersectLaser(Unit unit, float x1, float y1, float x2, float y2, float damage){
        if(!active(unit)) return null;
        Vec2 pos = shieldPos(unit, paramPos);
        return Damage.raycastRegularPolygon(sides, pos.x, pos.y, radiusScale * radius, rotation + angleOffset + (followUnitRot ? unit.rotation : 0f), x1, y1, x2, y2);
    }

    @Override
    public float absorbExplosion(Unit unit, float ex, float ey, float damage){
        if(!active(unit)) return 0f;
        Vec2 pos = shieldPos(unit, paramPos);
        if(!Intersector.isInRegularPolygon(sides, pos.x, pos.y, radiusScale * radius, rotation + angleOffset + (followUnitRot ? unit.rotation : 0f), ex, ey)) return 0f;
        return absorb(unit, ex, ey, damage, false);
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(radius / tilesize, 2)));
        t.row();
    }

    public float shieldRot(){
        return rotation + angleOffset + (followUnitRot ? paramUnit.rotation : 0f);
    }

    @Override
    public void draw(Unit unit){
        checkRadius();

        if(getShield(unit) > 0 || widthScale > 0.001f){
            Draw.z(Layer.shields);
            Draw.color(shieldColor(unit), Color.white, Mathf.clamp(alpha));
            Vec2 pos = shieldPos(unit, paramPos);

            if(renderer.animateSurfaces){
                Draw.z(Layer.shields + 0.001f * alpha);
                Fill.poly(pos.x, pos.y, sides, realRad, shieldRot());
            }else{
                Draw.z(Layer.shields);
                Lines.stroke(1.5f);
                Draw.alpha(0.09f);
                Fill.poly(pos.x, pos.y, sides, radius, shieldRot());
                Draw.alpha(1f);
                Lines.poly(pos.x, pos.y, sides, radius, shieldRot());
            }
            Draw.reset();
        }
    }
}