package mindustry.entities.bullet;

import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

//TODO scale velocity depending on fslope()
public class ArtilleryBulletType extends BasicBulletType{
    protected Effect trailEffect = Fx.artilleryTrail;

    public ArtilleryBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        collidesTiles = false;
        collides = false;
        collidesAir = false;
        hitShake = 1f;
        hitSound = Sounds.explosion;
    }

    public ArtilleryBulletType(){
        this(1f, 1f, "shell");
    }

    @Override
    public void update(Bulletc b){
        super.update(b);

        if(b.timer(0, 3 + b.fslope() * 2f)){
            trailEffect.at(b.x(), b.y(), b.fslope() * 4f, backColor);
        }
    }

    @Override
    public void draw(Bulletc b){
        float baseScale = 0.7f;
        float scale = (baseScale + b.fslope() * (1f - baseScale));

        float height = bulletHeight * ((1f - bulletShrink) + bulletShrink * b.fout());

        Draw.color(backColor);
        Draw.rect(backRegion, b.x(), b.y(), bulletWidth * scale, height * scale, b.rotation() - 90);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x(), b.y(), bulletWidth * scale, height * scale, b.rotation() - 90);
        Draw.color();
    }
}
