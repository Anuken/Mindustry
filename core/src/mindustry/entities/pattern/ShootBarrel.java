package mindustry.entities.pattern;

import arc.util.*;

public class ShootBarrel extends ShootPattern{
    /** barrels [in x, y, rotation] format. */
    public float[] barrels = {0f, 0f, 0f};
    /** offset of barrel to start on */
    public int barrelOffset = 0;

    @Override
    public void flip(){
        barrels = barrels.clone();
        for(int i = 0; i < barrels.length; i += 3){
            barrels[i] *= -1;
            barrels[i + 2] *= -1;
        }
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        for(int i = 0; i < shots; i++){
            int index = ((i + totalShots + barrelOffset) % (barrels.length / 3)) * 3;
            handler.shoot(barrels[index], barrels[index + 1], barrels[index + 2], firstShotDelay + shotDelay * i);
            if(barrelIncrementer != null) barrelIncrementer.run();
        }
    }
}
