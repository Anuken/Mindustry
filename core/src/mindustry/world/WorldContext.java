package mindustry.world;

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

}
