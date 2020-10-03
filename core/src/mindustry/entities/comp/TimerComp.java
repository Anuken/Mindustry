package mindustry.entities.comp;

import arc.util.*;
import mindustry.annotations.Annotations.*;

@Component
abstract class TimerComp{
    transient Interval timer = new Interval(6);

    public boolean timer(int index, float time){
        return timer.get(index, time);
    }
}
