package arc.math.geom;

import arc.func.*;
import arc.math.*;
import arc.struct.*;

public final class Geometry{
    /** Points representing cardinal directions, starting at the left and going counter-clockwise. */
    public final static Point2[] d4 = {
        new Point2(1, 0),
        new Point2(0, 1),
        new Point2(-1, 0),
        new Point2(0, -1)
    };
    /** Points representing cardinal directions, starting at the left and going counter-clockwise. Also contains a center point: 0,0. */
    public final static Point2[] d4c = {
        new Point2(1, 0),
        new Point2(0, 1),
        new Point2(-1, 0),
        new Point2(0, -1),
        new Point2(0, 0)
    };
    public final static int[] d4x = {1, 0, -1, 0};
    public final static int[] d4y = {0, 1, 0, -1};
    public final static Point2[] d8 = {
        new Point2(1, 0),
        new Point2(1, 1),
        new Point2(0, 1),
        new Point2(-1, 1),
        new Point2(-1, 0),
        new Point2(-1, -1),
        new Point2(0, -1),
        new Point2(1, -1),
    };
    public final static Point2[] d8edge = {
        new Point2(1, 1),
        new Point2(-1, 1),
        new Point2(-1, -1),
        new Point2(1, -1)
    };
    private static final Vec2 tmp1 = new Vec2(), tmp2 = new Vec2(), tmp3 = new Vec2();

    public static Point2 d4(int i){
        return d4[Mathf.mod(i, 4)];
    }

    public static int d4x(int i){
        return d4x[Mathf.mod(i, 4)];
    }

    public static int d4y(int i){
        return d4y[Mathf.mod(i, 4)];
    }

    public static Point2 d8(int i){
        return d8[Mathf.mod(i, 8)];
    }

    public static Point2 d8edge(int i){
        return d8edge[Mathf.mod(i, 4)];
    }

    public static void circle(int x, int y, int radius, Intc2 cons){
        for(int dx = -radius; dx <= radius; dx++){
            for(int dy = -radius; dy <= radius; dy++){
                if(Mathf.within(dx, dy, radius)){
                    cons.get(dx + x, dy + y);
                }
            }
        }
    }

    public static void circle(int x, int y, int width, int height, int radius, Intc2 cons){
        for(int dx = Math.max(x - radius, 0); dx <= Math.min(x + radius, width - 1); dx++){
            for(int dy = Math.max(y - radius, 0); dy <= Math.min(y + radius, height - 1); dy++){
                if(Mathf.within(dx, dy, x, y, radius)){
                    cons.get(dx, dy);
                }
            }
        }
    }

    public static FloatSeq vectorsToFloats(Seq<Vec2> result){
        FloatSeq out = new FloatSeq(result.size * 2);
        result.each(v -> out.add(v.x, v.y));
        return out;
    }

    public static <T extends Position> T findClosest(float x, float y, T[] list){
        T closest = null;
        float cdist = 0f;
        for(T t : list){
            float dst = t.dst2(x, y);
            if(closest == null || dst < cdist){
                closest = t;
                cdist = dst;
            }
        }
        return closest;
    }

    public static <T extends Position> T findClosest(float x, float y, Iterable<T> list){
        T closest = null;
        float cdist = 0f;
        for(T t : list){
            float dst = t.dst2(x, y);
            if(closest == null || dst < cdist){
                closest = t;
                cdist = dst;
            }
        }
        return closest;
    }

    public static <T extends Position> T findClosest(float x, float y, Iterable<T> list, Boolf<T> predicate){
        T closest = null;
        float cdist = 0f;
        for(T t : list){
            if(!predicate.get(t)) continue;
            float dst = t.dst2(x, y);
            if(closest == null || dst < cdist){
                closest = t;
                cdist = dst;
            }
        }
        return closest;
    }

    public static <T extends Position> T findFurthest(float x, float y, Iterable<T> list){
        T furthest = null;
        float fdist = 0f;
        for(T t : list){
            float dst = t.dst2(x, y);
            if(furthest == null || dst > fdist){
                furthest = t;
                fdist = dst;
            }
        }
        return furthest;
    }

    public static <T extends Position> T findFurthest(float x, float y, Iterable<T> list, Boolf<T> predicate){
        T furthest = null;
        float fdist = 0f;
        for(T t : list){
            if(!predicate.get(t)) continue;
            float dst = t.dst2(x, y);
            if(furthest == null || dst > fdist){
                furthest = t;
                fdist = dst;
            }
        }
        return furthest;
    }

    public static Vec2[] pixelCircle(float tindex){
        return pixelCircle(tindex, (index, x, y) -> Mathf.dst(x, y, index, index) < index - 0.5f);
    }

    public static Vec2[] pixelCircle(float index, SolidChecker checker){
        int size = (int)(index * 2);
        IntSeq ints = new IntSeq();

        //add edges (bottom left corner)
        for(int x = -1; x < size + 1; x++){
            for(int y = -1; y < size + 1; y++){
                if((checker.solid(index, x, y) || checker.solid(index, x - 1, y) || checker.solid(index, x, y - 1) || checker.solid(index, x - 1, y - 1)) &&
                !(checker.solid(index, x, y) && checker.solid(index, x - 1, y) && checker.solid(index, x, y - 1) && checker.solid(index, x - 1, y - 1))){
                    ints.add(x + y * (size + 1));
                }
            }
        }

        Seq<Vec2> path = new Seq<>();

        //size = 3 needs special sorting due to conflicts
        if(size == 3){
            //int arrays don't support sorting with custom comparators, so box them.
            Seq<Integer> boxed = new Seq<>();
            ints.each(boxed::add);
            boxed.sort(i -> Angles.angle(i % (size + 1), i / (size + 1), index, index));
            ints.clear();
            boxed.each(ints::add);
        }

        int cindex = 0;
        while(ints.size > 0){
            int x = ints.get(cindex) % (size + 1);
            int y = ints.get(cindex) / (size + 1);
            path.add(new Vec2(x - size / 2, y - size / 2));
            ints.removeIndex(cindex);

            //find nearby edge
            for(int i = 0; i < ints.size; i++){

                int x2 = ints.get(i) % (size + 1);
                int y2 = ints.get(i) / (size + 1);
                if(Math.abs(x2 - x) <= 1 && Math.abs(y2 - y) <= 1 &&
                !(Math.abs(x2 - x) == 1 && Math.abs(y2 - y) == 1)){
                    cindex = i;
                    break;
                }
            }
        }

        return path.toArray(Vec2.class);
    }

    /** returns a regular polygon with {amount} sides */
    public static float[] regPoly(int amount, float size){
        float[] v = new float[amount * 2];
        Vec2 vec = new Vec2(1, 1);
        vec.setLength(size);
        for(int i = 0; i < amount; i++){
            vec.setAngle((360f / amount) * i + 90);
            v[i * 2] = vec.x;
            v[i * 2 + 1] = vec.y;
        }
        return v;
    }

    public static float iterateLine(float start, float x1, float y1, float x2, float y2, float segment, Floatc2 pos){
        float len = Mathf.dst(x1, y1, x2, y2);
        int steps = (int)(len / segment);
        float step = 1f / steps;

        float offset = len;
        tmp2.set(x2, y2);
        for(int i = 0; i < steps; i++){
            float s = step * i;
            tmp1.set(x1, y1);
            tmp1.lerp(tmp2, s);
            pos.get(tmp1.x, tmp1.y);
            offset -= step;
        }

        return offset;
    }

    public static void iteratePolySegments(float[] vertices, Floatc4 it){
        for(int i = 0; i < vertices.length; i += 2){
            float x = vertices[i];
            float y = vertices[i + 1];
            float x2, y2;
            if(i == vertices.length - 2){
                x2 = vertices[0];
                y2 = vertices[1];
            }else{
                x2 = vertices[i + 2];
                y2 = vertices[i + 3];
            }

            it.get(x, y, x2, y2);
        }
    }

    public static void iteratePolygon(Floatc2 path, float[] vertices){
        for(int i = 0; i < vertices.length; i += 2){
            float x = vertices[i];
            float y = vertices[i + 1];
            path.get(x, y);
        }
    }

    public static Point2[] getD4Points(){
        return d4;
    }

    public static Point2[] getD8Points(){
        return d8;
    }

    public static Point2[] getD8EdgePoints(){
        return d8edge;
    }

    public static boolean raycast(int x0f, int y0f, int x1, int y1, Raycaster cons){
        int x0 = x0f;
        int y0 = y0f;
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;
        while(true){

            if(cons.accept(x0, y0)) return true;
            if(x0 == x1 && y0 == y1) return false;

            e2 = 2 * err;
            if(e2 > -dy){
                err = err - dy;
                x0 = x0 + sx;
            }

            if(e2 < dx){
                err = err + dx;
                y0 = y0 + sy;
            }
        }
    }

    public static Vec2 raycastRect(float startx, float starty, float endx, float endy, Rect rect){
        return raycastRect(startx, starty, endx, endy, rect.x + rect.width / 2, rect.y + rect.height / 2, rect.width / 2f, rect.height / 2f);
    }

    public static Vec2 raycastRect(float startx, float starty, float endx, float endy, float x, float y, float halfx, float halfy){
        float deltax = endx - startx, deltay = endy - starty;

        Vec2 hit = tmp1;


        float scaleX = 1.0f / deltax;
        float scaleY = 1.0f / deltay;
        int signX = Mathf.sign(scaleX);
        int signY = Mathf.sign(scaleY);
        float nearTimeX = (x - signX * (halfx) - startx) * scaleX;
        float nearTimeY = (y - signY * (halfy) - starty) * scaleY;
        float farTimeX = (x + signX * (halfx) - startx) * scaleX;
        float farTimeY = (y + signY * (halfy) - starty) * scaleY;

        if(nearTimeX > farTimeY || nearTimeY > farTimeX)
            return null;

        float nearTime = Math.max(nearTimeX, nearTimeY);
        float farTime = Math.min(farTimeX, farTimeY);

        if(nearTime >= 1 || farTime <= 0)
            return null;

        float htime = Mathf.clamp(nearTime);
        float hdeltax = htime * deltax;
        float hdeltay = htime * deltay;
        hit.x = startx + hdeltax;
        hit.y = starty + hdeltay;
        return hit;
    }

    /**
     * Checks for collisions between two rectangles, and returns the correct delta vector of A.
     * Note: The same vector instance is returned each time!
     */
    public static Vec2 overlap(Rect a, Rect b, boolean x){
        float penetration = 0f;

        float ax = a.x + a.width / 2, bx = b.x + b.width / 2;
        float ay = a.y + a.height / 2, by = b.y + b.height / 2;

        //Vector from A to B
        float nx = ax - bx,
        ny = ay - by;

        // Calculate half extends along x axis
        float aex = a.width / 2,
        bex = b.width / 2;

        // Overlap on x axis
        float xoverlap = aex + bex - Math.abs(nx);
        if(Math.abs(xoverlap) > 0){

            // Calculate half extends along y axis
            float aey = a.height / 2,
            bey = b.height / 2;

            // Overlap on x axis
            float yoverlap = aey + bey - Math.abs(ny);
            if(Math.abs(yoverlap) > 0){

                // Find out which axis is the axis of least penetration
                if(Math.abs(xoverlap) < Math.abs(yoverlap)){
                    // Point towards B knowing that n points from A to B
                    tmp1.x = nx < 0 ? 1 : -1;
                    tmp1.y = 0;
                    penetration = xoverlap;
                }else{
                    // Point towards B knowing that n points from A to B
                    tmp1.x = 0;
                    tmp1.y = ny < 0 ? 1 : -1;
                    penetration = yoverlap;
                }
            }
        }

        float m = Math.max(penetration, 0.0f);

        // Apply correctional impulse
        float cx = m * tmp1.x,
        cy = m * tmp1.y;

        tmp1.x = -cx;
        tmp1.y = -cy;

        return tmp1;
    }

    /**
     * Computes the barycentric coordinates v,w for the specified point in the triangle.
     * <p>
     * If barycentric.x >= 0 && barycentric.y >= 0 && barycentric.x + barycentric.y <= 1 then the point is inside the triangle.
     * <p>
     * If vertices a,b,c have values aa,bb,cc then to get an interpolated value at point p:
     *
     * <pre>
     * Geometry.barycentric(p, a, b, c, barycentric);
     * float u = 1.f - barycentric.x - barycentric.y;
     * float x = u * aa.x + barycentric.x * bb.x + barycentric.y * cc.x;
     * float y = u * aa.y + barycentric.x * bb.y + barycentric.y * cc.y;
     * </pre>
     * @return barycentricOut
     */
    public static Vec2 toBarycoord(Vec2 p, Vec2 a, Vec2 b, Vec2 c, Vec2 barycentricOut){
        Vec2 v0 = tmp1.set(b).sub(a);
        Vec2 v1 = tmp2.set(c).sub(a);
        Vec2 v2 = tmp3.set(p).sub(a);
        float d00 = v0.dot(v0);
        float d01 = v0.dot(v1);
        float d11 = v1.dot(v1);
        float d20 = v2.dot(v0);
        float d21 = v2.dot(v1);
        float denom = d00 * d11 - d01 * d01;
        barycentricOut.x = (d11 * d20 - d01 * d21) / denom;
        barycentricOut.y = (d00 * d21 - d01 * d20) / denom;
        return barycentricOut;
    }

    /** Returns true if the barycentric coordinates are inside the triangle. */
    public static boolean barycoordInsideTriangle(Vec2 barycentric){
        return barycentric.x >= 0 && barycentric.y >= 0 && barycentric.x + barycentric.y <= 1;
    }

    /**
     * Returns interpolated values given the barycentric coordinates of a point in a triangle and the values at each vertex.
     * @return interpolatedOut
     */
    public static Vec2 fromBarycoord(Vec2 barycentric, Vec2 a, Vec2 b, Vec2 c, Vec2 interpolatedOut){
        float u = 1 - barycentric.x - barycentric.y;
        interpolatedOut.x = u * a.x + barycentric.x * b.x + barycentric.y * c.x;
        interpolatedOut.y = u * a.y + barycentric.x * b.y + barycentric.y * c.y;
        return interpolatedOut;
    }

    /**
     * Returns an interpolated value given the barycentric coordinates of a point in a triangle and the values at each vertex.
     * @return interpolatedOut
     */
    public static float fromBarycoord(Vec2 barycentric, float a, float b, float c){
        float u = 1 - barycentric.x - barycentric.y;
        return u * a + barycentric.x * b + barycentric.y * c;
    }

    /**
     * Returns the lowest positive root of the quadric equation given by a* x * x + b * x + c = 0. If no solution is given
     * Float.Nan is returned.
     * @param a the first coefficient of the quadric equation
     * @param b the second coefficient of the quadric equation
     * @param c the third coefficient of the quadric equation
     * @return the lowest positive root or Float.Nan
     */
    public static float lowestPositiveRoot(float a, float b, float c){
        float det = b * b - 4 * a * c;
        if(det < 0) return Float.NaN;

        float sqrtD = (float)Math.sqrt(det);
        float invA = 1 / (2 * a);
        float r1 = (-b - sqrtD) * invA;
        float r2 = (-b + sqrtD) * invA;

        if(r1 > r2){
            float tmp = r2;
            r2 = r1;
            r1 = tmp;
        }

        if(r1 > 0) return r1;
        if(r2 > 0) return r2;
        return Float.NaN;
    }

    public static boolean colinear(float x1, float y1, float x2, float y2, float x3, float y3){
        float dx21 = x2 - x1, dy21 = y2 - y1;
        float dx32 = x3 - x2, dy32 = y3 - y2;
        float det = dx32 * dy21 - dx21 * dy32;
        return Math.abs(det) < Mathf.FLOAT_ROUNDING_ERROR;
    }

    public static Vec2 triangleCentroid(float x1, float y1, float x2, float y2, float x3, float y3, Vec2 centroid){
        centroid.x = (x1 + x2 + x3) / 3;
        centroid.y = (y1 + y2 + y3) / 3;
        return centroid;
    }

    /** Returns the circumcenter of the triangle. The input points must not be colinear. */
    public static Vec2 triangleCircumcenter(float x1, float y1, float x2, float y2, float x3, float y3, Vec2 circumcenter){
        float dx21 = x2 - x1, dy21 = y2 - y1;
        float dx32 = x3 - x2, dy32 = y3 - y2;
        float dx13 = x1 - x3, dy13 = y1 - y3;
        float det = dx32 * dy21 - dx21 * dy32;
        if(Math.abs(det) < Mathf.FLOAT_ROUNDING_ERROR)
            throw new IllegalArgumentException("Triangle points must not be colinear.");
        det *= 2;
        float sqr1 = x1 * x1 + y1 * y1, sqr2 = x2 * x2 + y2 * y2, sqr3 = x3 * x3 + y3 * y3;
        circumcenter.set((sqr1 * dy32 + sqr2 * dy13 + sqr3 * dy21) / det, -(sqr1 * dx32 + sqr2 * dx13 + sqr3 * dx21) / det);
        return circumcenter;
    }

    public static float triangleCircumradius(float x1, float y1, float x2, float y2, float x3, float y3){
        float m1, m2, mx1, mx2, my1, my2, x, y;
        if(Math.abs(y2 - y1) < Mathf.FLOAT_ROUNDING_ERROR){
            m2 = -(x3 - x2) / (y3 - y2);
            mx2 = (x2 + x3) / 2;
            my2 = (y2 + y3) / 2;
            x = (x2 + x1) / 2;
            y = m2 * (x - mx2) + my2;
        }else if(Math.abs(y3 - y2) < Mathf.FLOAT_ROUNDING_ERROR){
            m1 = -(x2 - x1) / (y2 - y1);
            mx1 = (x1 + x2) / 2;
            my1 = (y1 + y2) / 2;
            x = (x3 + x2) / 2;
            y = m1 * (x - mx1) + my1;
        }else{
            m1 = -(x2 - x1) / (y2 - y1);
            m2 = -(x3 - x2) / (y3 - y2);
            mx1 = (x1 + x2) / 2;
            mx2 = (x2 + x3) / 2;
            my1 = (y1 + y2) / 2;
            my2 = (y2 + y3) / 2;
            x = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
            y = m1 * (x - mx1) + my1;
        }
        float dx = x1 - x, dy = y1 - y;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Ratio of circumradius to shortest edge as a measure of triangle quality.
     * <p>
     * Gary L. Miller, Dafna Talmor, Shang-Hua Teng, and Noel Walkington. A Delaunay Based Numerical Method for Three Dimensions:
     * Generation, Formulation, and Partition.
     */
    public static float triangleQuality(float x1, float y1, float x2, float y2, float x3, float y3){
        float length1 = (float)Math.sqrt(x1 * x1 + y1 * y1);
        float length2 = (float)Math.sqrt(x2 * x2 + y2 * y2);
        float length3 = (float)Math.sqrt(x3 * x3 + y3 * y3);
        return Math.min(length1, Math.min(length2, length3)) / triangleCircumradius(x1, y1, x2, y2, x3, y3);
    }

    public static float triangleArea(float x1, float y1, float x2, float y2, float x3, float y3){
        return Math.abs((x1 - x3) * (y2 - y1) - (x1 - x2) * (y3 - y1)) * 0.5f;
    }

    public static Vec2 quadrilateralCentroid(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
                                             Vec2 centroid){
        float avgX1 = (x1 + x2 + x3) / 3;
        float avgY1 = (y1 + y2 + y3) / 3;
        float avgX2 = (x1 + x4 + x3) / 3;
        float avgY2 = (y1 + y4 + y3) / 3;
        centroid.x = avgX1 - (avgX1 - avgX2) / 2;
        centroid.y = avgY1 - (avgY1 - avgY2) / 2;
        return centroid;
    }

    /** Returns the centroid for the specified non-self-intersecting polygon. */
    public static Vec2 polygonCentroid(float[] polygon, int offset, int count, Vec2 centroid){
        if(count < 6) throw new IllegalArgumentException("A polygon must have 3 or more coordinate pairs.");
        float x = 0, y = 0;

        float signedArea = 0;
        int i = offset;
        for(int n = offset + count - 2; i < n; i += 2){
            float x0 = polygon[i];
            float y0 = polygon[i + 1];
            float x1 = polygon[i + 2];
            float y1 = polygon[i + 3];
            float a = x0 * y1 - x1 * y0;
            signedArea += a;
            x += (x0 + x1) * a;
            y += (y0 + y1) * a;
        }

        float x0 = polygon[i];
        float y0 = polygon[i + 1];
        float x1 = polygon[offset];
        float y1 = polygon[offset + 1];
        float a = x0 * y1 - x1 * y0;
        signedArea += a;
        x += (x0 + x1) * a;
        y += (y0 + y1) * a;

        if(signedArea == 0){
            centroid.x = 0;
            centroid.y = 0;
        }else{
            signedArea *= 0.5f;
            centroid.x = x / (6 * signedArea);
            centroid.y = y / (6 * signedArea);
        }
        return centroid;
    }

    /** Computes the area for a convex polygon. */
    public static float polygonArea(float[] polygon, int offset, int count){
        float area = 0;
        for(int i = offset, n = offset + count; i < n; i += 2){
            int y1 = i + 1;
            int x2 = (i + 2) % n;
            if(x2 < offset) x2 += offset;
            int y2 = (i + 3) % n;
            if(y2 < offset) y2 += offset;
            area += polygon[i] * polygon[y2];
            area -= polygon[x2] * polygon[y1];
        }
        area *= 0.5f;
        return area;
    }

    public static void ensureCCW(float[] polygon){
        ensureCCW(polygon, 0, polygon.length);
    }

    public static void ensureCCW(float[] polygon, int offset, int count){
        if(!isClockwise(polygon, offset, count)) return;
        int lastX = offset + count - 2;
        for(int i = offset, n = offset + count / 2; i < n; i += 2){
            int other = lastX - i;
            float x = polygon[i];
            float y = polygon[i + 1];
            polygon[i] = polygon[other];
            polygon[i + 1] = polygon[other + 1];
            polygon[other] = x;
            polygon[other + 1] = y;
        }
    }

    public static boolean isClockwise(float[] polygon, int offset, int count){
        if(count <= 2) return false;
        float area = 0, p1x, p1y, p2x, p2y;
        for(int i = offset, n = offset + count - 3; i < n; i += 2){
            p1x = polygon[i];
            p1y = polygon[i + 1];
            p2x = polygon[i + 2];
            p2y = polygon[i + 3];
            area += p1x * p2y - p2x * p1y;
        }
        p1x = polygon[offset + count - 2];
        p1y = polygon[offset + count - 1];
        p2x = polygon[offset];
        p2y = polygon[offset + 1];
        return area + p1x * p2y - p2x * p1y < 0;
    }

    public interface Raycaster{
        boolean accept(int x, int y);
    }

    public interface SolidChecker{
        boolean solid(float index, int x, int y);
    }
}
