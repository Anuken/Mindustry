package mindustry.maps.generators;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.world.*;

import java.util.*;

import static mindustry.Vars.world;

public abstract class BasicGenerator implements WorldGenerator{
    protected static final DistanceHeuristic manhattan = (x1, y1, x2, y2) -> Math.abs(x1 - x2) + Math.abs(y1 - y2);

    protected Simplex sim = new Simplex();
    protected Simplex sim2 = new Simplex();
    
    protected int width, height;
    protected Tiles tiles;

    //for drawing
    protected Block floor;
    protected Block block;
    protected Block ore;

    @Override
    public void generate(Tiles tiles){
        this.tiles = tiles;
        this.width = tiles.width;
        this.height = tiles.height;
        int seed = Mathf.random(99999999);
        sim.setSeed(seed);
        sim2.setSeed(seed + 1);

        generate();
    }

    protected void generate(){

    }

    public void ores(Array<Block> ores){
        pass((x, y) -> {
            int offsetX = x - 4, offsetY = y + 23;
            for(int i = ores.size - 1; i >= 0; i--){
                Block entry = ores.get(i);
                if(Math.abs(0.5f - sim.octaveNoise2D(2, 0.7, 1f / (40 + i * 2), offsetX, offsetY + i*999)) > 0.26f &&
                Math.abs(0.5f - sim2.octaveNoise2D(1, 1, 1f / (30 + i * 4), offsetX, offsetY - i*999)) > 0.37f){
                    ore = entry;
                    break;
                }
            }
        });
    }

    public void terrain(Block dst, float scl, float mag, float cmag){
        pass((x, y) -> {
            double rocks = sim.octaveNoise2D(5, 0.5, 1f / scl, x, y) * mag
            + Mathf.dst((float)x / width, (float)y / height, 0.5f, 0.5f) * cmag;

            double edgeDist = Math.min(x, Math.min(y, Math.min(Math.abs(x - (width - 1)), Math.abs(y - (height - 1)))));
            double transition = 5;
            if(edgeDist < transition){
                rocks += (transition - edgeDist) / transition / 1.5;
            }

            if(rocks > 0.9){
                block = dst;
            }
        });
    }

    public void noise(Block floor, Block block, int octaves, float falloff, float scl, float threshold){
        sim.setSeed(Mathf.random(99999));
        pass((x, y) -> {
            if(sim.octaveNoise2D(octaves, falloff, 1f / scl, x, y) > threshold){
                Tile tile = tiles.getn(x, y);
                this.floor = floor;
                if(tile.block().solid){
                    this.block = block;
                }
            }
        });
    }

    public void overlay(Block floor, Block block, float chance, int octaves, float falloff, float scl, float threshold){
        sim.setSeed(Mathf.random(99999));
        pass((x, y) -> {
            if(sim.octaveNoise2D(octaves, falloff, 1f / scl, x, y) > threshold && Mathf.chance(chance) && tiles.getn(x, y).floor() == floor){
                ore = block;
            }
        });
    }

    public void tech(){
        Block[] blocks = {Blocks.darkPanel3};
        int secSize = 20;
        pass((x, y) -> {
            int mx = x % secSize, my = y % secSize;
            int sclx = x / secSize, scly = y / secSize;
            if(noise(sclx, scly, 10f, 1f) > 0.63f && (mx == 0 || my == 0 || mx == secSize - 1 || my == secSize - 1)){
                if(Mathf.chance(noise(x + 0x231523, y, 40f, 1f))){
                    floor = Structs.random(blocks);
                    if(Mathf.dst(mx, my, secSize/2, secSize/2) > secSize/2f + 2){
                        floor = Blocks.darkPanel4;
                    }
                }

                if(block.solid && Mathf.chance(0.7)){
                    block = Blocks.darkMetal;
                }
            }
        });
    }

    public void distort(float scl, float mag){
        short[] blocks = new short[tiles.width * tiles.height];
        short[] floors = new short[blocks.length];

        tiles.each((x, y) -> {
            int idx = y*tiles.width + x;
            float cx = x + noise(x, y, scl, mag) - mag / 2f, cy = y + noise(x + 155f, y + 155f, scl, mag) - mag / 2f;
            Tile other = tiles.getn(Mathf.clamp((int)cx, 0, tiles.width-1), Mathf.clamp((int)cy, 0, tiles.height-1));
            blocks[idx] = other.block().id;
            floors[idx] = other.floor().id;
        });

        for(int i = 0; i < blocks.length; i++){
            Tile tile = tiles.geti(i);
            tile.setFloor(Vars.content.block(floors[i]).asFloor());
            tile.setBlock(Vars.content.block(blocks[i]));
        }
    }

    public void scatter(Block target, Block dst, float chance){
        pass((x, y) -> {
            if(!Mathf.chance(chance)) return;
            if(floor == target){
                floor = dst;
            }else if(block == target){
                block = dst;
            }
        });
    }

    public void each(Intc2 r){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                r.get(x, y);
            }
        }
    }

    protected float noise(float x, float y, float scl, float mag){
        return (float)sim2.octaveNoise2D(1f, 0f, 1f / scl, x + 0x361266f, y + 0x251259f) * mag;
    }

    public void pass(Intc2 r){
        for(Tile tile : tiles){
            floor = tile.floor();
            block = tile.block();
            ore = tile.overlay();
            r.get(tile.x, tile.y);
            tile.setFloor(floor.asFloor());
            tile.setBlock(block);
            tile.setOverlay(ore);
        }
    }

    public void brush(Array<Tile> path, int rad){
        path.each(tile -> erase(tile.x, tile.y, rad));
    }

    public void erase(int cx, int cy, int rad){
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                int wx = cx + x, wy = cy + y;
                if(Structs.inBounds(wx, wy, width, height) && Mathf.dst(x, y, 0, 0) <= rad){
                    Tile other = tiles.getn(wx, wy);
                    other.setBlock(Blocks.air);
                }
            }
        }
    }

    public Array<Tile> pathfind(int startX, int startY, int endX, int endY, TileHueristic th, DistanceHeuristic dh){
        Tile start = tiles.getn(startX, startY);
        Tile end = tiles.getn(endX, endY);
        GridBits closed = new GridBits(width, height);
        IntFloatMap costs = new IntFloatMap();
        PriorityQueue<Tile> queue = new PriorityQueue<>(tiles.width * tiles.height /4, (a, b) -> Float.compare(costs.get(a.pos(), 0f) + dh.cost(a.x, a.y, end.x, end.y), costs.get(b.pos(), 0f) + dh.cost(b.x, b.y, end.x, end.y)));
        queue.add(start);
        boolean found = false;
        while(!queue.isEmpty()){
            Tile next = queue.poll();
            float baseCost = costs.get(next.pos(), 0f);
            if(next == end){
                found = true;
                break;
            }
            closed.set(next.x, next.y);
            for(Point2 point : Geometry.d4){
                int newx = next.x + point.x, newy = next.y + point.y;
                if(Structs.inBounds(newx, newy, width, height) && world.getDarkness(newx, newy) == 0){
                    Tile child = tiles.getn(newx, newy);
                    if(!closed.get(child.x, child.y)){
                        closed.set(child.x, child.y);
                        child.rotation(child.relativeTo(next.x, next.y));
                        costs.put(child.pos(), th.cost(child) + baseCost);
                        queue.add(child);
                    }
                }
            }
        }

        Array<Tile> out = new Array<>();

        if(!found) return out;

        Tile current = end;
        while(current != start){
            out.add(current);
            Point2 p = Geometry.d4(current.rotation());
            current = tiles.getn(current.x + p.x, current.y + p.y);
        }

        out.reverse();

        return out;
    }

    public void inverseFloodFill(Tile start){
        IntArray arr = new IntArray();
        arr.add(start.pos());
        while(!arr.isEmpty()){
            int i = arr.pop();
            int x = Pos.x(i), y = Pos.y(i);
            tiles.getn(x, y).cost = 2;
            for(Point2 point : Geometry.d4){
                int newx = x + point.x, newy = y + point.y;
                if(tiles.in(newx, newy)){
                    Tile child = tiles.getn(newx, newy);
                    if(child.block() == Blocks.air && child.cost != 2){
                        child.cost = 2;
                        arr.add(child.pos());
                    }
                }
            }
        }

        for(Tile tile : tiles){
            if((tile.cost != 2 && tile.block() == Blocks.air) || world.getDarkness(tile.x, tile.y) != 0){
                tile.setBlock(tile.floor().wall);
            }
        }
    }

    public interface DistanceHeuristic{
        float cost(int x1, int y1, int x2, int y2);
    }

    public interface TileHueristic{
        float cost(Tile tile);
    }
}
