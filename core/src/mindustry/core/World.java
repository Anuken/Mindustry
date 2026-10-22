package mindustry.core;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.Geometry.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import java.util.*;

import static mindustry.Vars.*;

/** Container for tile data. */
public class World implements Iterable<Tile>{
    public final int width, height;
    public final float unitWidth, unitHeight;

    final Tile[] array;
    final Puddle[] puddles;
    final Fire[] fires;
    @Nullable long[] tmpFloorState, tmpBlockState;

    /** The number of times tiles have changed in this session. Used for blocks that need to poll world state, but not frequently. */
    public int tileChanges = 1, floorChanges = 1;

    public World(int width, int height){
        this.array = new Tile[width * height];
        this.width = width;
        this.height = height;
        this.unitWidth = width * tilesize;
        this.unitHeight = height * tilesize;
        this.puddles = new Puddle[width * height];
        this.fires = new Fire[width * height];
    }

    public World(){
        this(0, 0);
    }


    public long getTmpFloorState(int pos){
        return tmpFloorState == null ? 0 : tmpFloorState[pos];
    }

    public void setTmpFloorState(int pos, long value){
        if(tmpFloorState == null || tmpFloorState.length != array.length) tmpFloorState = new long[array.length];
        tmpFloorState[pos] = value;
    }

    public long getTmpBlockState(int pos){
        return tmpBlockState == null ? 0 : tmpBlockState[pos];
    }

    public void setTmpBlockState(int pos, long value){
        if(tmpBlockState == null || tmpBlockState.length != array.length) tmpBlockState = new long[array.length];
        tmpBlockState[pos] = value;
    }

    public Puddle getPuddle(int pos){
        return puddles[pos];
    }

    public void setPuddle(int pos, Puddle p){
        puddles[pos] = p;
    }

    public @Nullable Fire getFire(int pos){
        return fires[pos];
    }

    public void setFire(int pos, Fire f){
        fires[pos] = f;
    }

    public void each(Intc2 cons){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                cons.get(x, y);
            }
        }
    }

    /** fills this tile set with empty air  */
    public void fill(){
        for(int i = 0; i < array.length; i++){
            array[i] = new Tile(i % width, i / width);
        }
    }

    /** set a tile at a position; does not range-check. use with caution. */
    public void set(int x, int y, Tile tile){
        array[y*width + x] = tile;
    }

    /** set a tile at a raw array position; used for fast iteration / 1-D for-loops */
    public void seti(int i, Tile tile){
        array[i] = tile;
    }

    /** @return whether these coordinates are in bounds */
    public boolean in(int x, int y){
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /** @return a tile at coordinates, or null if out of bounds */
    @Nullable
    public Tile tile(int x, int y){
        return (x < 0 || x >= width || y < 0 || y >= height) ? null : array[y*width + x];
    }

    /** @return a tile at coordinates; throws an exception if out of bounds */
    public Tile getn(int x, int y){
        if(x < 0 || x >= width || y < 0 || y >= height) throw new IllegalArgumentException(x + ", " + y + " out of bounds: width=" + width + ", height=" + height);
        return array[y*width + x];
    }

    /** @return a tile at coordinates, clamped. */
    public Tile getc(int x, int y){
        x = Mathf.clamp(x, 0, width - 1);
        y = Mathf.clamp(y, 0, height - 1);
        return array[y*width + x];
    }

    /** @return a tile at an iteration index [0, width * height] */
    public Tile geti(int idx){
        return array[idx];
    }

    /** @return a tile at an int position (not equivalent to geti) */
    public @Nullable Tile getp(int pos){
        return tile(Point2.x(pos), Point2.y(pos));
    }

    public void eachTile(Cons<Tile> cons){
        for(Tile tile : array){
            cons.get(tile);
        }
    }

    @Override
    public Iterator<Tile> iterator(){
        //iterating through the entire map is expensive anyway, so a new allocation doesn't make much of a difference
        return new TileIterator();
    }

    public boolean solid(int x, int y){
        Tile tile = tile(x, y);

        return tile == null || tile.solid();
    }

    public boolean wallSolid(int x, int y){
        Tile tile = tile(x, y);
        return tile == null || tile.block().solid;
    }

    public boolean isAccessible(int x, int y){
        return !wallSolid(x, y - 1) || !wallSolid(x, y + 1) || !wallSolid(x - 1, y) || !wallSolid(x + 1, y);
    }

    public Floor floor(int x, int y){
        Tile tile = tile(x, y);
        return tile == null ? Blocks.air.asFloor() : tile.floor();
    }

    public Floor floorWorld(float x, float y){
        Tile tile = tileWorld(x, y);
        return tile == null ? Blocks.air.asFloor() : tile.floor();
    }

    @Nullable
    public Tile tile(int pos){
        return tile(Point2.x(pos), Point2.y(pos));
    }

    @Nullable
    public Tile tileBuilding(int x, int y){
        Tile tile = tile(x, y);
        if(tile == null) return null;
        if(tile.build != null){
            return tile.build.tile;
        }
        return tile;
    }

    public @Nullable Building build(int x, int y){
        Tile tile = tile(x, y);
        if(tile == null) return null;
        return tile.build;
    }

    public @Nullable Building build(int pos){
        Tile tile = tile(pos);
        if(tile == null) return null;
        return tile.build;
    }

    public Tile rawTile(int x, int y){
        return getn(x, y);
    }

    public @Nullable Tile tileWorld(float x, float y){
        return tile(Math.round(x / tilesize), Math.round(y / tilesize));
    }

    public @Nullable Building buildWorld(float x, float y){
        return build(Math.round(x / tilesize), Math.round(y / tilesize));
    }

    public @Nullable Building buildWorld(Position pos){
        return buildWorld(pos.getX(), pos.getY());
    }

    /** Convert from world to logic tile coordinates. Whole numbers are at centers of  */
    public static float conv(float coord){
        return coord / tilesize;
    }

    /** Convert from tile to world coordinates. */
    public static float unconv(float coord){
        return coord * tilesize;
    }

    public static int toTile(float coord){
        return Math.round(coord / tilesize);
    }

    public int packArray(int x, int y){
        return x + y * width;
    }

    public void clearBuildings(){
        for(Tile tile : array){
            if(tile != null && tile.build != null){
                tile.build.remove();
            }
        }
    }

    public Rect getQuadBounds(Rect in){
        return in.set(-finalWorldBounds, -finalWorldBounds, width * tilesize + finalWorldBounds * 2, height * tilesize + finalWorldBounds * 2);
    }

    /** @return whether the coordinates are inside the map's defined limit rect. */
    public boolean isInMapArea(int x, int y){
        return in(x, y) && (!state.rules.limitMapArea || Rect.contains(state.rules.limitX, state.rules.limitY, state.rules.limitWidth, state.rules.limitHeight, x, y));
    }

    public byte getWallDarkness(Tile tile){
        if(tile.isDarkened()){
            int minDst = darkRadius + 1;
            for(int cx = tile.x - darkRadius; cx <= tile.x + darkRadius; cx++){
                for(int cy = tile.y - darkRadius; cy <= tile.y + darkRadius; cy++){
                    if(in(cx, cy) && !rawTile(cx, cy).isDarkened()){
                        minDst = Math.min(minDst, Math.abs(cx - tile.x) + Math.abs(cy - tile.y));
                    }
                }
            }

            return (byte)Math.max((minDst - 1), 0);
        }
        return 0;
    }

    public void checkMapArea(int x, int y, int w, int h){
        for(var team : state.teams.present){
            for(var build : team.buildings){
                //reset map-area-based disabled blocks that were not in the previous map area
                if(!build.enabled && build.block.autoResetEnabled && !Rect.contains(x, y, w, h, build.tile.x, build.tile.y)){
                    build.enabled = true;
                }

                //if the map area contracts, disable the block
                build.checkAllowUpdate();
            }
        }
    }

    //TODO optimize; this is very slow and called too often!
    public float getDarkness(int x, int y){
        float dark = 0;

        if(Vars.state.rules.borderDarkness){
            int edgeBlend = 2;
            int edgeDst;

            if(!state.rules.limitMapArea){
                edgeDst = Math.min(x, Math.min(y, Math.min(-(x - (width - 1)), -(y - (height - 1)))));
            }else{
                edgeDst =
                    Math.min(x - state.rules.limitX,
                    Math.min(y - state.rules.limitY,
                    Math.min(-(x - (state.rules.limitX + state.rules.limitWidth - 1)), -(y - (state.rules.limitY + state.rules.limitHeight - 1)))));
            }

            if(edgeDst <= edgeBlend){
                dark = Math.max((edgeBlend - edgeDst) * (4f / edgeBlend), dark);
            }
        }

        if(state.hasSector() && state.getSector().preset == null){
            int circleBlend = 5;
            //quantized angle
            float offset = state.getSector().rect.rotation + 90;
            float angle = Angles.angle(x, y, width/2, height/2) + offset;
            //polygon sides, depends on sector
            int sides = state.getSector().tile.corners.length;
            float step = 360f / sides;
            //prev and next angles of poly
            float prev = Mathf.round(angle, step);
            float next = prev + step;
            //raw line length to be translated
            float length = state.getSector().getSize()/2f;
            float rawDst = Intersector.distanceLinePoint(Tmp.v1.trns(prev, length), Tmp.v2.trns(next, length), Tmp.v3.set(x - width/2, y - height/2).rotate(offset)) / Mathf.sqrt3 - 1;

            //noise
            rawDst += Noise.noise(x, y, 11f, 7f) + Noise.noise(x, y, 22f, 15f);

            int circleDst = (int)(rawDst - (length - circleBlend));
            if(circleDst > 0){
                dark = Math.max(circleDst, dark);
            }
        }

        Tile tile = tile(x, y);
        if(tile != null && tile.isDarkened()){
            dark = Math.max(dark, tile.data);
        }

        return dark;
    }

    public void applyDarkness(){
        byte[] dark = new byte[width * height];
        byte[] writeBuffer = new byte[width * height];

        byte darkIterations = darkRadius;

        for(int i = 0; i < dark.length; i++){
            Tile tile = array[i];
            if(tile.isDarkened()){
                dark[i] = darkIterations;
            }
        }

        for(int i = 0; i < darkIterations; i++){
            for(Tile tile : array){
                int idx = tile.y * width + tile.x;
                boolean min = false;
                for(Point2 point : Geometry.d4){
                    int newX = tile.x + point.x, newY = tile.y + point.y;
                    int nidx = newY * width + newX;
                    if(in(newX, newY) && dark[nidx] < dark[idx]){
                        min = true;
                        break;
                    }
                }
                writeBuffer[idx] = (byte)Math.max(0, dark[idx] - Mathf.num(min));
            }

            System.arraycopy(writeBuffer, 0, dark, 0, writeBuffer.length);
        }

        for(Tile tile : array){
            int idx = tile.y * width + tile.x;

            if(tile.isDarkened()){
                tile.data = dark[idx];
            }

            if(dark[idx] == darkRadius){
                boolean full = true;
                for(Point2 p : Geometry.d4){
                    int px = p.x + tile.x, py = p.y + tile.y;
                    int nidx = py * width + px;
                    if(in(px, py) && !(tile.isDarkened() && dark[nidx] == 4)){
                        full = false;
                        break;
                    }
                }

                if(full) tile.data = darkRadius + 1;
            }
        }
    }

    public static void raycastEachWorld(float x0, float y0, float x1, float y1, Raycaster cons){
        raycastEach(toTile(x0), toTile(y0), toTile(x1), toTile(y1), cons);
    }

    public static void raycastEach(int x1, int y1, int x2, int y2, Raycaster cons){
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(true){
            if(cons.accept(x, y)) break;
            if(x == x2 && y == y2) break;

            e2 = 2 * err;
            if(e2 > -dy){
                err -= dy;
                x += sx;
            }

            if(e2 < dx){
                err += dx;
                y += sy;
            }
        }
    }

    public static void raycastEachNoDiagonalWorld(float x0, float y0, float x1, float y1, Raycaster cons){
        raycastEachNoDiagonal(toTile(x0), toTile(y0), toTile(x1), toTile(y1), cons);
    }

    public static void raycastEachNoDiagonal(int startX, int startY, int endX, int endY, Raycaster cons){
        int xDist = Math.abs(endX - startX);
        int yDist = -Math.abs(endY - startY);
        int xStep = (startX < endX ? +1 : -1);
        int yStep = (startY < endY ? +1 : -1);
        int error = xDist + yDist;

        while(true){
            if(cons.accept(startX, startY) || (startX == endX && startY == endY)) break;

            if(2 * error - yDist > xDist - 2 * error){
                error += yDist;
                startX += xStep;
            }else{
                error += xDist;
                startY += yStep;
            }
        }
    }

    public static boolean raycast(int x1, int y1, int x2, int y2, Raycaster cons){
        return raycastHit(x1, y1, x2, y2, cons) != -1;
    }

    /** @return -1 if not hit, packed point if hit */
    public static int raycastHit(int x1, int y1, int x2, int y2, Raycaster cons){
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
        int e2, err = dx - dy;

        while(true){
            if(cons.accept(x, y)) return Point2.pack(x, y);
            if(x == x2 && y == y2) return -1;

            e2 = 2 * err;
            if(e2 > -dy){
                err = err - dy;
                x = x + sx;
            }

            if(e2 < dx){
                err = err + dx;
                y = y + sy;
            }
        }
    }

    private class TileIterator implements Iterator<Tile>{
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
