package mindustry.entities.pattern;

import arc.math.*;

public class SinePattern extends ShotPattern{
    /** scaling applied to bullet index */
    public float scl = 4f;
    /** magnitude of sine curve for position displacement */
    public float mag = 20f;

    public SinePattern(float scl, float mag){
        this.scl = scl;
        this.mag = mag;
    }

    public SinePattern(){
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler){
        for(int i = 0; i < shots; i++){
            float angleOffset = Mathf.sin(i + totalShots, scl, mag);
            handler.shoot(0, 0, angleOffset, firstShotDelay + shotDelay * i);
        }
    }
}
