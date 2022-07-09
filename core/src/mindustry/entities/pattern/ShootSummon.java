package mindustry.entities.pattern;

import arc.math.*;
import arc.util.*;

public class ShootSummon extends ShootPattern{
    public float x, y, radius, spread;

    public ShootSummon(float x, float y, float radius, float spread){
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.spread = spread;
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler){


        for(int i = 0; i < shots; i++){
            Tmp.v1.trns(Mathf.random(360f), Mathf.random(radius));

            handler.shoot(x + Tmp.v1.x, y + Tmp.v1.y, Mathf.range(spread), firstShotDelay + shotDelay * i);
        }
    }
}
