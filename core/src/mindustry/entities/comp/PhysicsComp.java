package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.async.PhysicsProcess.*;
import mindustry.gen.*;

/** Affected by physics.
 * Will bounce off of other objects that are at similar elevations.
 * Has mass.*/
@Component
abstract class PhysicsComp implements Velc, Hitboxc, Flyingc{
    transient PhysicRef physref;
    transient float mass = 1f;

    public void impulse(float x, float y){
        vel().add(x / mass, y / mass);
    }
}
