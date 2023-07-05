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
import mindustry.world.blocks.environment.*;

public abstract class GenerateFilter implements Cloneable{
    public int seed = 0;

    public void apply(Tiles tiles, GenerateInput in){

        if(isBuffered()){
            //buffer of tiles used, each tile packed into a long struct
            long[] buffer = new long[tiles.width * tiles.height];

            for(int i = 0; i < tiles.width * tiles.height; i++){
                Tile tile = tiles.geti(i);

                in.set(tile.x, tile.y, tile.block(), tile.floor(), tile.overlay());
                apply(in);

                buffer[i] = PackTile.get(in.block.id, in.floor.id, in.overlay.id);
            }

            //write to buffer
            for(int i = 0; i < tiles.width * tiles.height; i++){
                Tile tile = tiles.geti(i);
                long b = buffer[i];

                Block block = Vars.content.block(PackTile.block(b)), floor = Vars.content.block(PackTile.floor(b)), overlay = Vars.content.block(PackTile.overlay(b));

                tile.setFloor(floor.asFloor());
                tile.setOverlay(!floor.asFloor().hasSurface() && overlay.asFloor().needsSurface && overlay instanceof OreBlock ? Blocks.air : overlay);

                if(!tile.block().synthetic() && !block.synthetic()){
                    tile.setBlock(block);
                }
            }
        }else{
            for(Tile tile : tiles){
                in.set(tile.x, tile.y, tile.block(), tile.floor(), tile.overlay());
                apply(in);

                tile.setFloor(in.floor.asFloor());
                tile.setOverlay(!in.floor.asFloor().hasSurface() && in.overlay.asFloor().needsSurface && in.overlay instanceof OreBlock ? Blocks.air : in.overlay);

                if(!tile.block().synthetic() && !in.block.synthetic()){
                    tile.setBlock(in.block);
                }
            }
        }
    }

    /** @return a new array of options for configuring this filter */
    public abstract FilterOption[] options();

    /** apply the actual filter on the input */
    public void apply(GenerateInput in){}

    /** draw any additional guides */
    public void draw(Image image){}

    public String simpleName(){
        Class c = getClass();
        if(c.isAnonymousClass()) c = c.getSuperclass();
        return c.getSimpleName().toLowerCase().replace("filter", "");
    }

    /** localized display name */
    public String name(){
        return Core.bundle.get("filter." + simpleName());
    }

    public char icon(){
        return '\0';
    }

    /** set the seed to a random number */
    public void randomize(){
        seed = Mathf.random(999999999);
    }

    /** @return whether this filter needs a read/write buffer (e.g. not a 1:1 tile mapping). */
    public boolean isBuffered(){
        return false;
    }

    /** @return whether this filter can *only* be used while generating the map, e.g. is not undoable. */
    public boolean isPost(){
        return false;
    }

    //utility generation functions; + 10 is added as noise has similar values at 0,0

    protected float noise(int seedOffset, GenerateInput in, float scl, float mag){
        return Simplex.noise2d(seedOffset + seed, 1f, 0f, 1f / scl, in.x + 10, in.y + 10) * mag;
    }

    protected float noise(GenerateInput in, float scl, float mag){
        return Simplex.noise2d(seed, 1f, 0f, 1f / scl, in.x + 10, in.y + 10) * mag;
    }

    protected float noise(GenerateInput in, float scl, float mag, float octaves, float persistence){
        return Simplex.noise2d(seed, octaves, persistence, 1f / scl, in.x + 10, in.y + 10) * mag;
    }

    protected float noise(float x, float y, float scl, float mag, float octaves, float persistence){
        return Simplex.noise2d(seed, octaves, persistence, 1f / scl, x + 10, y + 10) * mag;
    }

    protected float rnoise(float x, float y, float scl, float mag){
        return Ridged.noise2d(seed + 1, (int)(x), (int)(y), 1f / scl) * mag;
    }

    protected float rnoise(float x, float y, int octaves, float scl, float falloff, float mag){
        return Ridged.noise2d(seed + 1, (int)(x), (int)(y), octaves, falloff, 1f / scl) * mag;
    }

    protected float chance(int x, int y){
        return Mathf.randomSeed(Pack.longInt(x, y + seed));
    }

    public GenerateFilter copy(){
        try{
            return (GenerateFilter) clone();
        }catch(CloneNotSupportedException disgrace){
            throw new RuntimeException("java is the best language", disgrace);
        }
    }

    /** an input for generating at a certain coordinate. should only be instantiated once. */
    public static class GenerateInput{

        /** input size parameters */
        public int x, y, width, height;

        /** output parameters */
        public Block floor, block, overlay;

        TileProvider buffer;

        public void set(int x, int y, Block block, Block floor, Block overlay){
            this.floor = floor;
            this.block = block;
            this.overlay = overlay;
            this.x = x;
            this.y = y;
        }

        public void begin(int width, int height, TileProvider buffer){
            this.buffer = buffer;
            this.width = width;
            this.height = height;
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
