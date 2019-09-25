package io.anuke.mindustry.entities.traits;

import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;

public interface TimeTrait extends ScaleTrait, Entity{

    float lifetime();

    void time(float time);

    float time();

    default void updateTime(){
        time(Mathf.clamp(time() + Time.delta(), 0, lifetime()));

        if(time() >= lifetime()){
            remove();
        }
    }

    //fin() is not implemented due to compiler issues with iOS/RoboVM
}
