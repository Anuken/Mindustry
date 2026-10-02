package arc.math.geom;

import arc.math.*;
import arc.struct.*;

/**
 * Class offering various static methods for intersection testing between different geometric objects.
 * @author badlogicgames@gmail.com
 * @author jan.stria
 * @author Nathan Sweet
 */
public final class Intersector{
    private static final Vec3 v0 = new Vec3();
    private static final Vec3 v1 = new Vec3();
    private static final Vec3 v2 = new Vec3();
    private static final FloatSeq floatArray = new FloatSeq();
    private static final FloatSeq floatArray2 = new FloatSeq();
    private static final Vec2 ip = new Vec2();
    private static final Vec2 ep1 = new Vec2();
    private static final Vec2 ep2 = new Vec2();
    private static final Vec2 s = new Vec2();
    private static final Vec2 e = new Vec2();
    static Vec3 tmp = new Vec3();
    static Vec3 tmp1 = new Vec3();
    static Vec3 tmp2 = new Vec3();
    static Vec3 tmp3 = new Vec3();
    static Vec2 v2tmp = new Vec2();

    public static boolean intersectPolygons(float[] p1, float[] p2){
        // reusable points to trace edges around polygon
        floatArray2.clear();
        floatArray.clear();
        floatArray2.addAll(p1);
        if(p1.length == 0 || p2.length == 0){
            return false;
        }
        for(int i = 0; i < p2.length; i += 2){
            ep1.set(p2[i], p2[i + 1]);
            // wrap around to beginning of array if index points to end;
            if(i < p2.length - 2){
                ep2.set(p2[i + 2], p2[i + 3]);
            }else{
                ep2.set(p2[0], p2[1]);
            }
            if(floatArray2.size == 0){
                return false;
            }
            s.set(floatArray2.get(floatArray2.size - 2), floatArray2.get(floatArray2.size - 1));
            for(int j = 0; j < floatArray2.size; j += 2){
                e.set(floatArray2.get(j), floatArray2.get(j + 1));
                // determine if point is inside clip edge
                if(Intersector.pointLineSide(ep2, ep1, e) > 0){
                    if(!(Intersector.pointLineSide(ep2, ep1, s) > 0)){
                        Intersector.intersectLines(s, e, ep1, ep2, ip);
                        if(floatArray.size < 2 || floatArray.get(floatArray.size - 2) != ip.x
                        || floatArray.get(floatArray.size - 1) != ip.y){
                            floatArray.add(ip.x);
                            floatArray.add(ip.y);
                        }
                    }
                    floatArray.add(e.x);
                    floatArray.add(e.y);
                }else if(Intersector.pointLineSide(ep2, ep1, s) > 0){
                    Intersector.intersectLines(s, e, ep1, ep2, ip);
                    floatArray.add(ip.x);
                    floatArray.add(ip.y);
                }
                s.set(e.x, e.y);
            }
            floatArray2.clear();
            floatArray2.addAll(floatArray);
            floatArray.clear();
        }

        return !(floatArray2.size == 0);
    }

    /**
     * Returns whether the given point is inside the triangle. This assumes that the point is on the plane of the triangle. No
     * check is performed that this is the case.
     * @param point the point
     * @param t1 the first vertex of the triangle
     * @param t2 the second vertex of the triangle
     * @param t3 the third vertex of the triangle
     * @return whether the point is in the triangle
     */
    public static boolean isInTriangle(Vec3 point, Vec3 t1, Vec3 t2, Vec3 t3){
        v0.set(t1).sub(point);
        v1.set(t2).sub(point);
        v2.set(t3).sub(point);

        float ab = v0.dot(v1);
        float ac = v0.dot(v2);
        float bc = v1.dot(v2);
        float cc = v2.dot(v2);

        if(bc * ac - cc * ab < 0) return false;
        float bb = v1.dot(v1);
        return !(ab * bc - ac * bb < 0);
    }

    /** @return whether x,y is inside the hexagon with radius d centered at cx, cy. */
    public static boolean isInsideHexagon(float cx, float cy, float d, float x, float y){
        float dx = Math.abs(x - cx) / d;
        float dy = Math.abs(y - cy) / d;
        float a = 0.25f * Mathf.sqrt3;
        return (dy <= a) && (a * dx + 0.25 * dy <= 0.5 * a);
    }

    /** @return whether the specified x,y is inside a regular polygon. */
    public static boolean isInRegularPolygon(int sides, float cx, float cy, float radius, float rotation, float x, float y){
        floatArray.clear();
        for(int i = 0; i < sides; i++){
            s.trns(i * 360f / sides + rotation, radius);
            floatArray.add(cx + s.x, cy + s.y);
        }

        return isInPolygon(floatArray.items, 0, floatArray.size, x, y);
    }

    /** Returns true if the given point is inside the triangle. */
    public static boolean isInTriangle(Vec2 p, Vec2 a, Vec2 b, Vec2 c){
        float px1 = p.x - a.x;
        float py1 = p.y - a.y;
        boolean side12 = (b.x - a.x) * py1 - (b.y - a.y) * px1 > 0;
        if((c.x - a.x) * py1 - (c.y - a.y) * px1 > 0 == side12) return false;
        return (c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x) > 0 == side12;
    }

    /** Returns true if the given point is inside the triangle. */
    public static boolean isInTriangle(float px, float py, float ax, float ay, float bx, float by, float cx, float cy){
        float px1 = px - ax;
        float py1 = py - ay;
        boolean side12 = (bx - ax) * py1 - (by - ay) * px1 > 0;
        if((cx - ax) * py1 - (cy - ay) * px1 > 0 == side12) return false;
        return (cx - bx) * (py - by) - (cy - by) * (px - bx) > 0 == side12;
    }

    /**
     * Determines on which side of the given line the point is. Returns -1 if the point is on the left side of the line, 0 if the
     * point is on the line and 1 if the point is on the right side of the line. Left and right are relative to the lines direction
     * which is linePoint1 to linePoint2.
     */
    public static int pointLineSide(Vec2 linePoint1, Vec2 linePoint2, Vec2 point){
        return (int)Math.signum(
        (linePoint2.x - linePoint1.x) * (point.y - linePoint1.y) - (linePoint2.y - linePoint1.y) * (point.x - linePoint1.x));
    }

    public static int pointLineSide(float linePoint1X, float linePoint1Y, float linePoint2X, float linePoint2Y, float pointX, float pointY){
        return (int)Math
        .signum((linePoint2X - linePoint1X) * (pointY - linePoint1Y) - (linePoint2Y - linePoint1Y) * (pointX - linePoint1X));
    }

    /**
     * Checks whether the given point is in the polygon.
     * @param polygon The polygon vertices passed as an array
     * @param point The point
     * @return true if the point is in the polygon
     */
    public static boolean isInPolygon(Seq<Vec2> polygon, Vec2 point){
        Vec2 lastVertice = polygon.peek();
        boolean oddNodes = false;
        for(int i = 0; i < polygon.size; i++){
            Vec2 vertice = polygon.get(i);
            if((vertice.y < point.y && lastVertice.y >= point.y) || (lastVertice.y < point.y && vertice.y >= point.y)){
                if(vertice.x + (point.y - vertice.y) / (lastVertice.y - vertice.y) * (lastVertice.x - vertice.x) < point.x){
                    oddNodes = !oddNodes;
                }
            }
            lastVertice = vertice;
        }
        return oddNodes;
    }

    /**
     * Returns true if the specified point is in the polygon.
     * @param offset Starting polygon index.
     * @param count Number of array indices to use after offset.
     */
    public static boolean isInPolygon(float[] polygon, int offset, int count, float x, float y){
        boolean oddNodes = false;
        int j = offset + count - 2;
        for(int i = offset, n = j; i <= n; i += 2){
            float yi = polygon[i + 1];
            float yj = polygon[j + 1];
            if((yi < y && yj >= y) || (yj < y && yi >= y)){
                float xi = polygon[i];
                if(xi + (y - yi) / (yj - yi) * (polygon[j] - xi) < x) oddNodes = !oddNodes;
            }
            j = i;
        }
        return oddNodes;
    }

    /**
     * Intersects two convex polygons with clockwise vertices and sets the overlap polygon resulting from the intersection.
     * Follows the Sutherland-Hodgman algorithm.
     * @param p1 The polygon that is being clipped
     * @param p2 The clip polygon
     * @param overlap The intersection of the two polygons (can be null, if an intersection polygon is not needed)
     * @return Whether the two polygons intersect.
     */
    public static boolean intersectPolygons(Polygon p1, Polygon p2, Polygon overlap){
        if(p1.getVertices().length == 0 || p2.getVertices().length == 0){
            return false;
        }
        // reusable points to trace edges around polygon
        floatArray2.clear();
        floatArray.clear();
        floatArray2.addAll(p1.getTransformedVertices());
        for(int i = 0; i < p2.getTransformedVertices().length; i += 2){
            ep1.set(p2.getTransformedVertices()[i], p2.getTransformedVertices()[i + 1]);
            // wrap around to beginning of array if index points to end;
            if(i < p2.getTransformedVertices().length - 2){
                ep2.set(p2.getTransformedVertices()[i + 2], p2.getTransformedVertices()[i + 3]);
            }else{
                ep2.set(p2.getTransformedVertices()[0], p2.getTransformedVertices()[1]);
            }
            if(floatArray2.size == 0){
                return false;
            }
            s.set(floatArray2.get(floatArray2.size - 2), floatArray2.get(floatArray2.size - 1));
            for(int j = 0; j < floatArray2.size; j += 2){
                e.set(floatArray2.get(j), floatArray2.get(j + 1));
                // determine if point is inside clip edge
                if(Intersector.pointLineSide(ep2, ep1, e) > 0){
                    if(!(Intersector.pointLineSide(ep2, ep1, s) > 0)){
                        Intersector.intersectLines(s, e, ep1, ep2, ip);
                        if(floatArray.size < 2 || floatArray.get(floatArray.size - 2) != ip.x
                        || floatArray.get(floatArray.size - 1) != ip.y){
                            floatArray.add(ip.x);
                            floatArray.add(ip.y);
                        }
                    }
                    floatArray.add(e.x);
                    floatArray.add(e.y);
                }else if(Intersector.pointLineSide(ep2, ep1, s) > 0){
                    Intersector.intersectLines(s, e, ep1, ep2, ip);
                    floatArray.add(ip.x);
                    floatArray.add(ip.y);
                }
                s.set(e.x, e.y);
            }
            floatArray2.clear();
            floatArray2.addAll(floatArray);
            floatArray.clear();
        }
        if(floatArray2.size != 0){
            if(overlap != null){
                if(overlap.getVertices().length == floatArray2.size)
                    System.arraycopy(floatArray2.items, 0, overlap.getVertices(), 0, floatArray2.size);
                else
                    overlap.setVertices(floatArray2.toArray());
            }
            return true;
        }else{
            return false;
        }
    }

    /** Returns the distance between the given line and point. Note the specified line is not a line segment. */
    public static float distanceLinePoint(Vec2 start, Vec2 end, Vec2 point){
        return distanceLinePoint(start.x, start.y, end.x, end.y, point.x, point.y);
    }

    /** Returns the distance between the given line and point. Note the specified line is not a line segment. */
    public static float distanceLinePoint(float startX, float startY, float endX, float endY, float pointX, float pointY){
        float normalLength = (float)Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
        return Math.abs((pointX - startX) * (endY - startY) - (pointY - startY) * (endX - startX)) / normalLength;
    }

    /** Returns the distance between the given segment and point. */
    public static float distanceSegmentPoint(float startX, float startY, float endX, float endY, float pointX, float pointY){
        return nearestSegmentPoint(startX, startY, endX, endY, pointX, pointY, v2tmp).dst(pointX, pointY);
    }

    /** Returns the distance between the given segment and point. */
    public static float distanceSegmentPoint(Vec2 start, Vec2 end, Vec2 point){
        return nearestSegmentPoint(start, end, point, v2tmp).dst(point);
    }

    /** Returns a point on the segment nearest to the specified point. */
    public static Vec2 nearestSegmentPoint(Vec2 start, Vec2 end, Vec2 point, Vec2 nearest){
        float length2 = start.dst2(end);
        if(length2 == 0) return nearest.set(start);
        float t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / length2;
        if(t < 0) return nearest.set(start);
        if(t > 1) return nearest.set(end);
        return nearest.set(start.x + t * (end.x - start.x), start.y + t * (end.y - start.y));
    }

    /** Returns a point on the segment nearest to the specified point. */
    public static Vec2 nearestSegmentPoint(float startX, float startY, float endX, float endY, float pointX, float pointY,
                                           Vec2 nearest){
        final float xDiff = endX - startX;
        final float yDiff = endY - startY;
        float length2 = xDiff * xDiff + yDiff * yDiff;
        if(length2 == 0) return nearest.set(startX, startY);
        float t = ((pointX - startX) * (endX - startX) + (pointY - startY) * (endY - startY)) / length2;
        if(t < 0) return nearest.set(startX, startY);
        if(t > 1) return nearest.set(endX, endY);
        return nearest.set(startX + t * (endX - startX), startY + t * (endY - startY));
    }

    /**
     * Returns whether the given line segment intersects the given circle.
     * @param start The start point of the line segment
     * @param end The end point of the line segment
     * @param center The center of the circle
     * @param squareRadius The squared radius of the circle
     * @return Whether the line segment and the circle intersect
     */
    public static boolean intersectSegmentCircle(Vec2 start, Vec2 end, Vec2 center, float squareRadius){
        tmp.set(end.x - start.x, end.y - start.y, 0);
        tmp1.set(center.x - start.x, center.y - start.y, 0);
        float l = tmp.len();
        float u = tmp1.dot(tmp.nor());
        if(u <= 0){
            tmp2.set(start.x, start.y, 0);
        }else if(u >= l){
            tmp2.set(end.x, end.y, 0);
        }else{
            tmp3.set(tmp.scl(u)); // remember tmp is already normalized
            tmp2.set(tmp3.x + start.x, tmp3.y + start.y, 0);
        }

        float x = center.x - tmp2.x;
        float y = center.y - tmp2.y;

        return x * x + y * y <= squareRadius;
    }

    /**
     * Checks whether the line segment and the circle intersect and returns by how much and in what direction the line has to move
     * away from the circle to not intersect.
     * @param start The line segment starting point
     * @param end The line segment end point
     * @param point The center of the circle
     * @param radius The radius of the circle
     * @param displacement The displacement vector set by the method having unit length
     * @return The displacement or Float.POSITIVE_INFINITY if no intersection is present
     */
    public static float intersectSegmentCircleDisplace(Vec2 start, Vec2 end, Vec2 point, float radius,
                                                       Vec2 displacement){
        float u = (point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y);
        float d = start.dst(end);
        u /= d * d;
        if(u < 0 || u > 1) return Float.POSITIVE_INFINITY;
        tmp.set(end.x, end.y, 0).sub(start.x, start.y, 0);
        tmp2.set(start.x, start.y, 0).add(tmp.scl(u));
        d = tmp2.dst(point.x, point.y, 0);
        if(d < radius){
            displacement.set(point).sub(tmp2.x, tmp2.y).nor();
            return d;
        }else
            return Float.POSITIVE_INFINITY;
    }

    /**
     * Intersect two 2D Rays and return the scalar parameter of the first ray at the intersection point. You can get the
     * intersection point by: Vec2 point(direction1).scl(scalar).add(start1); For more information, check:
     * http://stackoverflow.com/a/565282/1091440
     * @param start1 Where the first ray start
     * @param direction1 The direction the first ray is pointing
     * @param start2 Where the second ray start
     * @param direction2 The direction the second ray is pointing
     * @return scalar parameter on the first ray describing the point where the intersection happens. May be negative. In case the
     * rays are collinear, Float.POSITIVE_INFINITY will be returned.
     */
    public static float intersectRayRay(Vec2 start1, Vec2 direction1, Vec2 start2, Vec2 direction2){
        float difx = start2.x - start1.x;
        float dify = start2.y - start1.y;
        float d1xd2 = direction1.x * direction2.y - direction1.y * direction2.x;
        if(d1xd2 == 0.0f){
            return Float.POSITIVE_INFINITY; // collinear
        }
        float d2sx = direction2.x / d1xd2;
        float d2sy = direction2.y / d1xd2;
        return difx * d2sy - dify * d2sx;
    }

    /**
     * Intersects the two lines and returns the intersection point in intersection.
     * @param p1 The first point of the first line
     * @param p2 The second point of the first line
     * @param p3 The first point of the second line
     * @param p4 The second point of the second line
     * @param intersection The intersection point. May be null.
     * @return Whether the two lines intersect
     */
    public static boolean intersectLines(Vec2 p1, Vec2 p2, Vec2 p3, Vec2 p4, Vec2 intersection){
        float x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y, x3 = p3.x, y3 = p3.y, x4 = p4.x, y4 = p4.y;

        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if(d == 0) return false;

        if(intersection != null){
            float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d;
            intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        }
        return true;
    }

    /**
     * Intersects the two lines and returns the intersection point in intersection.
     * @param intersection The intersection point, or null.
     * @return Whether the two lines intersect
     */
    public static boolean intersectLines(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vec2 intersection){
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if(d == 0) return false;

        if(intersection != null){
            float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d;
            intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        }
        return true;
    }

    /**
     * Check whether the given line and {@link Polygon} intersect.
     * @param p1 The first point of the line
     * @param p2 The second point of the line
     * @param polygon The polygon
     * @return Whether polygon and line intersects
     */
    public static boolean intersectLinePolygon(Vec2 p1, Vec2 p2, Polygon polygon){
        float[] vertices = polygon.getTransformedVertices();
        float x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;
        int n = vertices.length;
        float x3 = vertices[n - 2], y3 = vertices[n - 1];
        for(int i = 0; i < n; i += 2){
            float x4 = vertices[i], y4 = vertices[i + 1];
            float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
            if(d != 0){
                float yd = y1 - y3;
                float xd = x1 - x3;
                float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
                if(ua >= 0 && ua <= 1){
                    return true;
                }
            }
            x3 = x4;
            y3 = y4;
        }
        return false;
    }

    /**
     * Determines whether the given rectangles intersect and, if they do, sets the supplied {@code intersection} rectangle to the
     * area of overlap.
     * @return Whether the rectangles intersect
     */
    public static boolean intersectRectangles(Rect rect1, Rect rect2, Rect intersection){
        if(rect1.overlaps(rect2)){
            intersection.x = Math.max(rect1.x, rect2.x);
            intersection.width = Math.min(rect1.x + rect1.width, rect2.x + rect2.width) - intersection.x;
            intersection.y = Math.max(rect1.y, rect2.y);
            intersection.height = Math.min(rect1.y + rect1.height, rect2.y + rect2.height) - intersection.y;
            return true;
        }
        return false;
    }

    /** Experimental method! May be inaccurate, do not use.*/
    public static boolean intersectSegmentRectangleFast(float startx, float starty, float endx, float endy, float rectX, float rectY, float rectW, float rectH){
        float
        deltax = endx - startx,
        deltay = endy - starty,
        x = rectX + rectW / 2,
        y = rectY + rectH / 2,
        halfx = rectW / 2f,
        halfy = rectH / 2f;

        float scaleX = 1.0f / deltax;
        float scaleY = 1.0f / deltay;
        int signX = Mathf.sign(scaleX);
        int signY = Mathf.sign(scaleY);
        float nearTimeX = (x - signX * (halfx) - startx) * scaleX;
        float nearTimeY = (y - signY * (halfy) - starty) * scaleY;
        float farTimeX = (x + signX * (halfx) - startx) * scaleX;
        float farTimeY = (y + signY * (halfy) - starty) * scaleY;

        return nearTimeX < farTimeY && nearTimeY < farTimeX && Math.max(nearTimeX, nearTimeY) < 1 && Math.min(farTimeX, farTimeY) > 0;
    }

    /**
     * Determines whether the given rectangle and segment intersect
     * @param startX x-coordinate start of line segment
     * @param startY y-coordinate start of line segment
     * @param endX y-coordinate end of line segment
     * @param endY y-coordinate end of line segment
     * @return whether the rectangle intersects with the line segment
     */
    public static boolean intersectSegmentRectangle(float startX, float startY, float endX, float endY, float rectX, float rectY, float rectW, float rectH){
        float rectangleEndX = rectX + rectW;
        float rectangleEndY = rectY + rectH;

        return
            intersectSegments(startX, startY, endX, endY, rectX, rectY, rectX, rectangleEndY, null) ||
            intersectSegments(startX, startY, endX, endY, rectX, rectY, rectangleEndX, rectY, null) ||
            intersectSegments(startX, startY, endX, endY, rectangleEndX, rectY, rectangleEndX, rectangleEndY, null) ||
            intersectSegments(startX, startY, endX, endY, rectX, rectangleEndY, rectangleEndX, rectangleEndY, null) ||
            Rect.contains(rectX, rectY, rectW, rectH, startX, startY);
    }

    /**
     * Determines whether the given rectangle and segment intersect
     * @param startX x-coordinate start of line segment
     * @param startY y-coordinate start of line segment
     * @param endX y-coordinate end of line segment
     * @param endY y-coordinate end of line segment
     * @param rect rectangle that is being tested for collision
     * @return whether the rectangle intersects with the line segment
     */
    public static boolean intersectSegmentRectangle(float startX, float startY, float endX, float endY, Rect rect){
        float rectangleEndX = rect.x + rect.width;
        float rectangleEndY = rect.y + rect.height;

        if(intersectSegments(startX, startY, endX, endY, rect.x, rect.y, rect.x, rectangleEndY, null))
            return true;

        if(intersectSegments(startX, startY, endX, endY, rect.x, rect.y, rectangleEndX, rect.y, null))
            return true;

        if(intersectSegments(startX, startY, endX, endY, rectangleEndX, rect.y, rectangleEndX, rectangleEndY, null))
            return true;

        if(intersectSegments(startX, startY, endX, endY, rect.x, rectangleEndY, rectangleEndX, rectangleEndY, null))
            return true;

        return rect.contains(startX, startY);
    }

    /**
     * {@link #intersectSegmentRectangle(float, float, float, float, Rect)}
     */
    public static boolean intersectSegmentRectangle(Vec2 start, Vec2 end, Rect rect){
        return intersectSegmentRectangle(start.x, start.y, end.x, end.y, rect);
    }

    /**
     * Check whether the given line segment and {@link Polygon} intersect.
     * @param p1 The first point of the segment
     * @param p2 The second point of the segment
     * @return Whether polygon and segment intersect
     */
    public static boolean intersectSegmentPolygon(Vec2 p1, Vec2 p2, Polygon polygon){
        float[] vertices = polygon.getTransformedVertices();
        float x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y;
        int n = vertices.length;
        float x3 = vertices[n - 2], y3 = vertices[n - 1];
        for(int i = 0; i < n; i += 2){
            float x4 = vertices[i], y4 = vertices[i + 1];
            float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
            if(d != 0){
                float yd = y1 - y3;
                float xd = x1 - x3;
                float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
                if(ua >= 0 && ua <= 1){
                    float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
                    if(ub >= 0 && ub <= 1){
                        return true;
                    }
                }
            }
            x3 = x4;
            y3 = y4;
        }
        return false;
    }

    /**
     * Intersects the two line segments and returns the intersection point in intersection.
     * @param p1 The first point of the first line segment
     * @param p2 The second point of the first line segment
     * @param p3 The first point of the second line segment
     * @param p4 The second point of the second line segment
     * @param intersection The intersection point. May be null.
     * @return Whether the two line segments intersect
     */
    public static boolean intersectSegments(Vec2 p1, Vec2 p2, Vec2 p3, Vec2 p4, Vec2 intersection){
        float x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y, x3 = p3.x, y3 = p3.y, x4 = p4.x, y4 = p4.y;

        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if(d == 0) return false;

        float yd = y1 - y3;
        float xd = x1 - x3;
        float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
        if(ua < 0 || ua > 1) return false;

        float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
        if(ub < 0 || ub > 1) return false;

        if(intersection != null) intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        return true;
    }

    /** @param intersection May be null. */
    public static boolean intersectSegments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
                                            Vec2 intersection){
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if(d == 0) return false;

        float yd = y1 - y3;
        float xd = x1 - x3;
        float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
        if(ua < 0 || ua > 1) return false;

        float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
        if(ub < 0 || ub > 1) return false;

        if(intersection != null) intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        return true;
    }

    static float det(float a, float b, float c, float d){
        return a * d - b * c;
    }

    static double detd(double a, double b, double c, double d){
        return a * d - b * c;
    }

    public static boolean overlapsRect(float x1, float y1, float w1, float h1, float x2, float y2, float w2, float h2){
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    public static boolean overlaps(Circle c1, Circle c2){
        return c1.overlaps(c2);
    }

    public static boolean overlaps(Rect r1, Rect r2){
        return r1.overlaps(r2);
    }

    public static boolean overlaps(Circle c, Rect r){
        float closestX = c.x;
        float closestY = c.y;

        if(c.x < r.x){
            closestX = r.x;
        }else if(c.x > r.x + r.width){
            closestX = r.x + r.width;
        }

        if(c.y < r.y){
            closestY = r.y;
        }else if(c.y > r.y + r.height){
            closestY = r.y + r.height;
        }

        closestX = closestX - c.x;
        closestX *= closestX;
        closestY = closestY - c.y;
        closestY *= closestY;

        return closestX + closestY < c.radius * c.radius;
    }

    /**
     * Check whether specified counter-clockwise wound convex polygons overlap.
     * @param p1 The first polygon.
     * @param p2 The second polygon.
     * @return Whether polygons overlap.
     */
    public static boolean overlapConvexPolygons(Polygon p1, Polygon p2){
        return overlapConvexPolygons(p1, p2, null);
    }

    /**
     * Check whether specified counter-clockwise wound convex polygons overlap. If they do, optionally obtain a Minimum
     * Translation Vector indicating the minimum magnitude vector required to push the polygon p1 out of collision with polygon p2.
     * @param p1 The first polygon.
     * @param p2 The second polygon.
     * @param mtv A Minimum Translation Vector to fill in the case of a collision, or null (optional).
     * @return Whether polygons overlap.
     */
    public static boolean overlapConvexPolygons(Polygon p1, Polygon p2, MinimumTranslationVector mtv){
        return overlapConvexPolygons(p1.getTransformedVertices(), p2.getTransformedVertices(), mtv);
    }

    /** @see #overlapConvexPolygons(float[], int, int, float[], int, int, MinimumTranslationVector) */
    public static boolean overlapConvexPolygons(float[] verts1, float[] verts2, MinimumTranslationVector mtv){
        return overlapConvexPolygons(verts1, 0, verts1.length, verts2, 0, verts2.length, mtv);
    }

    /**
     * Check whether polygons defined by the given counter-clockwise wound vertex arrays overlap. If they do, optionally obtain a
     * Minimum Translation Vector indicating the minimum magnitude vector required to push the polygon defined by verts1 out of the
     * collision with the polygon defined by verts2.
     * @param verts1 Vertices of the first polygon.
     * @param verts2 Vertices of the second polygon.
     * @param mtv A Minimum Translation Vector to fill in the case of a collision, or null (optional).
     * @return Whether polygons overlap.
     */
    public static boolean overlapConvexPolygons(float[] verts1, int offset1, int count1, float[] verts2, int offset2, int count2,
                                                MinimumTranslationVector mtv){
        float overlap = Float.MAX_VALUE;
        float smallestAxisX = 0;
        float smallestAxisY = 0;
        int numInNormalDir;

        int end1 = offset1 + count1;
        int end2 = offset2 + count2;

        // Get polygon1 axes
        for(int i = offset1; i < end1; i += 2){
            float x1 = verts1[i];
            float y1 = verts1[i + 1];
            float x2 = verts1[(i + 2) % count1];
            float y2 = verts1[(i + 3) % count1];

            float axisX = y1 - y2;
            float axisY = -(x1 - x2);

            final float length = (float)Math.sqrt(axisX * axisX + axisY * axisY);
            axisX /= length;
            axisY /= length;

            // -- Begin check for separation on this axis --//

            // Project polygon1 onto this axis
            float min1 = axisX * verts1[0] + axisY * verts1[1];
            float max1 = min1;
            for(int j = offset1; j < end1; j += 2){
                float p = axisX * verts1[j] + axisY * verts1[j + 1];
                if(p < min1){
                    min1 = p;
                }else if(p > max1){
                    max1 = p;
                }
            }

            // Project polygon2 onto this axis
            numInNormalDir = 0;
            float min2 = axisX * verts2[0] + axisY * verts2[1];
            float max2 = min2;
            for(int j = offset2; j < end2; j += 2){
                // Counts the number of points that are within the projected area.
                numInNormalDir -= pointLineSide(x1, y1, x2, y2, verts2[j], verts2[j + 1]);
                float p = axisX * verts2[j] + axisY * verts2[j + 1];
                if(p < min2){
                    min2 = p;
                }else if(p > max2){
                    max2 = p;
                }
            }

            if(!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)){
                return false;
            }else{
                float o = Math.min(max1, max2) - Math.max(min1, min2);
                if(min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1){
                    float mins = Math.abs(min1 - min2);
                    float maxs = Math.abs(max1 - max2);
                    if(mins < maxs){
                        o += mins;
                    }else{
                        o += maxs;
                    }
                }
                if(o < overlap){
                    overlap = o;
                    // Adjusts the direction based on the number of points found
                    smallestAxisX = numInNormalDir >= 0 ? axisX : -axisX;
                    smallestAxisY = numInNormalDir >= 0 ? axisY : -axisY;
                }
            }
            // -- End check for separation on this axis --//
        }

        // Get polygon2 axes
        for(int i = offset2; i < end2; i += 2){
            float x1 = verts2[i];
            float y1 = verts2[i + 1];
            float x2 = verts2[(i + 2) % count2];
            float y2 = verts2[(i + 3) % count2];

            float axisX = y1 - y2;
            float axisY = -(x1 - x2);

            final float length = (float)Math.sqrt(axisX * axisX + axisY * axisY);
            axisX /= length;
            axisY /= length;

            // -- Begin check for separation on this axis --//
            numInNormalDir = 0;

            // Project polygon1 onto this axis
            float min1 = axisX * verts1[0] + axisY * verts1[1];
            float max1 = min1;
            for(int j = offset1; j < end1; j += 2){
                float p = axisX * verts1[j] + axisY * verts1[j + 1];
                // Counts the number of points that are within the projected area.
                numInNormalDir -= pointLineSide(x1, y1, x2, y2, verts1[j], verts1[j + 1]);
                if(p < min1){
                    min1 = p;
                }else if(p > max1){
                    max1 = p;
                }
            }

            // Project polygon2 onto this axis
            float min2 = axisX * verts2[0] + axisY * verts2[1];
            float max2 = min2;
            for(int j = offset2; j < end2; j += 2){
                float p = axisX * verts2[j] + axisY * verts2[j + 1];
                if(p < min2){
                    min2 = p;
                }else if(p > max2){
                    max2 = p;
                }
            }

            if(!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)){
                return false;
            }else{
                float o = Math.min(max1, max2) - Math.max(min1, min2);

                if(min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1){
                    float mins = Math.abs(min1 - min2);
                    float maxs = Math.abs(max1 - max2);
                    if(mins < maxs){
                        o += mins;
                    }else{
                        o += maxs;
                    }
                }

                if(o < overlap){
                    overlap = o;
                    // Adjusts the direction based on the number of points found
                    smallestAxisX = numInNormalDir < 0 ? axisX : -axisX;
                    smallestAxisY = numInNormalDir < 0 ? axisY : -axisY;
                }
            }
            // -- End check for separation on this axis --//
        }
        if(mtv != null){
            mtv.normal.set(smallestAxisX, smallestAxisY);
            mtv.depth = overlap;
        }
        return true;
    }


    /** Minimum translation required to separate two polygons. */
    public static class MinimumTranslationVector{
        /** Unit length vector that indicates the direction for the separation */
        public Vec2 normal = new Vec2();
        /** Distance of the translation required for the separation */
        public float depth = 0;
    }
}
