package mindustry.entities.bullet;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/** A continuous bullet type that only damages in a point. */
public class PointLaserBulletType extends BulletType{
    public String sprite = "point-laser";
    public TextureRegion laser, laserEnd;

    public Color color = Color.white;

    public Effect beamEffect = Fx.colorTrail;
    public float beamEffectInterval = 3f, beamEffectSize = 3.5f;

    public float oscScl = 2f, oscMag = 0.3f;
    public float damageInterval = 5f;

    public float shake = 0f;

    public PointLaserBulletType(){
        removeAfterPierce = false;
        speed = 0f;
        despawnEffect = Fx.none;
        shootEffect = Fx.none;
        lifetime = 20f;
        impact = true;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
        optimalLifeFract = 0.5f;

        //just make it massive, users of this bullet can adjust as necessary
        drawSize = 1000f;
    }

    @Override
    public float estimateDPS(){
        return damage * 100f / damageInterval * 3f;
    }

    @Override
    public void load(){
        super.load();

        laser = Core.atlas.find(sprite);
        laserEnd = Core.atlas.find(sprite + "-end");
    }

    @Override
    public void draw(Bullet b){
        super.draw(b);

        Draw.color(color);
        Drawf.laser(laser, laserEnd, b.x, b.y, b.aimX, b.aimY, b.fslope() * (1f - oscMag + Mathf.absin(Time.time, oscScl, oscMag)));

        Draw.reset();
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(b.timer.get(0, damageInterval)){
            Damage.collidePoint(b, b.team, hitEffect, b.aimX, b.aimY);
        }

        if(b.timer.get(1, beamEffectInterval)){
            beamEffect.at(b.aimX, b.aimY, beamEffectSize * b.fslope(), hitColor);
        }

        if(shake > 0){
            Effect.shake(shake, shake, b);
        }
    }
}
