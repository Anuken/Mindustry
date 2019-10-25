package io.anuke.mindustry.input;

import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Bresenham2;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.Vars.tilesize;

public class PlaceUtils{
    private static final NormalizeResult result = new NormalizeResult();
    private static final NormalizeDrawResult drawResult = new NormalizeDrawResult();
    private static Bresenham2 bres = new Bresenham2();
    private static Array<Point2> points = new Array<>();

    /** Normalize a diagonal line into points. */
    public static Array<Point2> normalizeDiagonal(int startX, int startY, int endX, int endY){
        Pools.freeAll(points);
        points.clear();
        return bres.lineNoDiagonal(startX, startY, endX, endY, Pools.get(Point2.class, Point2::new), points);
    }

    /** Normalize two points into one straight line, no diagonals. */
    public static Array<Point2> normalizeLine(int startX, int startY, int endX, int endY){
        Pools.freeAll(points);
        points.clear();
        if(Math.abs(startX - endX) > Math.abs(startY - endY)){
            //go width
            for(int i = 0; i <= Math.abs(startX - endX); i++){
                points.add(Pools.obtain(Point2.class, Point2::new).set(startX + i * Mathf.sign(endX - startX), startY));
            }
        }else{
            //go height
            for(int i = 0; i <= Math.abs(startY - endY); i++){
                points.add(Pools.obtain(Point2.class, Point2::new).set(startX, startY + i * Mathf.sign(endY - startY)));
            }
        }
        return points;
    }

    /**
     * Normalizes a placement area and returns the result, ready to be used for drawing a rectangle.
     * Returned x2 and y2 will <i>always</i> be greater than x and y.
     * @param block block that will be drawn
     * @param startx starting X coordinate
     * @param starty starting Y coordinate
     * @param endx ending X coordinate
     * @param endy ending Y coordinate
     * @param snap whether to snap to a line
     * @param maxLength maximum length of area
     */
    public static NormalizeDrawResult normalizeDrawArea(Block block, int startx, int starty, int endx, int endy, boolean snap, int maxLength, float scaling){
        normalizeArea(startx, starty, endx, endy, 0, snap, maxLength);

        float offset = block.offset();

        drawResult.x = result.x * tilesize;
        drawResult.y = result.y * tilesize;
        drawResult.x2 = result.x2 * tilesize;
        drawResult.y2 = result.y2 * tilesize;

        drawResult.x -= block.size * scaling * tilesize / 2;
        drawResult.x2 += block.size * scaling * tilesize / 2;


        drawResult.y -= block.size * scaling * tilesize / 2;
        drawResult.y2 += block.size * scaling * tilesize / 2;

        drawResult.x += offset;
        drawResult.y += offset;
        drawResult.x2 += offset;
        drawResult.y2 += offset;

        return drawResult;
    }

    /**
     * Normalizes a placement area and returns the result.
     * Returned x2 and y2 will <i>always</i> be greater than x and y.
     * @param tilex starting X coordinate
     * @param tiley starting Y coordinate
     * @param endx ending X coordinate
     * @param endy ending Y coordinate
     * @param snap whether to snap to a line
     * @param rotation placement rotation
     * @param maxLength maximum length of area
     */
    public static NormalizeResult normalizeArea(int tilex, int tiley, int endx, int endy, int rotation, boolean snap, int maxLength){

        if(snap){
            if(Math.abs(tilex - endx) > Math.abs(tiley - endy)){
                endy = tiley;
            }else{
                endx = tilex;
            }
        }

        if(Math.abs(endx - tilex) > maxLength){
            endx = Mathf.sign(endx - tilex) * maxLength + tilex;
        }

        if(Math.abs(endy - tiley) > maxLength){
            endy = Mathf.sign(endy - tiley) * maxLength + tiley;
        }

        int dx = endx - tilex, dy = endy - tiley;

        if(Math.abs(dx) > Math.abs(dy)){
            if(dx >= 0){
                rotation = 0;
            }else{
                rotation = 2;
            }
        }else if(Math.abs(dx) < Math.abs(dy)){
            if(dy >= 0){
                rotation = 1;
            }else{
                rotation = 3;
            }
        }

        if(endx < tilex){
            int t = endx;
            endx = tilex;
            tilex = t;
        }
        if(endy < tiley){
            int t = endy;
            endy = tiley;
            tiley = t;
        }

        result.x2 = endx;
        result.y2 = endy;
        result.x = tilex;
        result.y = tiley;
        result.rotation = rotation;

        return result;
    }

    public static class NormalizeDrawResult{
        float x, y, x2, y2;
    }

    public static class NormalizeResult{
        public int x, y, x2, y2, rotation;

        boolean isX(){
            return Math.abs(x2 - x) > Math.abs(y2 - y);
        }

        /**
         * Returns length of greater edge of the selection.
         */
        int getLength(){
            return Math.max(x2 - x, y2 - y);
        }

        /**
         * Returns the X position of a specific index along this area as a line.
         */
        int getScaledX(int i){
            return x + (x2 - x > y2 - y ? i : 0);
        }

        /**
         * Returns the Y position of a specific index along this area as a line.
         */
        int getScaledY(int i){
            return y + (x2 - x > y2 - y ? 0 : i);
        }
    }
}
