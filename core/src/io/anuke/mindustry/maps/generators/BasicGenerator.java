package io.anuke.mindustry.maps.generators;

import io.anuke.arc.collection.*;
import io.anuke.arc.function.IntPositionConsumer;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.Floor;

import java.util.PriorityQueue;

public abstract class BasicGenerator extends RandomGenerator{
    protected static final DistanceHeuristic manhattan = (x1, y1, x2, y2) -> Math.abs(x1 - x2) + Math.abs(y1 - y2);

    protected Array<Block> ores;
    protected Simplex sim = new Simplex();
    protected Simplex sim2 = new Simplex();

    public BasicGenerator(int width, int height, Block... ores){
        super(width, height);
        this.ores = Array.with(ores);
    }

    @Override
    public void generate(Tile[][] tiles){
        int seed = Mathf.random(99999999);
        sim.setSeed(seed);
        sim2.setSeed(seed + 1);
        super.generate(tiles);
    }

    public void ores(Tile[][] tiles){
        pass(tiles, (x, y) -> {
            if(ores != null){
                int offsetX = x - 4, offsetY = y + 23;
                for(int i = ores.size - 1; i >= 0; i--){
                    Block entry = ores.get(i);
                    if(Math.abs(0.5f - sim.octaveNoise2D(2, 0.7, 1f / (40 + i * 2), offsetX, offsetY + i*999)) > 0.26f &&
                    Math.abs(0.5f - sim2.octaveNoise2D(1, 1, 1f / (30 + i * 4), offsetX, offsetY - i*999)) > 0.37f){
                        ore = entry;
                        break;
                    }
                }
            }
        });
    }

    public void terrain(Tile[][] tiles, Block dst, float scl, float mag, float cmag){
        pass(tiles, (x, y) -> {
            double rocks = sim.octaveNoise2D(5, 0.5, 1f / scl, x, y) * mag
            + Mathf.dst((float)x / width, (float)y / height, 0.5f, 0.5f) * cmag;

            double edgeDist = Math.min(x, Math.min(y, Math.min(Math.abs(x - (width - 1)), Math.abs(y - (height - 1)))));
            double transition = 5;
            if(edgeDist < transition){
                rocks += (transition - edgeDist) / transition / 1.5;
            }

            if(rocks > 0.9){
                block =dst;
            }
        });
    }

    public void noise(Tile[][] tiles, Block floor, Block block, int octaves, float falloff, float scl, float threshold){
        sim.setSeed(Mathf.random(99999));
        pass(tiles, (x, y) -> {
            if(sim.octaveNoise2D(octaves, falloff, 1f / scl, x, y) > threshold){
                Tile tile = tiles[x][y];
                this.floor = floor;
                if(tile.block().solid){
                    this.block = block;
                }
            }
        });
    }

    public void overlay(Tile[][] tiles, Block floor, Block block, float chance, int octaves, float falloff, float scl, float threshold){
        sim.setSeed(Mathf.random(99999));
        pass(tiles, (x, y) -> {
            if(sim.octaveNoise2D(octaves, falloff, 1f / scl, x, y) > threshold && Mathf.chance(chance) && tiles[x][y].floor() == floor){
                ore = block;
            }
        });
    }

    public void tech(Tile[][] tiles){
        Block[] blocks = {Blocks.darkPanel3};
        int secSize = 20;
        pass(tiles, (x, y) -> {
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

    public void distort(Tile[][] tiles, float scl, float mag){
        Block[][] blocks = new Block[width][height];
        Floor[][] floors = new Floor[width][height];

        each((x, y) -> {
            float cx = x + noise(x, y, scl, mag) - mag / 2f, cy = y + noise(x, y + 1525215f, scl, mag) - mag / 2f;
            Tile other = tiles[Mathf.clamp((int)cx, 0, width-1)][Mathf.clamp((int)cy, 0, height-1)];
            blocks[x][y] = other.block();
            floors[x][y] = other.floor();
        });

        pass(tiles, (x, y) -> {
            floor = floors[x][y];
            block = blocks[x][y];
        });
    }

    public void scatter(Tile[][] tiles, Block target, Block dst, float chance){
        pass(tiles, (x, y) -> {
            if(!Mathf.chance(chance)) return;
            if(floor == target){
                floor = dst;
            }else if(block == target){
                block = dst;
            }
        });
    }

    public void each(IntPositionConsumer r){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                r.accept(x, y);
            }
        }
    }

    protected float noise(float x, float y, float scl, float mag){
        return (float)sim2.octaveNoise2D(1f, 0f, 1f / scl, x + 0x361266f, y + 0x251259f) * mag;
    }

    public void pass(Tile[][] tiles, IntPositionConsumer r){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                floor = tiles[x][y].floor();
                block = tiles[x][y].block();
                ore = tiles[x][y].overlay();
                r.accept(x, y);
                tiles[x][y] = new Tile(x, y, floor.id, ore.id, block.id);
            }
        }
    }

    public void brush(Tile[][] tiles, Array<Tile> path, int rad){
        path.each(tile -> erase(tiles, tile.x, tile.y, rad));
    }

    public void erase(Tile[][] tiles, int cx, int cy, int rad){
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                int wx = cx + x, wy = cy + y;
                if(Structs.inBounds(wx, wy, width, height) && Mathf.dst(x, y, 0, 0) <= rad){
                    Tile other = tiles[wx][wy];
                    other.setBlock(Blocks.air);
                }
            }
        }
    }

    public Array<Tile> pathfind(Tile[][] tiles, int startX, int startY, int endX, int endY, TileHueristic th, DistanceHeuristic dh){
        Tile start = tiles[startX][startY];
        Tile end = tiles[endX][endY];
        GridBits closed = new GridBits(width, height);
        IntFloatMap costs = new IntFloatMap();
        PriorityQueue<Tile> queue = new PriorityQueue<>(tiles.length * tiles[0].length / 2, (a, b) -> Float.compare(costs.get(a.pos(), 0f) + dh.cost(a.x, a.y, end.x, end.y), costs.get(b.pos(), 0f) + dh.cost(b.x, b.y, end.x, end.y)));
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
                if(Structs.inBounds(newx, newy, width, height)){
                    Tile child = tiles[newx][newy];
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
            current = tiles[current.x + p.x][current.y + p.y];
        }

        out.reverse();

        return out;
    }

    public void inverseFloodFill(Tile[][] tiles, Tile start, Block block){
        IntArray arr = new IntArray();
        arr.add(start.pos());
        while(!arr.isEmpty()){
            int i = arr.pop();
            int x = Pos.x(i), y = Pos.y(i);
            tiles[x][y].cost = 2;
            for(Point2 point : Geometry.d4){
                int newx = x + point.x, newy = y + point.y;
                if(Structs.inBounds(newx, newy, width, height)){
                    Tile child = tiles[newx][newy];
                    if(child.block() == Blocks.air && child.cost != 2){
                        child.cost = 2;
                        arr.add(child.pos());
                    }
                }
            }
        }

        for(int x = 0; x < width; x ++){
            for(int y = 0; y < height; y++){
                Tile tile = tiles[x][y];
                if(tile.cost != 2 && tile.block() == Blocks.air){
                    tile.setBlock(block);
                }
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
