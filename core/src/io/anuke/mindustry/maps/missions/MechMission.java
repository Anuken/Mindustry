package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.type.Mech;
import io.anuke.ucore.util.Bundles;

public class MechMission extends Mission{
    private final Mech mech;

    public MechMission(Mech mech){
        this.mech = mech;
    }

    @Override
    public boolean isComplete(){
        for(Player player : Vars.playerGroup.all()){
            if(player.mech == mech){
                return true;
            }
        }
        return false;
    }

    @Override
    public String displayString(){
        return Bundles.format("text.mission.mech", mech.localizedName());
    }
}
