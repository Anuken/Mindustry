package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.type.Mech;
import io.anuke.ucore.util.Bundles;

public class MechMission extends Mission{
    private final Mech mech;

    public MechMission(Mech mech){
        this.mech = mech;
    }

    @Override
    public boolean isComplete(){
        return false;
    }

    @Override
    public String displayString(){
        return Bundles.format("te");
    }
}
