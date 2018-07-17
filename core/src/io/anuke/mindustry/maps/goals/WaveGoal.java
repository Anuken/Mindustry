package io.anuke.mindustry.maps.goals;

import static io.anuke.mindustry.Vars.*;

public class WaveGoal implements Goal{
    private final int target;

    public WaveGoal(int target){
        this.target = target;
    }

    @Override
    public boolean isComplete(){
        return state.wave >= target;
    }
}
