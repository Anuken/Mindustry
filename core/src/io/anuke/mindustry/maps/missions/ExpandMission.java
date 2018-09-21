package io.anuke.mindustry.maps.missions;

import static io.anuke.mindustry.Vars.*;

/**An action mission which simply expands the sector.*/
public class ExpandMission extends ActionMission{

    public ExpandMission(int expandX, int expandY){
        super(() -> {
            if(headless){
                world.sectors().expandSector(world.getSector(), expandX, expandY);
            }else{
                ui.loadLogic(() -> world.sectors().expandSector(world.getSector(), expandX, expandY));
            }
        });
    }
}
