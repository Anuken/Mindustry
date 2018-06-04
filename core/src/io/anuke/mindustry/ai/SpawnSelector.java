package io.anuke.mindustry.ai;

import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.ucore.core.Events;

import static io.anuke.mindustry.Vars.world;

public class SpawnSelector {
    private static final int quadsize = 15;

    public SpawnSelector(){
        Events.on(WorldLoadEvent.class, this::reset);
    }

    public void calculateSpawn(){

        for(int x = 0; x < world.width(); x += quadsize){
            for(int y = 0; y < world.height(); y += quadsize){
                //TODO quadrant operations, etc
            }
        }
    }

    private void reset(){

    }
}
