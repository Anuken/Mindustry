package io.anuke.mindustry.maps.filters;

import io.anuke.arc.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.noise.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

public abstract class GenerateFilter{
    protected transient float o = (float)(Math.random() * 10000000.0);
    protected transient long seed;
    protected transient GenerateInput in;

    public transient boolean buffered = false;
    public transient FilterOption[] options;

    public final void apply(GenerateInput in){
        this.in = in;
        apply();
        //remove extra ores on liquids
        if(((Floor)in.floor).isLiquid){
            in.ore = Blocks.air;
        }
    }

    /** sets up the options; this is necessary since the constructor can't access subclass variables. */
    protected void options(FilterOption... options){
        this.options = options;
    }

    /** apply the actual filter on the input */
    protected abstract void apply();

    /** draw any additional guides */
    public void draw(Image image){}

    /** localized display name */
    public String name(){
        return Core.bundle.get("filter." + getClass().getSimpleName().toLowerCase().replace("filter", ""), getClass().getSimpleName().replace("Filter", ""));
    }

    /** set the seed to a random number */
    public void randomize(){
        seed = Mathf.random(99999999);
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
        public Block floor, block, ore;

        Simplex noise = new Simplex();
        RidgedPerlin pnoise = new RidgedPerlin(0, 1);
        TileProvider buffer;

        public void apply(int x, int y, Block floor, Block block, Block ore){
            this.floor = floor;
            this.block = block;
            this.ore = ore;
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
}
