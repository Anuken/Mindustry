package io.anuke.mindustry.entities.bullet;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.arc.entities.Effects;
import io.anuke.arc.util.Time;
import io.anuke.arc.math.Mathf;

public class MissileBulletType extends BasicBulletType{
    protected Color trailColor = Palette.missileYellowBack;

    protected float weaveScale = 0f;
    protected float weaveMag = -1f;

    public MissileBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        backColor = Palette.missileYellowBack;
        frontColor = Palette.missileYellow;
        homingPower = 7f;
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(Mathf.chance(Time.delta() * 0.2)){
            Effects.effect(Fx.missileTrail, trailColor, b.x, b.y, 2f);
        }

        if(weaveMag > 0){
            b.velocity().rotate(Mathf.sin(Time.time() + b.id * 4422, weaveScale, weaveMag));
        }
    }
}
