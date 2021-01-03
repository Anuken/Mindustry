package mindustry.game;

import arc.struct.*;

/**
 * Contains some rule-like data,
 * though not saved with the map.
 * (relevant to current game only)
 */
public class Amendments{
    /** Name of the last player who changed that pos. */
    public IntMap<String> lastAccessed = new IntMap<>();
    /** The last building here before deconstruction. */
    public IntMap<String> lastBuilding = new IntMap<>();

    public void reset(){
        lastAccessed.clear();
        lastBuilding.clear();
    }
}
