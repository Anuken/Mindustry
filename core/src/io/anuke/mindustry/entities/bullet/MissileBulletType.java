package io.anuke.mindustry.entities.bullet;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects;

public class MissileBulletType extends BasicBulletType {

    public MissileBulletType(float speed, float damage, String bulletSprite) {
        super(speed, damage, bulletSprite);
        backColor = Palette.missileYellowBack;
        frontColor = Palette.missileYellow;
        homingPower = 6f;
    }

    @Override
    public void update(Bullet b) {
        super.update(b);

        if(b.timer.get(0, 4f)){
            Effects.effect(BulletFx.missileTrail, b.x, b.y, 2f);
        }
    }
}
