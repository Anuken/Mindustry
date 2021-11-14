package mindustry.entities.bullet;

import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.gen.*;

//TODO scale velocity depending on fslope()
public class ArtilleryBulletType extends BasicBulletType{
    public float trailMult = 1f, trailSize = 4f;

    public ArtilleryBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        collidesTiles = false;
        collides = false;
        collidesAir = false;
        scaleVelocity = true;
        hitShake = 1f;
        hitSound = Sounds.explosion;
        shootEffect = Fx.shootBig;
        trailEffect = Fx.artilleryTrail;

        //default settings:
        shrinkX = 0.15f;
        shrinkY = 0.63f;

        //for trail:

        /*
        trailLength = 27;
        trailWidth = 3.5f;
        trailEffect = Fx.none;
        trailColor = Pal.bulletYellowBack;

        trailInterp = Interp.slope;

        shrinkX = 0.8f;
        shrinkY = 0.3f;
        */
    }

    public ArtilleryBulletType(float speed, float damage){
        this(speed, damage, "shell");
    }

    public ArtilleryBulletType(){
        this(1f, 1f, "shell");
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(b.timer(0, (3 + b.fslope() * 2f) * trailMult)){
            trailEffect.at(b.x, b.y, b.fslope() * trailSize, backColor);
        }
    }

    @Override
    public void draw(Bullet b){
        drawTrail(b);
        float xscale = (1f - shrinkX + b.fslope() * (shrinkX)), yscale = (1f - shrinkY + b.fslope() * (shrinkY)), rot = b.rotation();

        Draw.color(backColor);
        Draw.rect(backRegion, b.x, b.y, width * xscale, height * yscale, rot - 90);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x, b.y, width * xscale, height * yscale, rot - 90);
        Draw.color();
    }
}
