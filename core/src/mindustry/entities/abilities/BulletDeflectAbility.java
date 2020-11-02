package mindustry.entities.abilities;

import arc.util.*;
import arc.func.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class BulletDeflectAbility extends Ability{
    public float chanceDeflect = 10f, reload = 100, range = 60f;
    public Effect activeEffect = Fx.deflectWaveDynamic;
    public Effect applyEffect = Fx.bulletDeflect;

    protected float timer;
    private static Unit paramUnit;
    private static BulletDeflectAbility paramField;
    private static final Cons<Bullet> bulletDeflector = bullet -> {
        if(bullet.team != paramUnit.team && !(bullet.vel().len() <= 0.1f || !bullet.type.reflectable) && Mathf.chance(paramField.chanceDeflect / bullet.damage())) {
            bullet.trns(-bullet.vel.x, -bullet.vel.y);

            float angleToUnit = Mathf.angle(bullet.x - paramUnit.x, bullet.y - paramUnit.y) + 90f;
            float newAngle = 2 * (angleToUnit - bullet.vel.angle());

            bullet.vel.trns(newAngle, bullet.vel.x, bullet.vel.y);

            bullet.owner(paramUnit);
            bullet.team(paramUnit.team);
            bullet.time(bullet.time() + 1f);

            paramField.applyEffect.at(bullet.x, bullet.y, paramField.range);
        }
    };

    BulletDeflectAbility(){}

    public BulletDeflectAbility(float chanceDeflect, float reload, float range){
        this.chanceDeflect = chanceDeflect;
        this.range = range;
        this.reload = reload;
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;

        if(timer >= reload){
            Bullet nearestBullet = Groups.bullet.intersect(unit.x - range, unit.y - range, range*2, range*2).min(b -> b.team == unit.team || !b.type().hittable ? Float.MAX_VALUE : b.dst2(unit.x, unit.y));

            if(nearestBullet != null && nearestBullet.team != unit.team) {
                activeEffect.at(unit, range);

                paramUnit = unit;
                paramField = this;
                Groups.bullet.intersect(unit.x - range, unit.y - range, range * 2f, range * 2f, bulletDeflector);
            }

            timer = 0f;
        }
    }
}