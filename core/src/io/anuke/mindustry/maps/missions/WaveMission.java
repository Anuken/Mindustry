package io.anuke.mindustry.maps.missions;

import static io.anuke.mindustry.Vars.*;

public class WaveMission implements Mission{
    private final int target;

    public WaveMission(int target){
        this.target = target;
    }

    @Override
    public boolean isComplete(){
        return state.wave >= target;
    }
}
