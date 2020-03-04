package mindustry.entities.bullet;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
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
    public void update(Bulletc b){
        super.update(b);

        if(Mathf.chance(Time.delta() * 0.2)){
            Fx.missileTrail.at(b.x(), b.y(), 2f, trailColor);
        }

        if(weaveMag > 0){
            b.vel().rotate(Mathf.sin(Time.time() + b.id() * 442, weaveScale, weaveMag) * Time.delta());
        }
    }
}
