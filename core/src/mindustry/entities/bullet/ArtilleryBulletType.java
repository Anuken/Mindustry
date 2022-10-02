package mindustry.entities.bullet;

import arc.math.*;
import mindustry.content.*;
import mindustry.gen.*;

public class ArtilleryBulletType extends BasicBulletType{
    public float trailMult = 1f, trailSize = 4f;

    public ArtilleryBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        collidesTiles = false;
        collides = false;
        collidesAir = false;
        scaleLife = true;
        hitShake = 1f;
        hitSound = Sounds.explosion;
        hitEffect = Fx.flakExplosion;
        shootEffect = Fx.shootBig;
        trailEffect = Fx.artilleryTrail;

        //default settings:
        shrinkX = 0.15f;
        shrinkY = 0.63f;
        shrinkInterp = Interp.slope;

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
}
