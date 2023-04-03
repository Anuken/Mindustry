package mindustry.entities.pattern;

import arc.math.*;
import arc.util.*;

public class ShootHelix extends ShootPattern{
    public float scl = 2f, mag = 1.5f, offset = Mathf.PI * 1.25f;

    @Override
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        for(int i = 0; i < shots; i++){
            for(int sign : Mathf.signs){
                handler.shoot(0, 0, 0, firstShotDelay + shotDelay * i,
                    b -> b.moveRelative(0f, Mathf.sin(b.time + offset, scl, mag * sign)));
            }
        }
    }
}
