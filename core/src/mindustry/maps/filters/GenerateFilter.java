package mindustry.maps.filters;

import arc.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;

public abstract class GenerateFilter{
    protected transient float o = (float)(Math.random() * 10000000.0);
    protected transient long seed;
    protected transient GenerateInput in;

    public void apply(Tiles tiles, GenerateInput in){
        this.in = in;

        if(isBuffered()){
            //buffer of tiles used, each tile packed into a long struct
            long[] buffer = new long[tiles.width * tiles.height];

            for(int i = 0; i < tiles.width * tiles.height; i++){
                Tile tile = tiles.geti(i);

                in.apply(tile.x, tile.y, tile.block(), tile.floor(), tile.overlay());
                apply();

                buffer[i] = PackTile.get(in.block.id, in.floor.id, in.overlay.id);
            }

            //write to buffer
            for(int i = 0; i < tiles.width * tiles.height; i++){
                Tile tile = tiles.geti(i);
                long b = buffer[i];

                Block block = Vars.content.block(PackTile.block(b)), floor = Vars.content.block(PackTile.floor(b)), overlay = Vars.content.block(PackTile.overlay(b));

                tile.setFloor(floor.asFloor());
                tile.setOverlay(!floor.asFloor().hasSurface() && overlay.asFloor().needsSurface ? Blocks.air : overlay);

                if(!tile.block().synthetic() && !block.synthetic()){
                    tile.setBlock(block);
                }
            }
        }else{
            for(Tile tile : tiles){
                in.apply(tile.x, tile.y, tile.block(), tile.floor(), tile.overlay());
                apply();

                tile.setFloor(in.floor.asFloor());
                tile.setOverlay(!in.floor.asFloor().hasSurface() && in.overlay.asFloor().needsSurface ? Blocks.air : in.overlay);

                if(!tile.block().synthetic() && !in.block.synthetic()){
                    tile.setBlock(in.block);
                }
            }
        }
    }

    public final void apply(GenerateInput in){
        this.in = in;
        apply();
    }

    /** @return a new array of options for configuring this filter */
    public abstract FilterOption[] options();

    /** apply the actual filter on the input */
    protected void apply(){}

    /** draw any additional guides */
    public void draw(Image image){}

    /** localized display name */
    public String name(){
        Class c = getClass();
        if(c.isAnonymousClass()) c = c.getSuperclass();
        return Core.bundle.get("filter." + c.getSimpleName().toLowerCase().replace("filter", ""), c.getSimpleName().replace("Filter", ""));
    }

    /** set the seed to a random number */
    public void randomize(){
        seed = Mathf.random(99999999);
    }

    /** @return whether this filter needs a read/write buffer (e.g. not a 1:1 tile mapping). */
    public boolean isBuffered(){
        return false;
    }

    /** @return whether this filter can *only* be used while generating the map, e.g. is not undoable. */
    public boolean isPost(){
        return false;
    }

    //utility generation functions

    protected float noise(float x, float y, float scl, float mag){
        return (float)in.noise.octaveNoise2D(1f, 0f, 1f / scl, x + o, y + o) * mag;
    }

    protected float noise(float x, float y, float scl, float mag, float octaves, float persistence){
        return (float)in.noise.octaveNoise2D(octaves, persistence, 1f / scl, x + o, y + o) * mag;
    }

    protected float rnoise(float x, float y, float scl, float mag){
        return in.pnoise.getValue((int)(x + o), (int)(y + o), 1f / scl) * mag;
    }

    protected float chance(){
        return Mathf.randomSeed(Pack.longInt(in.x, in.y + (int)seed));
    }

    /** an input for generating at a certain coordinate. should only be instantiated once. */
    public static class GenerateInput{

        /** input size parameters */
        public int x, y, width, height;

        /** output parameters */
        public Block floor, block, overlay;

        Simplex noise = new Simplex();
        RidgedPerlin pnoise = new RidgedPerlin(0, 1);
        TileProvider buffer;

        public void apply(int x, int y, Block block, Block floor, Block overlay){
            this.floor = floor;
            this.block = block;
            this.overlay = overlay;
            this.x = x;
            this.y = y;
        }

        public void begin(GenerateFilter filter, int width, int height, TileProvider buffer){
            this.buffer = buffer;
            this.width = width;
            this.height = height;
            noise.setSeed(filter.seed);
            pnoise.setSeed((int)(filter.seed + 1));
        }

        Tile tile(float x, float y){
            return buffer.get(Mathf.clamp((int)x, 0, width - 1), Mathf.clamp((int)y, 0, height - 1));
        }

        public interface TileProvider{
            Tile get(int x, int y);
        }
    }

    @Struct
    class PackTileStruct{
        short block, floor, overlay;
    }
}
