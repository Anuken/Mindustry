package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class MissileBulletType extends BasicBulletType{
    protected Color trailColor = Palette.missileYellowBack;

    public MissileBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        backColor = Palette.missileYellowBack;
        frontColor = Palette.missileYellow;
        homingPower = 7f;
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(Mathf.chance(Timers.delta() * 0.2)){
            Effects.effect(BulletFx.missileTrail, trailColor, b.x, b.y, 2f);
        }
    }
}
