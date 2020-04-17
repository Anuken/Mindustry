package mindustry.entities.def;

import arc.util.*;
import mindustry.annotations.Annotations.*;

@Component
abstract class TimerComp{
    Interval timer = new Interval(6);

    public boolean timer(int index, float time){
        return timer.get(index, time);
    }
}
