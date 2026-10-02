package arc.math;

import arc.struct.FloatSeq;
import arc.struct.IntSeq;
import arc.struct.ShortSeq;

/**
 * A simple implementation of the ear cutting algorithm to triangulate simple polygons without holes. For more information:
 * <ul>
 * <li><a href="http://cgm.cs.mcgill.ca/~godfried/teaching/cg-projects/97/Ian/algorithm2.html">http://cgm.cs.mcgill.ca/~godfried/
 * teaching/cg-projects/97/Ian/algorithm2.html</a></li>
 * <li><a
 * href="http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf">http://www.geometrictools.com/Documentation
 * /TriangulationByEarClipping.pdf</a></li>
 * </ul>
 * If the input polygon is not simple (self-intersects), there will be output but it is of unspecified quality (garbage in,
 * garbage out).
 * @author badlogicgames@gmail.com
 * @author Nicolas Gramlich (optimizations, collinear edge support)
 * @author Eric Spitz
 * @author Thomas ten Cate (bugfixes, optimizations)
 * @author Nathan Sweet (rewrite, return indices, no allocation, optimizations)
 */
public class EarClippingTriangulator{
    private static final int CONCAVE = -1;
    private static final int CONVEX = 1;

    private final ShortSeq indicesArray = new ShortSeq();
    private final IntSeq vertexTypes = new IntSeq();
    private final ShortSeq triangles = new ShortSeq();
    private short[] indices;
    private float[] vertices;
    private int vertexCount;

    private static boolean areVerticesClockwise(float[] vertices, int offset, int count){
        if(count <= 2) return false;
        float area = 0, p1x, p1y, p2x, p2y;
        for(int i = offset, n = offset + count - 3; i < n; i += 2){
            p1x = vertices[i];
            p1y = vertices[i + 1];
            p2x = vertices[i + 2];
            p2y = vertices[i + 3];
            area += p1x * p2y - p2x * p1y;
        }
        p1x = vertices[offset + count - 2];
        p1y = vertices[offset + count - 1];
        p2x = vertices[offset];
        p2y = vertices[offset + 1];
        return area + p1x * p2y - p2x * p1y < 0;
    }

    private static int computeSpannedAreaSign(float p1x, float p1y, float p2x, float p2y, float p3x, float p3y){
        float area = p1x * (p3y - p2y);
        area += p2x * (p1y - p3y);
        area += p3x * (p2y - p1y);
        return (int)Math.signum(area);
    }

    /** @see #computeTriangles(float[], int, int) */
    public ShortSeq computeTriangles(FloatSeq vertices){
        return computeTriangles(vertices.items, 0, vertices.size);
    }

    /** @see #computeTriangles(float[], int, int) */
    public ShortSeq computeTriangles(float[] vertices){
        return computeTriangles(vertices, 0, vertices.length);
    }

    /**
     * Triangulates the given (convex or concave) simple polygon to a list of triangle vertices.
     * @param vertices pairs describing vertices of the polygon, in either clockwise or counterclockwise order.
     * @return triples of triangle indices in clockwise order. Note the returned array is reused for later calls to the same
     * method.
     */
    public ShortSeq computeTriangles(float[] vertices, int offset, int count){
        this.vertices = vertices;
        int vertexCount = this.vertexCount = count / 2;
        int vertexOffset = offset / 2;

        ShortSeq indicesArray = this.indicesArray;
        indicesArray.clear();
        indicesArray.ensureCapacity(vertexCount);
        indicesArray.size = vertexCount;
        short[] indices = this.indices = indicesArray.items;
        if(areVerticesClockwise(vertices, offset, count)){
            for(short i = 0; i < vertexCount; i++)
                indices[i] = (short)(vertexOffset + i);
        }else{
            for(int i = 0, n = vertexCount - 1; i < vertexCount; i++)
                indices[i] = (short)(vertexOffset + n - i); // Reversed.
        }

        IntSeq vertexTypes = this.vertexTypes;
        vertexTypes.clear();
        vertexTypes.ensureCapacity(vertexCount);
        for(int i = 0; i < vertexCount; ++i)
            vertexTypes.add(classifyVertex(i));

        // A polygon with n vertices has a triangulation of n-2 triangles.
        ShortSeq triangles = this.triangles;
        triangles.clear();
        triangles.ensureCapacity(Math.max(0, vertexCount - 2) * 3);
        triangulate();
        return triangles;
    }

    private void triangulate(){
        int[] vertexTypes = this.vertexTypes.items;

        while(vertexCount > 3){
            int earTipIndex = findEarTip();
            cutEarTip(earTipIndex);

            // The type of the two vertices adjacent to the clipped vertex may have changed.
            int previousIndex = previousIndex(earTipIndex);
            int nextIndex = earTipIndex == vertexCount ? 0 : earTipIndex;
            vertexTypes[previousIndex] = classifyVertex(previousIndex);
            vertexTypes[nextIndex] = classifyVertex(nextIndex);
        }

        if(vertexCount == 3){
            ShortSeq triangles = this.triangles;
            short[] indices = this.indices;
            triangles.add(indices[0]);
            triangles.add(indices[1]);
            triangles.add(indices[2]);
        }
    }

    /** @return {@link #CONCAVE} or {@link #CONVEX} */
    private int classifyVertex(int index){
        short[] indices = this.indices;
        int previous = indices[previousIndex(index)] * 2;
        int current = indices[index] * 2;
        int next = indices[nextIndex(index)] * 2;
        float[] vertices = this.vertices;
        return computeSpannedAreaSign(vertices[previous], vertices[previous + 1], vertices[current], vertices[current + 1],
        vertices[next], vertices[next + 1]);
    }

    private int findEarTip(){
        int vertexCount = this.vertexCount;
        for(int i = 0; i < vertexCount; i++)
            if(isEarTip(i)) return i;

        // Desperate mode: if no vertex is an ear tip, we are dealing with a degenerate polygon (e.g. nearly collinear).
        // Note that the input was not necessarily degenerate, but we could have made it so by clipping some valid ears.

        // Idea taken from Martin Held, "FIST: Fast industrial-strength triangulation of polygons", Algorithmica (1998),
        // http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.115.291

        // Return a convex or tangential vertex if one exists.
        int[] vertexTypes = this.vertexTypes.items;
        for(int i = 0; i < vertexCount; i++)
            if(vertexTypes[i] != CONCAVE) return i;
        return 0; // If all vertices are concave, just return the first one.
    }

    private boolean isEarTip(int earTipIndex){
        int[] vertexTypes = this.vertexTypes.items;
        if(vertexTypes[earTipIndex] == CONCAVE) return false;

        int previousIndex = previousIndex(earTipIndex);
        int nextIndex = nextIndex(earTipIndex);
        short[] indices = this.indices;
        int p1 = indices[previousIndex] * 2;
        int p2 = indices[earTipIndex] * 2;
        int p3 = indices[nextIndex] * 2;
        float[] vertices = this.vertices;
        float p1x = vertices[p1], p1y = vertices[p1 + 1];
        float p2x = vertices[p2], p2y = vertices[p2 + 1];
        float p3x = vertices[p3], p3y = vertices[p3 + 1];

        // Check if any point is inside the triangle formed by previous, current and next vertices.
        // Only consider vertices that are not part of this triangle, or else we'll always find one inside.
        for(int i = nextIndex(nextIndex); i != previousIndex; i = nextIndex(i)){
            // Concave vertices can obviously be inside the candidate ear, but so can tangential vertices
            // if they coincide with one of the triangle's vertices.
            if(vertexTypes[i] != CONVEX){
                int v = indices[i] * 2;
                float vx = vertices[v];
                float vy = vertices[v + 1];
                // Because the polygon has clockwise winding order, the area sign will be positive if the point is strictly inside.
                // It will be 0 on the edge, which we want to include as well.
                // note: check the edge defined by p1->p3 first since this fails _far_ more then the other 2 checks.
                if(computeSpannedAreaSign(p3x, p3y, p1x, p1y, vx, vy) >= 0){
                    if(computeSpannedAreaSign(p1x, p1y, p2x, p2y, vx, vy) >= 0){
                        if(computeSpannedAreaSign(p2x, p2y, p3x, p3y, vx, vy) >= 0) return false;
                    }
                }
            }
        }
        return true;
    }

    private void cutEarTip(int earTipIndex){
        short[] indices = this.indices;
        ShortSeq triangles = this.triangles;

        triangles.add(indices[previousIndex(earTipIndex)]);
        triangles.add(indices[earTipIndex]);
        triangles.add(indices[nextIndex(earTipIndex)]);

        indicesArray.removeIndex(earTipIndex);
        vertexTypes.removeIndex(earTipIndex);
        vertexCount--;
    }

    private int previousIndex(int index){
        return (index == 0 ? vertexCount : index) - 1;
    }

    private int nextIndex(int index){
        return (index + 1) % vertexCount;
    }
}
