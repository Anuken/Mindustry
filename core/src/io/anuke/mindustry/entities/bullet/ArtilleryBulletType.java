package io.anuke.mindustry.entities.bullet;

import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.graphics.Draw;

//TODO scale velocity depending on fslope()
public class ArtilleryBulletType extends BasicBulletType {

    public ArtilleryBulletType(float speed, float damage, String bulletSprite) {
        super(speed, damage, bulletSprite);
        collidesTiles = false;
        collides = false;
    }

    @Override
    public void update(Bullet b) {
        if(b.timer.get(0, 3 + b.fslope()*2f)){
            Effects.effect(BulletFx.artilleryTrail, b.x, b.y, b.fslope() * 4f);
        }
    }

    @Override
    public void draw(Bullet b) {
        float baseScale = 0.7f;
        float scale = (baseScale + b.fslope()*(1f-baseScale));

        float height = bulletHeight * ((1f - bulletShrink) + bulletShrink * b.fout());

        Draw.color(backColor);
        Draw.rect(backRegion, b.x, b.y, bulletWidth * scale, height * scale, b.angle() - 90);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x, b.y, bulletWidth * scale, height * scale, b.angle() - 90);
        Draw.color();
    }
}
