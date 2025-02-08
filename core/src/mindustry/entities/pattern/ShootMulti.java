package mindustry.entities.pattern;

import arc.util.*;

public class ShootMulti extends ShootPattern{
    public ShootPattern source;
    public ShootPattern[] dest = {};

    public ShootMulti(ShootPattern source, ShootPattern... dest){
        this.source = source;
        this.dest = dest;
    }

    public ShootMulti(){
    }

    //deep copy needed for flips
    @Override
    public void flip(){
        source = source.copy();
        source.flip();
        dest = dest.clone();
        for(int i = 0; i < dest.length; i++){
            dest[i] = dest[i].copy();
            dest[i].flip();
        }
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        source.shoot(totalShots, (x, y, rotation, delay, move) -> {
            for(var pattern : dest){
                pattern.shoot(totalShots, (x2, y2, rot2, delay2, mover) -> {
                    handler.shoot(x + x2, y + y2, rotation + rot2, delay + delay2, move == null && mover == null ? null : b -> {
                        if(move != null) move.move(b);
                        if(mover != null) mover.move(b);
                    });
                }, null);
            }
        }, barrelIncrementer);
    }
}
