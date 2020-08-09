package mindustry.entities.bullet;

import mindustry.gen.*;
import mindustry.graphics.*;

public class MissileBulletType extends BasicBulletType{

    public MissileBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
        backColor = Pal.missileYellowBack;
        frontColor = Pal.missileYellow;
        homingPower = 0.08f;
        shrinkY = 0f;
        width = 8f;
        height = 8f;
        hitSound = Sounds.explosion;
        trailChance = 0.2f;
    }

    public MissileBulletType(float speed, float damage){
        this(speed, damage, "missile");
    }

    public MissileBulletType(){
        this(1f, 1f, "missile");
    }
}
