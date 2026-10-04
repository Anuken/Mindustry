package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.game.Interval;

@Component
abstract class TimerComp{
    transient Interval timer = new mindustry.game.Interval(6);

    public boolean timer(int index, float time){
        if(Float.isInfinite(time)) return false;
        return timer.get(index, time);
    }
}
