package mindustry.entities.bullet;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effects;
import mindustry.entities.type.Bullet;
import mindustry.gen.*;
import mindustry.graphics.Pal;

public class MissileBulletType extends BasicBulletType{
    protected Color trailColor = Pal.missileYellowBack;

    protected float weaveScale = 0f;
    protected float weaveMag = -1f;

    public MissileBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        backColor = Pal.missileYellowBack;
        frontColor = Pal.missileYellow;
        homingPower = 7f;
        hitSound = Sounds.explosion;
    }

    public MissileBulletType(){
        this(1f, 1f, "missile");
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(Mathf.chance(Time.delta() * 0.2)){
            Effects.effect(Fx.missileTrail, trailColor, b.x, b.y, 2f);
        }

        if(weaveMag > 0){
            b.velocity().rotate(Mathf.sin(Time.time() + b.id * 4422, weaveScale, weaveMag) * Time.delta());
        }
    }
}
