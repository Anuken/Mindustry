package mindustry.maps.generators;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.Astar.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public abstract class BasicGenerator implements WorldGenerator{
    protected static final ShortSeq ints1 = new ShortSeq(), ints2 = new ShortSeq();

    protected Rand rand = new Rand();

    protected int width, height;
    protected @Nullable Tiles tiles;

    //for drawing
    protected @Nullable Block floor, block, ore;

    @Override
    public void generate(Tiles tiles){
        this.tiles = tiles;
        this.width = tiles.width;
        this.height = tiles.height;

        generate();
    }

    public Schematic getDefaultLoadout(){
        return Loadouts.basicShard;
    }

    protected void generate(){

    }

    public void median(int radius){
        median(radius, 0.5);
    }

    public void median(int radius, double percentile){
        median(radius, percentile, null);
    }

    public void median(int radius, double percentile, @Nullable Block targetFloor){
        short[] blocks = new short[tiles.width * tiles.height];
        short[] floors = new short[blocks.length];

        tiles.each((x, y) -> {
            if(targetFloor != null && tiles.getn(x, y).floor() != targetFloor) return;

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
            if(targetFloor != null && floor != targetFloor) return;

            block = content.block(blocks[x + y * width]);
            floor = content.block(floors[x + y * width]);
        });
    }

    public void ores(Seq<Block> ores){
        pass((x, y) -> {
            if(!floor.asFloor().hasSurface()) return;

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

    public void ore(Block dest, Block src, float i, float thresh){
        pass((x, y) -> {
            if(floor != src) return;

            if(Math.abs(0.5f - noise(x, y + i*999, 2, 0.7, (40 + i * 2))) > 0.26f * thresh &&
            Math.abs(0.5f - noise(x, y - i*999, 1, 1, (30 + i * 4))) > 0.37f * thresh){
                ore = dest;
            }
        });
    }

    public void oreAround(Block ore, Block wall, int radius, float scl, float thresh){
        for(Tile tile : tiles){
            int x = tile.x, y = tile.y;

            if(tile.block() == Blocks.air && tile.floor().hasSurface() && noise(x, y + ore.id*999, scl, 1f) > thresh){
                boolean found = false;

                outer:
                for(int dx = x-radius; dx <= x+radius; dx++){
                    for(int dy = y-radius; dy <= y+radius; dy++){
                        if(Mathf.within(dx, dy, x, y, radius + 0.001f) && tiles.in(dx, dy) && tiles.get(dx, dy).block() == wall){
                            found = true;
                            break outer;
                        }
                    }
                }

                if(found){
                    tile.setOverlay(ore);
                }
            }
        }
    }

    public void wallOre(Block src, Block dest, float scl, float thresh){
        boolean overlay = dest.isOverlay();
        pass((x, y) -> {
            if(block != Blocks.air){
                boolean empty = false;
                for(Point2 p : Geometry.d8){
                    Tile other = tiles.get(x + p.x, y + p.y);
                    if(other != null && other.block() == Blocks.air){
                        empty = true;
                        break;
                    }
                }

                if(empty && noise(x + 78, y, 4, 0.7f, scl, 1f) > thresh && block == src){
                    if(overlay){
                        ore = dest;
                    }else{
                        block = dest;
                    }
                }
            }
        });
    }

    public void cliffs(){
        for(Tile tile : tiles){
            if(!tile.block().isStatic() || tile.block() == Blocks.cliff) continue;

            int rotation = 0;
            for(int i = 0; i < 8; i++){
                Tile other = world.tiles.get(tile.x + Geometry.d8[i].x, tile.y + Geometry.d8[i].y);
                if(other != null && !other.block().isStatic()){
                    rotation |= (1 << i);
                }
            }

            if(rotation != 0){
                tile.setBlock(Blocks.cliff);
            }

            tile.data = (byte)rotation;
        }

        for(Tile tile : tiles){
            if(tile.block() != Blocks.cliff && tile.block().isStatic()){
                tile.setBlock(Blocks.air);
            }
        }
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
        tech(Blocks.darkPanel3, Blocks.darkPanel4, Blocks.darkMetal);
    }

    public void tech(Block floor1, Block floor2, Block wall){
        int secSize = 20;
        pass((x, y) -> {
            if(!floor.asFloor().hasSurface()) return;

            int mx = x % secSize, my = y % secSize;
            int sclx = x / secSize, scly = y / secSize;
            if(noise(sclx, scly, 0.2f, 1f) > 0.63f && noise(sclx, scly + 999, 200f, 1f) > 0.6f && (mx == 0 || my == 0 || mx == secSize - 1 || my == secSize - 1)){
                if(Mathf.chance(noise(x + 0x231523, y, 40f, 1f))){
                    floor = floor1;
                    if(Mathf.dst(mx, my, secSize/2, secSize/2) > secSize/2f + 2){
                        floor = floor2;
                    }
                }

                if(block.solid && Mathf.chance(0.7)){
                    block = wall;
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

        for(var t : tiles){
            t.setBlock(!read.get(t.x, t.y) ? Blocks.air : t.floor().wall);
        }
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

    public boolean nearWall(int x, int y){
        for(Point2 p : Geometry.d8){
            Tile other = tiles.get(x + p.x, y + p.y);
            if(other != null && other.block() != Blocks.air){
                return true;
            }
        }
        return false;
    }

    public boolean nearAir(int x, int y){
        for(Point2 p : Geometry.d4){
            Tile other = tiles.get(x + p.x, y + p.y);
            if(other != null && other.block() == Blocks.air){
                return true;
            }
        }
        return false;
    }

    public void removeWall(int cx, int cy, int rad, Boolf<Block> pred){
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                int wx = cx + x, wy = cy + y;
                if(Structs.inBounds(wx, wy, width, height) && Mathf.within(x, y, rad)){
                    Tile other = tiles.getn(wx, wy);
                    if(pred.get(other.block())){
                        other.setBlock(Blocks.air);
                    }
                }
            }
        }
    }

    public boolean near(int cx, int cy, int rad, Block block){
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                int wx = cx + x, wy = cy + y;
                if(Structs.inBounds(wx, wy, width, height) && Mathf.within(x, y, rad)){
                    Tile other = tiles.getn(wx, wy);
                    if(other.block() == block){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void decoration(float chance){
        pass((x, y) -> {
            for(int i = 0; i < 4; i++){
                Tile near = world.tile(x + Geometry.d4[i].x, y + Geometry.d4[i].y);
                if(near != null && near.block() != Blocks.air){
                    return;
                }
            }

            if(rand.chance(chance) && floor.asFloor().hasSurface() && block == Blocks.air){
                block = floor.asFloor().decoration;
            }
        });
    }

    public void blend(Block floor, Block around, float radius){
        float r2 = radius*radius;
        int cap = Mathf.ceil(radius);
        int max = tiles.width * tiles.height;
        Floor dest = around.asFloor();

        for(int i = 0; i < max; i++){
            Tile tile = tiles.geti(i);
            if(tile.floor() == floor || tile.block() == floor){
                for(int cx = -cap; cx <= cap; cx++){
                    for(int cy = -cap; cy <= cap; cy++){
                        if(cx*cx + cy*cy <= r2){
                            Tile other = tiles.get(tile.x + cx, tile.y + cy);

                            if(other != null && other.floor() != floor){
                                other.setFloor(dest);
                            }
                        }
                    }
                }
            }
        }
    }

    public void brush(Seq<Tile> path, int rad){
        path.each(tile -> erase(tile.x, tile.y, rad));
    }

    public void erase(int cx, int cy, int rad){
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                int wx = cx + x, wy = cy + y;
                if(Structs.inBounds(wx, wy, width, height) && Mathf.within(x, y, rad)){
                    Tile other = tiles.getn(wx, wy);
                    other.setBlock(Blocks.air);
                }
            }
        }
    }

    public Seq<Tile> pathfind(int startX, int startY, int endX, int endY, TileHueristic th, DistanceHeuristic dh){
        return Astar.pathfind(startX, startY, endX, endY, th, dh, tile -> world.getDarkness(tile.x, tile.y) <= 1f);
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
        GridBits used = new GridBits(tiles.width, tiles.height);

        IntSeq arr = new IntSeq();
        arr.add(start.pos());
        while(!arr.isEmpty()){
            int i = arr.pop();
            int x = Point2.x(i), y = Point2.y(i);
            used.set(x, y);
            for(Point2 point : Geometry.d4){
                int newx = x + point.x, newy = y + point.y;
                if(tiles.in(newx, newy)){
                    Tile child = tiles.getn(newx, newy);
                    if(child.block() == Blocks.air && !used.get(child.x, child.y)){
                        used.set(child.x, child.y);
                        arr.add(child.pos());
                    }
                }
            }
        }

        for(Tile tile : tiles){
            if(!used.get(tile.x, tile.y) && tile.block() == Blocks.air){
                tile.setBlock(tile.floor().wall);
            }
        }
    }
}
