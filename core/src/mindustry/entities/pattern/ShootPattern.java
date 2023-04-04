package mindustry.entities.pattern;

import arc.util.*;
import mindustry.entities.*;

/** Handles different types of bullet patterns for shooting. */
public class ShootPattern implements Cloneable{
    /** amount of shots per "trigger pull" */
    public int shots = 1;
    /** delay in ticks before first shot */
    public float firstShotDelay = 0;
    /** delay in ticks between shots */
    public float shotDelay = 0;

    /** Called on a single "trigger pull". This function should call the handler with any bullets that result. */
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        for(int i = 0; i < shots; i++){
            handler.shoot(0, 0, 0, firstShotDelay + shotDelay * i);
        }
    }

    /** Called on a single "trigger pull". This function should call the handler with any bullets that result. */
    public void shoot(int totalShots, BulletHandler handler){
        shoot(totalShots, handler, null);
    }

    /** Subclasses should override this to flip its sides. */
    public void flip(){

    }

    public ShootPattern copy(){
        try{
            return (ShootPattern)clone();
        }catch(CloneNotSupportedException absurd){
            throw new RuntimeException("impending doom", absurd);
        }
    }

    public interface BulletHandler{
        /**
         * @param x x offset of bullet, should be transformed by weapon rotation
         * @param y y offset of bullet, should be transformed by weapon rotation
         * @param rotation rotation offset relative to weapon
         * @param delay bullet delay in ticks
         * */
        default void shoot(float x, float y, float rotation, float delay){
            shoot(x, y, rotation, delay, null);
        }

        void shoot(float x, float y, float rotation, float delay, Mover move);
    }
}
