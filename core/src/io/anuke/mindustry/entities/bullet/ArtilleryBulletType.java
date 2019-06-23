package io.anuke.mindustry.entities.bullet;

import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;

//TODO scale velocity depending on fslope()
public class ArtilleryBulletType extends BasicBulletType{
    protected Effect trailEffect = Fx.artilleryTrail;

    public ArtilleryBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        collidesTiles = false;
        collides = false;
        collidesAir = false;
        hitShake = 1f;
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(b.timer.get(0, 3 + b.fslope() * 2f)){
            Effects.effect(trailEffect, backColor, b.x, b.y, b.fslope() * 4f);
        }
    }

    @Override
    public void draw(Bullet b){
        float baseScale = 0.7f;
        float scale = (baseScale + b.fslope() * (1f - baseScale));

        float height = bulletHeight * ((1f - bulletShrink) + bulletShrink * b.fout());

        Draw.color(backColor);
        Draw.rect(backRegion, b.x, b.y, bulletWidth * scale, height * scale, b.rot() - 90);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x, b.y, bulletWidth * scale, height * scale, b.rot() - 90);
        Draw.color();
    }
}
