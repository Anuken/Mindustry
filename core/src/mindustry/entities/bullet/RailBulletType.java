package mindustry.entities.bullet;

import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class RailBulletType extends BulletType{
    public Effect pierceEffect = Fx.hitBulletSmall, pointEffect = Fx.none, lineEffect = Fx.none;
    public Effect endEffect = Fx.none;

    public float length = 100f;

    public float pointEffectSpace = 20f;

    public RailBulletType(){
        speed = 0f;
        pierceBuilding = true;
        pierce = true;
        reflectable = false;
        hitEffect = Fx.none;
        despawnEffect = Fx.none;
        collides = false;
        keepVelocity = false;
        lifetime = 1f;
        delayFrags = true;
    }

    @Override
    protected float calculateRange(){
        return length;
    }

    @Override
    public void handlePierce(Bullet b, float initialHealth, float x, float y){
        float sub = Math.max(initialHealth * pierceDamageFactor, 0);

        if(b.damage <= 0){
            b.fdata = Math.min(b.fdata, b.dst(x, y));
            return;
        }

        if(b.damage > 0){
            pierceEffect.at(x, y, b.rotation());
        }

        //subtract health from each consecutive pierce
        b.damage -= Math.min(b.damage, sub);

        //bullet was stopped, decrease furthest distance
        if(b.damage <= 0f){
            b.fdata = Math.min(b.fdata, b.dst(x, y));
        }
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        b.fdata = length;
        Damage.collideLine(b, b.team, b.x, b.y, b.rotation(), length, false, false, pierceCap);
        float resultLen = b.fdata;

        Vec2 nor = Tmp.v1.trns(b.rotation(), 1f).nor();
        if(pointEffect != Fx.none){
            for(float i = 0; i <= resultLen; i += pointEffectSpace){
                pointEffect.at(b.x + nor.x * i, b.y + nor.y * i, b.rotation(), trailColor);
            }
        }

        boolean any = b.collided.size > 0;

        if(!any && endEffect != Fx.none){
            endEffect.at(b.x + nor.x * resultLen, b.y + nor.y * resultLen, b.rotation(), hitColor);
        }

        if(lineEffect != Fx.none){
            lineEffect.at(b.x, b.y, b.rotation(), hitColor, new Vec2(b.x, b.y).mulAdd(nor, resultLen));
        }
    }

    @Override
    public boolean testCollision(Bullet bullet, Building tile){
        return bullet.team != tile.team;
    }

    @Override
    public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        handlePierce(b, initialHealth, x, y);
    }
}
