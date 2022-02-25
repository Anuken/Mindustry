package mindustry.entities.pattern;

/** Handles different types of bullet patterns for shooting. */
public class ShotPattern{
    /** amount of shots per "trigger pull" */
    public int shots = 1;
    /** delay in ticks before first shot */
    public float firstShotDelay = 0;
    /** delay in ticks between shots */
    public float shotDelay = 0;

    /** Called on a single "trigger pull". This function should call the handler with any bullets that result. */
    public void shoot(int totalShots, BulletHandler handler){
        for(int i = 0; i < shots; i++){
            handler.shoot(0, 0, 0, firstShotDelay + shotDelay * i);
        }
    }

    public interface BulletHandler{
        /**
         * @param x x offset of bullet, should be transformed by weapon rotation
         * @param y y offset of bullet, should be transformed by weapon rotation
         * @param rotation rotation offset relative to weapon
         * @param delay bullet delay in ticks
         * */
        void shoot(float x, float y, float rotation, float delay);
    }
}
