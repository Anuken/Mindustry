package mindustry.entities.traits;

import arc.math.*;
import arc.util.Time;

public interface TimeTrait extends Scaled, Entity{

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
