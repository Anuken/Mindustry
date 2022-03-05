package mindustry.entities;

import arc.math.geom.*;
import mindustry.gen.*;

/** Applies custom movement to a bullet. */
public interface Mover{
    float move(Bullet bullet);
}
