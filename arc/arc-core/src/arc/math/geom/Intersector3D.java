package arc.math.geom;

import arc.math.*;
import arc.math.geom.Plane.*;
import arc.struct.*;

import java.util.*;

public class Intersector3D{
    private static final Vec3 v0 = new Vec3();
    private static final Vec3 v1 = new Vec3();
    private static final Vec3 v2 = new Vec3();
    private static final Plane p = new Plane(new Vec3(), 0);
    private static final Vec3 i = new Vec3();
    static Vec3 best = new Vec3();
    static Vec3 tmp = new Vec3();
    static Vec3 tmp1 = new Vec3();
    static Vec3 tmp2 = new Vec3();
    static Vec3 tmp3 = new Vec3();
    static Vec3 intersection = new Vec3();

    /**
     * Intersects a {@link Ray} and a {@link Plane}. The intersection point is stored in intersection in case an intersection is
     * present.
     * @param ray The ray
     * @param plane The plane
     * @param intersection The vector the intersection point is written to (optional)
     * @return Whether an intersection is present.
     */
    public static boolean intersectRayPlane(Ray ray, Plane plane, Vec3 intersection){
        float denom = ray.direction.dot(plane.normal);
        if(denom != 0){
            float t = -(ray.origin.dot(plane.normal) + plane.d) / denom;
            if(t < 0) return false;

            if(intersection != null) intersection.set(ray.origin).add(v0.set(ray.direction).scl(t));
            return true;
        }else if(plane.testPoint(ray.origin) == Plane.PlaneSide.onPlane){
            if(intersection != null) intersection.set(ray.origin);
            return true;
        }else
            return false;
    }

    /**
     * Intersects a line and a plane. The intersection is returned as the distance from the first point to the plane. In case an
     * intersection happened, the return value is in the range [0,1]. The intersection point can be recovered by point1 + t *
     * (point2 - point1) where t is the return value of this method.
     */
    public static float intersectLinePlane(float x, float y, float z, float x2, float y2, float z2, Plane plane,
                                           Vec3 intersection){
        Vec3 direction = tmp.set(x2, y2, z2).sub(x, y, z);
        Vec3 origin = tmp2.set(x, y, z);
        float denom = direction.dot(plane.normal);
        if(denom != 0){
            float t = -(origin.dot(plane.normal) + plane.d) / denom;
            if(intersection != null) intersection.set(origin).add(direction.scl(t));
            return t;
        }else if(plane.testPoint(origin) == Plane.PlaneSide.onPlane){
            if(intersection != null) intersection.set(origin);
            return 0;
        }

        return -1;
    }

    /**
     * Intersect a {@link Ray} and a triangle, returning the intersection point in intersection.
     * @param ray The ray
     * @param t1 The first vertex of the triangle
     * @param t2 The second vertex of the triangle
     * @param t3 The third vertex of the triangle
     * @param intersection The intersection point (optional)
     * @return True in case an intersection is present.
     */
    public static boolean intersectRayTriangle(Ray ray, Vec3 t1, Vec3 t2, Vec3 t3, Vec3 intersection){
        Vec3 edge1 = v0.set(t2).sub(t1);
        Vec3 edge2 = v1.set(t3).sub(t1);

        Vec3 pvec = v2.set(ray.direction).crs(edge2);
        float det = edge1.dot(pvec);
        if(Mathf.zero(det)){
            p.set(t1, t2, t3);
            if(p.testPoint(ray.origin) == PlaneSide.onPlane && Intersector.isInTriangle(ray.origin, t1, t2, t3)){
                if(intersection != null) intersection.set(ray.origin);
                return true;
            }
            return false;
        }

        det = 1.0f / det;

        Vec3 tvec = i.set(ray.origin).sub(t1);
        float u = tvec.dot(pvec) * det;
        if(u < 0.0f || u > 1.0f) return false;

        Vec3 qvec = tvec.crs(edge1);
        float v = ray.direction.dot(qvec) * det;
        if(v < 0.0f || u + v > 1.0f) return false;

        float t = edge2.dot(qvec) * det;
        if(t < 0) return false;

        if(intersection != null){
            if(t <= Mathf.FLOAT_ROUNDING_ERROR){
                intersection.set(ray.origin);
            }else{
                ray.getEndPoint(intersection, t);
            }
        }

        return true;
    }

    /**
     * Intersects a {@link Ray} and a sphere, returning the intersection point in intersection.
     * @param ray The ray, the direction component must be normalized before calling this method
     * @param center The center of the sphere
     * @param radius The radius of the sphere
     * @param intersection The intersection point (optional, can be null)
     * @return Whether an intersection is present.
     */
    public static boolean intersectRaySphere(Ray ray, Vec3 center, float radius, Vec3 intersection){
        final float len = ray.direction.dot(center.x - ray.origin.x, center.y - ray.origin.y, center.z - ray.origin.z);
        if(len < 0.f) // behind the ray
            return false;
        final float dst2 = center.dst2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len,
        ray.origin.z + ray.direction.z * len);
        final float r2 = radius * radius;
        if(dst2 > r2) return false;
        if(intersection != null)
            intersection.set(ray.direction).scl(len - (float)Math.sqrt(r2 - dst2)).add(ray.origin);
        return true;
    }

    /**
     * Intersects a {@link Ray} and a {@link BoundingBox}, returning the intersection point in intersection. This intersection is
     * defined as the point on the ray closest to the origin which is within the specified bounds.
     *
     * <p>
     * The returned intersection (if any) is guaranteed to be within the bounds of the bounding box, but it can occasionally
     * diverge slightly from ray, due to small floating-point errors.
     * </p>
     *
     * <p>
     * If the origin of the ray is inside the box, this method returns true and the intersection point is set to the origin of the
     * ray, accordingly to the definition above.
     * </p>
     * @param ray The ray
     * @param box The box
     * @param intersection The intersection point (optional)
     * @return Whether an intersection is present.
     */
    public static boolean intersectRayBounds(Ray ray, BoundingBox box, Vec3 intersection){
        if(box.contains(ray.origin)){
            if(intersection != null) intersection.set(ray.origin);
            return true;
        }
        float lowest = 0, t;
        boolean hit = false;

        // min x
        if(ray.origin.x <= box.min.x && ray.direction.x > 0){
            t = (box.min.x - ray.origin.x) / ray.direction.x;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // max x
        if(ray.origin.x >= box.max.x && ray.direction.x < 0){
            t = (box.max.x - ray.origin.x) / ray.direction.x;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.y >= box.min.y && v2.y <= box.max.y && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // min y
        if(ray.origin.y <= box.min.y && ray.direction.y > 0){
            t = (box.min.y - ray.origin.y) / ray.direction.y;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // max y
        if(ray.origin.y >= box.max.y && ray.direction.y < 0){
            t = (box.max.y - ray.origin.y) / ray.direction.y;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.z >= box.min.z && v2.z <= box.max.z && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // min z
        if(ray.origin.z <= box.min.z && ray.direction.z > 0){
            t = (box.min.z - ray.origin.z) / ray.direction.z;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        // max y
        if(ray.origin.z >= box.max.z && ray.direction.z < 0){
            t = (box.max.z - ray.origin.z) / ray.direction.z;
            if(t >= 0){
                v2.set(ray.direction).scl(t).add(ray.origin);
                if(v2.x >= box.min.x && v2.x <= box.max.x && v2.y >= box.min.y && v2.y <= box.max.y && (!hit || t < lowest)){
                    hit = true;
                    lowest = t;
                }
            }
        }
        if(hit && intersection != null){
            intersection.set(ray.direction).scl(lowest).add(ray.origin);
            if(intersection.x < box.min.x){
                intersection.x = box.min.x;
            }else if(intersection.x > box.max.x){
                intersection.x = box.max.x;
            }
            if(intersection.y < box.min.y){
                intersection.y = box.min.y;
            }else if(intersection.y > box.max.y){
                intersection.y = box.max.y;
            }
            if(intersection.z < box.min.z){
                intersection.z = box.min.z;
            }else if(intersection.z > box.max.z){
                intersection.z = box.max.z;
            }
        }
        return hit;
    }

    /**
     * Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
     * @param ray The ray
     * @param box The bounding box
     * @return Whether the ray and the bounding box intersect.
     */
    public static boolean intersectRayBoundsFast(Ray ray, BoundingBox box){
        return intersectRayBoundsFast(ray, box.getCenter(tmp1), box.getDimensions(tmp2));
    }

    /**
     * Quick check whether the given {@link Ray} and {@link BoundingBox} intersect.
     * @param ray The ray
     * @param center The center of the bounding box
     * @param dimensions The dimensions (width, height and depth) of the bounding box
     * @return Whether the ray and the bounding box intersect.
     */
    public static boolean intersectRayBoundsFast(Ray ray, Vec3 center, Vec3 dimensions){
        final float divX = 1f / ray.direction.x;
        final float divY = 1f / ray.direction.y;
        final float divZ = 1f / ray.direction.z;

        float minx = ((center.x - dimensions.x * .5f) - ray.origin.x) * divX;
        float maxx = ((center.x + dimensions.x * .5f) - ray.origin.x) * divX;
        if(minx > maxx){
            final float t = minx;
            minx = maxx;
            maxx = t;
        }

        float miny = ((center.y - dimensions.y * .5f) - ray.origin.y) * divY;
        float maxy = ((center.y + dimensions.y * .5f) - ray.origin.y) * divY;
        if(miny > maxy){
            final float t = miny;
            miny = maxy;
            maxy = t;
        }

        float minz = ((center.z - dimensions.z * .5f) - ray.origin.z) * divZ;
        float maxz = ((center.z + dimensions.z * .5f) - ray.origin.z) * divZ;
        if(minz > maxz){
            final float t = minz;
            minz = maxz;
            maxz = t;
        }

        float min = Math.max(Math.max(minx, miny), minz);
        float max = Math.min(Math.min(maxx, maxy), maxz);

        return max >= 0 && max >= min;
    }

    public static boolean intersectSegmentPlane(Vec3 start, Vec3 end, Plane plane, Vec3 intersection){
        Vec3 dir = v0.set(end).sub(start);
        float denom = dir.dot(plane.normal);
        if(denom == 0f) return false;
        float t = -(start.dot(plane.normal) + plane.d) / denom;
        if(t < 0 || t > 1) return false;

        intersection.set(start).add(dir.scl(t));
        return true;
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     * @param ray The ray
     * @param triangles The triangles, each successive 3 elements from a vertex
     * @param intersection The nearest intersection point (optional)
     * @return Whether the ray and the triangles intersect.
     */
    public static boolean intersectRayTriangles(Ray ray, float[] triangles, Vec3 intersection){
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;

        if(triangles.length / 3 % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

        for(int i = 0; i < triangles.length - 6; i += 9){
            boolean result = intersectRayTriangle(ray, tmp1.set(triangles[i], triangles[i + 1], triangles[i + 2]),
            tmp2.set(triangles[i + 3], triangles[i + 4], triangles[i + 5]),
            tmp3.set(triangles[i + 6], triangles[i + 7], triangles[i + 8]), tmp);

            if(result){
                float dist = ray.origin.dst2(tmp);
                if(dist < min_dist){
                    min_dist = dist;
                    best.set(tmp);
                    hit = true;
                }
            }
        }

        if(!hit)
            return false;
        else{
            if(intersection != null) intersection.set(best);
            return true;
        }
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     * @param ray The ray
     * @param vertices the vertices
     * @param indices the indices, each successive 3 shorts index the 3 vertices of a triangle
     * @param vertexSize the size of a vertex in floats
     * @param intersection The nearest intersection point (optional)
     * @return Whether the ray and the triangles intersect.
     */
    public static boolean intersectRayTriangles(Ray ray, float[] vertices, short[] indices, int vertexSize,
                                                Vec3 intersection){
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;

        if(indices.length % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

        for(int i = 0; i < indices.length; i += 3){
            int i1 = indices[i] * vertexSize;
            int i2 = indices[i + 1] * vertexSize;
            int i3 = indices[i + 2] * vertexSize;

            boolean result = intersectRayTriangle(ray, tmp1.set(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]),
            tmp2.set(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]),
            tmp3.set(vertices[i3], vertices[i3 + 1], vertices[i3 + 2]), tmp);

            if(result){
                float dist = ray.origin.dst2(tmp);
                if(dist < min_dist){
                    min_dist = dist;
                    best.set(tmp);
                    hit = true;
                }
            }
        }

        if(!hit)
            return false;
        else{
            if(intersection != null) intersection.set(best);
            return true;
        }
    }

    /**
     * Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
     * @param ray The ray
     * @param triangles The triangles
     * @param intersection The nearest intersection point (optional)
     * @return Whether the ray and the triangles intersect.
     */
    public static boolean intersectRayTriangles(Ray ray, Seq<Vec3> triangles, Vec3 intersection){
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;

        if(triangles.size % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

        for(int i = 0; i < triangles.size - 2; i += 3){
            boolean result = intersectRayTriangle(ray, triangles.get(i), triangles.get(i + 1), triangles.get(i + 2), tmp);

            if(result){
                float dist = ray.origin.dst2(tmp);
                if(dist < min_dist){
                    min_dist = dist;
                    best.set(tmp);
                    hit = true;
                }
            }
        }

        if(!hit)
            return false;
        else{
            if(intersection != null) intersection.set(best);
            return true;
        }
    }


    /**
     * Splits the triangle by the plane. The result is stored in the SplitTriangle instance. Depending on where the triangle is
     * relative to the plane, the result can be:
     *
     * <ul>
     * <li>Triangle is fully in front/behind: {@link SplitTriangle#front} or {@link SplitTriangle#back} will contain the original
     * triangle, {@link SplitTriangle#total} will be one.</li>
     * <li>Triangle has two vertices in front, one behind: {@link SplitTriangle#front} contains 2 triangles,
     * {@link SplitTriangle#back} contains 1 triangles, {@link SplitTriangle#total} will be 3.</li>
     * <li>Triangle has one vertex in front, two behind: {@link SplitTriangle#front} contains 1 triangle,
     * {@link SplitTriangle#back} contains 2 triangles, {@link SplitTriangle#total} will be 3.</li>
     * </ul>
     * <p>
     * The input triangle should have the form: x, y, z, x2, y2, z2, x3, y3, z3. One can add additional attributes per vertex which
     * will be interpolated if split, such as texture coordinates or normals. Note that these additional attributes won't be
     * normalized, as might be necessary in case of normals.
     * @param split output SplitTriangle
     */
    public static void splitTriangle(float[] triangle, Plane plane, SplitTriangle split){
        int stride = triangle.length / 3;
        boolean r1 = plane.testPoint(triangle[0], triangle[1], triangle[2]) == PlaneSide.back;
        boolean r2 = plane.testPoint(triangle[stride], triangle[1 + stride], triangle[2 + stride]) == PlaneSide.back;
        boolean r3 = plane.testPoint(triangle[stride * 2], triangle[1 + stride * 2],
        triangle[2 + stride * 2]) == PlaneSide.back;

        split.reset();

        // easy case, triangle is on one side (point on plane means front).
        if(r1 == r2 && r2 == r3){
            split.total = 1;
            if(r1){
                split.numBack = 1;
                System.arraycopy(triangle, 0, split.back, 0, triangle.length);
            }else{
                split.numFront = 1;
                System.arraycopy(triangle, 0, split.front, 0, triangle.length);
            }
            return;
        }

        // set number of triangles
        split.total = 3;
        split.numFront = (r1 ? 0 : 1) + (r2 ? 0 : 1) + (r3 ? 0 : 1);
        split.numBack = split.total - split.numFront;

        // hard case, split the three edges on the plane
        // determine which array to fill first, front or back, flip if we
        // cross the plane
        split.setSide(!r1);

        // split first edge
        int first = 0;
        int second = stride;
        if(r1 != r2){
            // split the edge
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

            // add first edge vertex and new vertex to current side
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);

            // flip side and add new vertex and second edge vertex to current side
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        }else{
            // add both vertices
            split.add(triangle, first, stride);
        }

        // split second edge
        first = stride;
        second = stride + stride;
        if(r2 != r3){
            // split the edge
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

            // add first edge vertex and new vertex to current side
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);

            // flip side and add new vertex and second edge vertex to current side
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        }else{
            // add both vertices
            split.add(triangle, first, stride);
        }

        // split third edge
        first = stride + stride;
        second = 0;
        if(r3 != r1){
            // split the edge
            splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);

            // add first edge vertex and new vertex to current side
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);

            // flip side and add new vertex and second edge vertex to current side
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        }else{
            // add both vertices
            split.add(triangle, first, stride);
        }

        // triangulate the side with 2 triangles
        if(split.numFront == 2){
            System.arraycopy(split.front, stride * 2, split.front, stride * 3, stride * 2);
            System.arraycopy(split.front, 0, split.front, stride * 5, stride);
        }else{
            System.arraycopy(split.back, stride * 2, split.back, stride * 3, stride * 2);
            System.arraycopy(split.back, 0, split.back, stride * 5, stride);
        }
    }

    private static void splitEdge(float[] vertices, int s, int e, int stride, Plane plane, float[] split, int offset){
        float t = intersectLinePlane(vertices[s], vertices[s + 1], vertices[s + 2], vertices[e], vertices[e + 1],
        vertices[e + 2], plane, intersection);
        split[offset] = intersection.x;
        split[offset + 1] = intersection.y;
        split[offset + 2] = intersection.z;
        for(int i = 3; i < stride; i++){
            float a = vertices[s + i];
            float b = vertices[e + i];
            split[offset + i] = a + t * (b - a);
        }
    }


    public static class SplitTriangle{
        public float[] front;
        public float[] back;
        public int numFront;
        public int numBack;
        public int total;
        float[] edgeSplit;
        boolean frontCurrent = false;
        int frontOffset = 0;
        int backOffset = 0;

        /**
         * Creates a new instance, assuming numAttributes attributes per triangle vertex.
         * @param numAttributes must be >= 3
         */
        public SplitTriangle(int numAttributes){
            front = new float[numAttributes * 3 * 2];
            back = new float[numAttributes * 3 * 2];
            edgeSplit = new float[numAttributes];
        }

        @Override
        public String toString(){
            return "SplitTriangle [front=" + Arrays.toString(front) + ", back=" + Arrays.toString(back) + ", numFront=" + numFront
            + ", numBack=" + numBack + ", total=" + total + "]";
        }

        boolean getSide(){
            return frontCurrent;
        }

        void setSide(boolean front){
            frontCurrent = front;
        }

        void add(float[] vertex, int offset, int stride){
            if(frontCurrent){
                System.arraycopy(vertex, offset, front, frontOffset, stride);
                frontOffset += stride;
            }else{
                System.arraycopy(vertex, offset, back, backOffset, stride);
                backOffset += stride;
            }
        }

        void reset(){
            frontCurrent = false;
            frontOffset = 0;
            backOffset = 0;
            numFront = 0;
            numBack = 0;
            total = 0;
        }
    }
}