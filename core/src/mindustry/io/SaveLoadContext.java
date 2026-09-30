package mindustry.io;

import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public abstract class SaveLoadContext{
    /** true when generating a map preview */
    public boolean preview;
    public @Nullable String ruleString;
    public Seq<Building> allBuildings = new Seq<>();

    /** Return a tile in the tile array.*/
    public abstract Tile tile(int index);

    /** Create the tile array.*/
    public abstract void resize(int width, int height);

    /** This should create a tile and put it into the tile array, then return it. */
    public abstract Tile create(int x, int y, int floorID, int overlayID, int wallID);

    /** Returns whether the world is already generating.*/
    public abstract boolean isGenerating();

    /** Begins generating.*/
    public abstract void begin();

    /** End generating, prepares tiles.*/
    public abstract void end();

    /** Called when a building is finished reading. */
    public void onReadBuilding(){}

    /** Called when data finishes reading for a tile. */
    public void onReadTileData(){}

    public @Nullable Sector getSector(){
        return null;
    }

    /** @return whether the SaveLoadEvent fired after the end should be counted as a new map load. */
    public boolean isMap(){
        return false;
    }
}
