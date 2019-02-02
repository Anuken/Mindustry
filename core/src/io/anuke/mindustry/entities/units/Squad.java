package io.anuke.mindustry.entities.units;

import io.anuke.arc.Core;
import io.anuke.arc.math.geom.Vector2;

/**
 * Used to group entities together, for formations and such.
 * Usually, squads are used by units spawned in the same wave.
 */
//TODO remove? is this necessary?
public class Squad{
    public Vector2 direction = new Vector2();
    public int units;

    private long lastUpdated;

    protected void update(){
        if(Core.graphics.getFrameId() != lastUpdated){
            direction.setZero();
            lastUpdated = Core.graphics.getFrameId();
        }
    }
}
