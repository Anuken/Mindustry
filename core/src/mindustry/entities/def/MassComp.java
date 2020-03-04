package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class MassComp implements Velc{
    transient float mass = 1f;

    public void impulse(float x, float y){
        vel().add(x / mass, y / mass);
    }
}
