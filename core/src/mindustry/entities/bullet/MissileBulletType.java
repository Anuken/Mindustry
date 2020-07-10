package mindustry.entities.bullet;

import arc.graphics.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class MissileBulletType extends BasicBulletType{
    protected Color trailColor = Pal.missileYellowBack;

    public MissileBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        backColor = Pal.missileYellowBack;
        frontColor = Pal.missileYellow;
        homingPower = 0.08f;
        hitSound = Sounds.explosion;
    }

    public MissileBulletType(){
        this(1f, 1f, "missile");
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(Mathf.chanceDelta(0.2)){
            Fx.missileTrail.at(b.x, b.y, 2f, trailColor);
        }
    }
}
