package mindustry.entities.pattern;

import arc.math.*;

public class ShootHelix extends ShootPattern{
    //TODO: pattern is broken without a proper offset
    public float scl = 2f, mag = 0.5f, offset = Mathf.PI * 1.25f;

    @Override
    public void shoot(int totalShots, BulletHandler handler){
        for(int i = 0; i < shots; i++){
            for(int sign : Mathf.signs){
                handler.shoot(0, 0, 0, firstShotDelay + shotDelay * i, b -> Mathf.sin(b.time + offset, scl, mag * sign));
            }
        }
    }
}
