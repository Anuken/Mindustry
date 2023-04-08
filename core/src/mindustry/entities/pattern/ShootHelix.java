package mindustry.entities.pattern;

import arc.math.*;
import arc.util.*;

public class ShootHelix extends ShootPattern{
    public float scl = 2f, mag = 1.5f, offset = Mathf.PI;

    public ShootHelix(){
        shots = 2;
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        for(int i = 0; i < shots; i++){
            float off = offset + i * Mathf.PI2 * scl / shots;
            handler.shoot(Mathf.cos(off, scl, scl * mag), 0, 0, firstShotDelay + shotDelay * i,
                b -> b.moveRelative(0f, Mathf.sin(b.time + off, scl, mag)));
        }
    }
}
