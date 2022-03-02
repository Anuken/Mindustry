package mindustry.world;

import arc.util.*;
import mindustry.type.*;

public interface WorldContext{

    /** Return a tile in the tile array.*/
    Tile tile(int index);

    /** Create the tile array.*/
    void resize(int width, int height);

    /** This should create a tile and put it into the tile array, then return it. */
    Tile create(int x, int y, int floorID, int overlayID, int wallID);

    /** Returns whether the world is already generating.*/
    boolean isGenerating();

    /** Begins generating.*/
    void begin();

    /** End generating, prepares tiles.*/
    void end();

    /** Called when a building is finished reading. */
    default void onReadBuilding(){}

    default @Nullable Sector getSector(){
        return null;
    }

    /** @return whether the SaveLoadEvent fired after the end should be counted as a new map load. */
    default boolean isMap(){
        return false;
    }

}
