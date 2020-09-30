package mindustry.world;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;

import java.util.*;

/**
 * Contains a list of tiles with extended behavior
 */
public class Tiles implements Iterable<Tile>{
    /** The width of the tilemap */
    public final int width;

    /** The height of the tilemap */
    public final int height;

    /** The internal array contianing the list of tiles */
    final Tile[] array;

    /**
     * Constructor for making a list of tiles
     * @param width The width of the tilemap {@link #width}
     * @param height The height of the tilemap {@link #height}
     */
    public Tiles(int width, int height){
        this.array = new Tile[width * height];
        this.width = width;
        this.height = height;
    }

    /**
     * Loop through each tile by its position
     * @param cons The cons to use for looping
     */
    public void each(Intc2 cons){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                cons.get(x, y);
            }
        }
    }

    /** Fills this tile set with empty air tiles. */
    public void fill(){
        for(int i = 0; i < array.length; i++){
            array[i] = new Tile(i % width, i / width);
        }
    }

    /**
     * Set a tile at a position; does not range-check. use with caution.
     *
     * @param x The x location to set the tile at. Make sure it is in bounds with {@link #width}
     * @param y The y location to set the tile at. Make sure it is in bounds with {@link #width}
     */
    public void set(int x, int y, Tile tile){
        array[y*width + x] = tile;
    }

    /**
     * Check if coordinates are in bounds of the tile array
     *
     * @param x The tile X coordinate
     * @param y The tile Y coordinate
     *
     * @return whether these coordinates are in bounds
     */
    public boolean in(int x, int y){
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Gets a tile at coordinates that can be null if it's out of bounds
     *
     * @param x The location on the x-coordinate
     * @param y The location on the y-coordinate
     *
     * @return a tile at coordinates, or null if out of bounds
     */
    public @Nullable Tile get(int x, int y){
        return (x < 0 || x >= width || y < 0 || y >= height) ? null : array[y*width + x];
    }

    /**
     * Get a tile at coordinates with an exception
     *
     * @param x The location on the x-coordinate
     * @param y The location on the y-coordinate
     *
     * @return a tile at coordinates
     * @exception IllegalArgumentException throws this if out of bounds
     */
    public @NonNull Tile getn(int x, int y){
        if(x < 0 || x >= width || y < 0 || y >= height) throw new IllegalArgumentException(x + ", " + y + " out of bounds: width=" + width + ", height=" + height);
        return array[y*width + x];
    }

    /**
     * Get a tile at coordinates, and clamps the coordinates if they are out of bounds
     *
     * @param x The location on the x-coordinate
     * @param y The location on the y-coordinate
     *
     * @return a tile at coordinates
     */
    public @NonNull Tile getc(int x, int y){
        x = Mathf.clamp(x, 0, width - 1);
        y = Mathf.clamp(y, 0, height - 1);
        return array[y*width + x];
    }
    /**
     * Get a tile at an iteration index [0, width * height]
     *
     * @param idx The iteration index
     *
     * @return a tile at coordinates
     */
    public @NonNull Tile geti(int idx){
        return array[idx];
    }

    /**
     * Get a tile at an int position (not equivalent to geti)
     *
     * @param pos The int position (x and y coordinate packed into an integer)
     *
     * @return a tile at an int position
     */
    public @Nullable Tile getp(int pos){
        return get(Point2.x(pos), Point2.y(pos));
    }

    /**
     * Loop through each tile
     * @param cons The lambda to use to loop through each tile
     */
    public void eachTile(Cons<Tile> cons){
        for(Tile tile : array){
            cons.get(tile);
        }
    }

    /** @return The TileIterator tied to this tile array. */
    @Override
    public Iterator<Tile> iterator(){
        // iterating through the entire map is expensive anyway, so a new allocation doesn't make much of a difference
        return new TileIterator();
    }

    /**
     * Implements through tiles with extra power
     */
    private class TileIterator implements Iterator<Tile>{

        /** The index at which the {@link TileIterator} lies. */
        int index = 0;

        TileIterator(){
        }

        @Override
        public boolean hasNext(){
            return index < array.length;
        }

        @Override
        public Tile next(){
            return array[index++];
        }
    }
}
