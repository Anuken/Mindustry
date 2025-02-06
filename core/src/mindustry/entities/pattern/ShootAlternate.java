package mindustry.entities.pattern;

import arc.util.*;

public class ShootAlternate extends ShootPattern{
    /** number of barrels used for shooting. */
    public int barrels = 2;
    /** spread between barrels, in world units - not degrees. */
    public float spread = 5f;
    /** offset of barrel to start on */
    public int barrelOffset = 0;

    public ShootAlternate(float spread){
        this.spread = spread;
    }

    public ShootAlternate(){
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        for(int i = 0; i < shots; i++){
            float index = ((totalShots + i + barrelOffset) % barrels) - (barrels-1)/2f;
            handler.shoot(index * spread, 0, 0f, firstShotDelay + shotDelay * i);
            if(barrelIncrementer != null) barrelIncrementer.run();
        }
    }
}
