package mindustry.entities.bullet;

import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class RailBulletType extends BulletType{
    public Effect pierceEffect = Fx.hitBulletSmall, updateEffect = Fx.none;
    /** Multiplier of damage decreased per health pierced. */
    public float pierceDamageFactor = 1f;

    public float length = 100f;

    public float updateEffectSeg = 20f;

    public RailBulletType(){
        speed = 0f;
        pierceBuilding = true;
        pierce = true;
        reflectable = false;
        hitEffect = Fx.none;
        despawnEffect = Fx.none;
        collides = false;
        lifetime = 1f;
    }

    @Override
    public float range(){
        return length;
    }

    void handle(Bullet b, Posc pos, float initialHealth){
        float sub = Math.max(initialHealth*pierceDamageFactor, 0);

        if(b.damage <= 0){
            b.fdata = Math.min(b.fdata, b.dst(pos));
            return;
        }

        if(b.damage > 0){
            pierceEffect.at(pos.getX(), pos.getY(), b.rotation());

            hitEffect.at(pos.getX(), pos.getY());
        }

        //subtract health from each consecutive pierce
        b.damage -= Math.min(b.damage, sub);
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        b.fdata = length;
        Damage.collideLine(b, b.team, b.type.hitEffect, b.x, b.y, b.rotation(), length, false, false);
        float resultLen = b.fdata;

        Vec2 nor = Tmp.v1.trns(b.rotation(), 1f).nor();
        for(float i = 0; i <= resultLen; i += updateEffectSeg){
            updateEffect.at(b.x + nor.x * i, b.y + nor.y * i, b.rotation());
        }
    }

    @Override
    public boolean testCollision(Bullet bullet, Building tile){
        return bullet.team != tile.team;
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float health){
        super.hitEntity(b, entity, health);
        handle(b, entity, health);
    }

    @Override
    public void hitTile(Bullet b, Building build, float initialHealth, boolean direct){
        handle(b, build, initialHealth);
    }
}
