package mindustry.entities.units;

import arc.func.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Class for storing build plans. Can be either a place or remove plan. */
public class BuildPlan implements Position, QuadTreeObject{
    /** Position and rotation of this plan. */
    public int x, y, rotation;
    /** Block being placed. If null, this is a breaking plan.*/
    public @Nullable Block block;
    /** Whether this is a break plan.*/
    public boolean breaking;
    /** Config int. Not used unless hasConfig is true.*/
    public Object config;
    /** Original position, only used in schematics.*/
    public int originalX, originalY, originalWidth, originalHeight;

    /** Last progress.*/
    public float progress;
    /** Whether construction has started for this plan. */
    public boolean initialized, stuck, cachedValid;
    /** If true, this plan is in the world. If false, it is being rendered in a schematic. */
    public boolean worldContext = true;

    /** Visual scale. Used only for rendering.*/
    public float animScale = 0f;

    /** This creates a build plan. */
    public BuildPlan(int x, int y, int rotation, Block block){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
    }

    /** This creates a build plan with a config. */
    public BuildPlan(int x, int y, int rotation, Block block, Object config){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
        this.config = config;
    }

    /** This creates a remove plan. */
    public BuildPlan(int x, int y){
        this.x = x;
        this.y = y;
        this.rotation = -1;
        this.block = world.tile(x, y).block();
        this.breaking = true;
    }

    public BuildPlan(){

    }

    public boolean placeable(Team team){
        return Build.validPlace(block, team, x, y, rotation);
    }

    public boolean isRotation(Team team){
        if(breaking) return false;
        Tile tile = tile();
        return tile != null && tile.team() == team && tile.block() == block && tile.build != null && tile.build.rotation != rotation;
    }

    public boolean samePos(BuildPlan other){
        return x == other.x && y == other.y;
    }

    /** Transforms the internal position of this config using the specified function, and return the result. */
    public static Object pointConfig(Block block, Object config, Cons<Point2> cons){
        if(config instanceof Point2 point){
            config = point.cpy();
            cons.get((Point2)config);
        }else if(config instanceof Point2[] points){
            Point2[] result = new Point2[points.length];
            int i = 0;
            for(Point2 p : points){
                result[i] = p.cpy();
                cons.get(result[i++]);
            }
            config = result;
        }else if(block != null){
            config = block.pointConfig(config, cons);
        }
        return config;
    }

    /** Transforms the internal position of this config using the specified function. */
    public void pointConfig(Cons<Point2> cons){
        this.config = pointConfig(block, this.config, cons);
    }

    public BuildPlan copy(){
        BuildPlan copy = new BuildPlan();
        copy.x = x;
        copy.y = y;
        copy.rotation = rotation;
        copy.block = block;
        copy.breaking = breaking;
        copy.config = config;
        copy.originalX = originalX;
        copy.originalY = originalY;
        copy.progress = progress;
        copy.initialized = initialized;
        copy.animScale = animScale;
        return copy;
    }

    public BuildPlan original(int x, int y, int originalWidth, int originalHeight){
        originalX = x;
        originalY = y;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        return this;
    }

    public Rect bounds(Rect rect){
        if(breaking){
            return rect.set(-100f, -100f, 0f, 0f);
        }else{
            return block.bounds(x, y, rect);
        }
    }

    public BuildPlan set(int x, int y, int rotation, Block block){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
        return this;
    }

    public float drawx(){
        return x*tilesize + (block == null ? 0 : block.offset);
    }

    public float drawy(){
        return y*tilesize + (block == null ? 0 : block.offset);
    }

    public @Nullable Tile tile(){
        return world.tile(x, y);
    }

    public @Nullable Building build(){
        return world.build(x, y);
    }

    @Override
    public void hitbox(Rect out){
        if(block != null){
            out.setCentered(x * tilesize + block.offset, y * tilesize + block.offset, block.size * tilesize);
        }else{
            out.setCentered(x * tilesize, y * tilesize, tilesize);
        }
    }

    @Override
    public float getX(){
        return drawx();
    }

    @Override
    public float getY(){
        return drawy();
    }

    @Override
    public String toString(){
        return "BuildPlan{" +
        "x=" + x +
        ", y=" + y +
        ", rotation=" + rotation +
        ", block=" + block +
        ", breaking=" + breaking +
        ", progress=" + progress +
        ", initialized=" + initialized +
        ", config=" + config +
        '}';
    }
}
