package mindustry.maps.generators;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public abstract class BasicGenerator implements WorldGenerator{
    protected static final DistanceHeuristic manhattan = (x1, y1, x2, y2) -> Math.abs(x1 - x2) + Math.abs(y1 - y2);
    protected static final ShortArray ints1 = new ShortArray(), ints2 = new ShortArray();

    protected Rand rand = new Rand();

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

        generate();
    }

    protected void generate(){

    }

    //for visual testing only
    public void cliffs2(){
        for(Tile tile : tiles){
            tile.setBlock(Blocks.air);
            tile.cost = tile.floor().isLiquid ? 0 : (byte)(noise(tile.x, tile.y, 4, 0.5f, 90f, 1) * 5);
        }

        for(Tile tile : tiles){
            if(tile.floor().isLiquid) continue;

            int rotation = 0;
            for(int i = 0; i < 8; i++){
                Tile other = tiles.get(tile.x + Geometry.d8[i].x, tile.y + Geometry.d8[i].y);
                if(other != null && other.cost < tile.cost){ //down slope
                    rotation |= (1 << i);
                }
            }

            tile.rotation(rotation);
        }

        for(Tile tile : tiles){
            if(tile.rotation() != 0){
                int rotation = tile.rotation();
                tile.setBlock(Blocks.cliff);
                tile.setOverlay(Blocks.air);
                tile.rotation(rotation);
            }
        }
    }

    public void cliffs(){
        for(Tile tile : tiles){
            if(!tile.block().isStatic()) continue;

            int rotation = 0;
            for(int i = 0; i < 8; i++){
                Tile other = tiles.get(tile.x + Geometry.d8[i].x, tile.y + Geometry.d8[i].y);
                if(other != null && !other.block().isStatic()){
                    rotation |= (1 << i);
                }
            }

            if(rotation != 0){
                tile.setBlock(Blocks.cliff);
            }

            tile.rotation(rotation);
        }

        for(Tile tile : tiles){
            if(tile.block() != Blocks.cliff && tile.block().isStatic()){
                tile.setBlock(Blocks.air);
            }
        }
    }

    public void median(int radius){
        median(radius, 0.5);
    }

    public void median(int radius, double percentile){
        short[] blocks = new short[tiles.width * tiles.height];
        short[] floors = new short[blocks.length];

        tiles.each((x, y) -> {
            ints1.clear();
            ints2.clear();
            Geometry.circle(x, y, width, height, radius, (cx, cy) -> {
                ints1.add(tiles.getn(cx, cy).floorID());
                ints2.add(tiles.getn(cx, cy).blockID());
            });
            ints1.sort();
            ints2.sort();

            floors[x + y*width] = ints1.get(Mathf.clamp((int)(ints1.size * percentile), 0, ints1.size - 1));
            blocks[x + y*width] = ints2.get(Mathf.clamp((int)(ints2.size * percentile), 0, ints2.size - 1));
        });

        pass((x, y) -> {
            block = content.block(blocks[x + y * width]);
            floor = content.block(floors[x + y * width]);
        });
    }

    public void ores(Array<Block> ores){
        pass((x, y) -> {
            if(floor.asFloor().isLiquid) return;

            int offsetX = x - 4, offsetY = y + 23;
            for(int i = ores.size - 1; i >= 0; i--){
                Block entry = ores.get(i);
                if(Math.abs(0.5f - noise(offsetX, offsetY + i*999, 2, 0.7, (40 + i * 2))) > 0.26f &&
                Math.abs(0.5f - noise(offsetX, offsetY - i*999, 1, 1, (30 + i * 4))) > 0.37f){
                    ore = entry;
                    break;
                }
            }
        });
    }

    public void terrain(Block dst, float scl, float mag, float cmag){
        pass((x, y) -> {
            double rocks = noise(x, y, 5, 0.5, scl) * mag
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
        pass((x, y) -> {
            if(noise(octaves, falloff, scl, x, y) > threshold){
                Tile tile = tiles.getn(x, y);
                this.floor = floor;
                if(tile.block().solid){
                    this.block = block;
                }
            }
        });
    }

    public void overlay(Block floor, Block block, float chance, int octaves, float falloff, float scl, float threshold){
        pass((x, y) -> {
            if(noise(x, y, octaves, falloff, scl) > threshold && Mathf.chance(chance) && tiles.getn(x, y).floor() == floor){
                ore = block;
            }
        });
    }

    public void tech(){
        Block[] blocks = {Blocks.darkPanel3};
        int secSize = 20;
        pass((x, y) -> {
            if(floor.asFloor().isLiquid) return;

            int mx = x % secSize, my = y % secSize;
            int sclx = x / secSize, scly = y / secSize;
            if(noise(sclx, scly, 0.2f, 1f) > 0.63f && noise(sclx, scly + 999, 200f, 1f) > 0.6f && (mx == 0 || my == 0 || mx == secSize - 1 || my == secSize - 1)){
                if(Mathf.chance(noise(x + 0x231523, y, 40f, 1f))){
                    floor = blocks[rand.random(0, blocks.length - 1)];
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
            float cx = x + noise(x - 155f, y - 200f, scl, mag) - mag / 2f, cy = y + noise(x + 155f, y + 155f, scl, mag) - mag / 2f;
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

    public void cells(int iterations){
        cells(iterations, 16, 16, 3);
    }

    public void cells(int iterations, int birthLimit, int deathLimit, int cradius){
        GridBits write = new GridBits(tiles.width, tiles.height);
        GridBits read = new GridBits(tiles.width, tiles.height);

        tiles.each((x, y) -> read.set(x, y, !tiles.get(x, y).block().isAir()));

        for(int i = 0; i < iterations; i++){
            tiles.each((x, y) -> {
                int alive = 0;

                for(int cx = -cradius; cx <= cradius; cx++){
                    for(int cy = -cradius; cy <= cradius; cy++){
                        if((cx == 0 && cy == 0) || !Mathf.within(cx, cy, cradius)) continue;
                        if(!Structs.inBounds(x + cx, y + cy, tiles.width, tiles.height) || read.get(x + cx, y + cy)){
                            alive++;
                        }
                    }
                }

                if(read.get(x, y)){
                    write.set(x, y, alive >= deathLimit);
                }else{
                    write.set(x, y, alive > birthLimit);
                }
            });

            //flush results
            read.set(write);
        }

        tiles.each((x, y) -> tiles.get(x, y).setBlock(!read.get(x, y) ? Blocks.air : tiles.get(x, y).floor().wall));
    }

    protected float noise(float x, float y, double scl, double mag){
        return noise(x, y, 1, 1, scl, mag);
    }

    protected abstract float noise(float x, float y, double octaves, double falloff, double scl, double mag);

    protected float noise(float x, float y, double octaves, double falloff, double scl){
        return noise(x, y, octaves, falloff, scl, 1);
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
        PQueue<Tile> queue = new PQueue<>(tiles.width * tiles.height / 4, Structs.comparingFloat(a -> costs.get(a.pos(), 0f) + dh.cost(a.x, a.y, end.x, end.y)));
        queue.add(start);
        boolean found = false;
        while(!queue.empty()){
            Tile next = queue.poll();
            float baseCost = costs.get(next.pos(), 0f);
            if(next == end){
                found = true;
                break;
            }
            closed.set(next.x, next.y);
            for(Point2 point : Geometry.d4){
                int newx = next.x + point.x, newy = next.y + point.y;
                if(Structs.inBounds(newx, newy, width, height) && world.getDarkness(newx, newy) <= 1f){
                    Tile child = tiles.getn(newx, newy);
                    float newCost = th.cost(child) + baseCost;
                    if(!closed.get(child.x, child.y)){
                        closed.set(child.x, child.y);
                        child.rotation(child.relativeTo(next.x, next.y));
                        costs.put(child.pos(), newCost);
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

    public void trimDark(){
        for(Tile tile : tiles){
            boolean any = world.getDarkness(tile.x, tile.y) > 0;
            for(int i = 0; i < 4 && !any; i++){
                any = world.getDarkness(tile.x + Geometry.d4[i].x, tile.y + Geometry.d4[i].y) > 0;
            }

            if(any){
                tile.setBlock(tile.floor().wall);
            }
        }
    }

    public void inverseFloodFill(Tile start){
        IntArray arr = new IntArray();
        arr.add(start.pos());
        while(!arr.isEmpty()){
            int i = arr.pop();
            int x = Point2.x(i), y = Point2.y(i);
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
            if(tile.cost != 2 && tile.block() == Blocks.air){
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
