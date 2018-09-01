package io.anuke.mindustry.maps.generation.pathfinding;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

public abstract class TilePathfinder{
    protected Tile[][] tiles;

    public TilePathfinder(Tile[][] tiles){
        this.tiles = tiles;
    }

    protected boolean inBounds(int x, int y){
        return Mathf.inBounds(x, y, tiles);
    }

    public abstract void search(Tile start, Tile end, Array<Tile> out);
}
