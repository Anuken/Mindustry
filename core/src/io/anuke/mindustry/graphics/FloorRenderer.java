package io.anuke.mindustry.graphics;

import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.ucore.core.Events;

//TODO point shader mesh
public class FloorRenderer{
    private final static int chunksize = 64;

    public FloorRenderer(){
        Events.on(WorldLoadGraphicsEvent.class, event -> clearTiles());
    }

    /**Draws all the floor in the camera range.*/
    public void drawFloor(){

    }
    /**Clears the mesh and renders the entire world to it.*/
    public void clearTiles(){

    }
}
