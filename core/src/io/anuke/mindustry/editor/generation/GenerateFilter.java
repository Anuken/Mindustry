package io.anuke.mindustry.editor.generation;

import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.editor.MapEditor;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

public abstract class GenerateFilter{
    protected float o = (float)(Math.random()*10000000.0);
    protected GenerateInput in;

    public FilterOption[] options;

    protected abstract void apply();

    protected float noise(float x, float y, float scl, float mag){
        return (float)in.noise.octaveNoise2D(1f, 0f, 1f/scl, x + o, y + o)*mag;
    }

    protected float noise(float x, float y, float scl, float mag, float octaves, float persistence){
        return (float)in.noise.octaveNoise2D(octaves, persistence, 1f/scl, x + o, y + o)*mag;
    }

    public void options(FilterOption... options){
        this.options = options;
    }

    public final void apply(GenerateInput in){
        this.in = in;
        apply();
    }

    public static class GenerateInput{
        public Floor srcfloor;
        public Block srcblock;
        public Block srcore;
        public int x, y;

        public MapEditor editor;
        public Block floor, block, ore;

        Simplex noise = new Simplex();

        public void begin(MapEditor editor, int x, int y, Block floor, Block block, Block ore){
            this.editor = editor;
            this.floor = this.srcfloor = (Floor)floor;
            this.block = this.srcblock = block;
            this.ore = srcore = ore;
            this.x = x;
            this.y = y;
        }

        public void randomize(){
            noise.setSeed(Mathf.random(99999999));
        }

        Tile tile(double x, double y){
            return editor.tile((int)Mathf.clamp(x, 0, editor.width() - 1), (int)Mathf.clamp(y, 0, editor.height() - 1));
        }
    }
}
