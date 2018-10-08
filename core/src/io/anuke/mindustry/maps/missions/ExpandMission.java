package io.anuke.mindustry.maps.missions;

import static io.anuke.mindustry.Vars.*;

/**An action mission which simply expands the sector.*/
public class ExpandMission extends ActionMission{
    private boolean done = false;

    public ExpandMission(int expandX, int expandY){
        runner = () -> {
            if(headless){
                world.sectors.expandSector(world.getSector(), expandX, expandY);
                done = true;
            }else{
                ui.loadLogic(() -> {
                    world.sectors.expandSector(world.getSector(), expandX, expandY);
                    done = true;
                });
            }
        };
    }

    @Override
    public void onBegin(){
        runner.run();
    }

    @Override
    public boolean isComplete(){
        return done;
    }

    @Override
    public void onComplete(){
        done = false;
    }
}
