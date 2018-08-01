package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.game.GameMode;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.state;

public class WaveMission implements Mission{
    private final int target;

    public WaveMission(int target){
        this.target = target;
    }

    @Override
    public GameMode getMode(){
        return GameMode.waves;
    }

    @Override
    public String displayString(){
        return Bundles.format("text.mission.wave", target);
    }

    @Override
    public boolean isComplete(){
        return state.wave >= target;
    }
}
