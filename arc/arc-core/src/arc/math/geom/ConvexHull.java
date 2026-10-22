package arc.math.geom;

import arc.struct.FloatSeq;
import arc.struct.IntSeq;
import arc.struct.ShortSeq;

/**
 * Computes the convex hull of a set of points using the monotone chain convex hull algorithm (aka Andrew's algorithm).
 * @author Nathan Sweet
 */
public class ConvexHull{
    private final IntSeq quicksortStack = new IntSeq();
    private final FloatSeq hull = new FloatSeq();
    private final IntSeq indices = new IntSeq();
    private final ShortSeq originalIndices = new ShortSeq(false, 0);
    private float[] sortedPoints;

    /** @see #computePolygon(float[], int, int, boolean) */
    public FloatSeq computePolygon(FloatSeq points, boolean sorted){
        return computePolygon(points.items, 0, points.size, sorted);
    }

    /** @see #computePolygon(float[], int, int, boolean) */
    public FloatSeq computePolygon(float[] polygon, boolean sorted){
        return computePolygon(polygon, 0, polygon.length, sorted);
    }

    /** Returns a list of points on the convex hull in counter-clockwise order. Note: the last point in the returned list is the
     * same as the first one. */
    /**
     * Returns the convex hull polygon for the given point cloud.
     * @param points x,y pairs describing points. Duplicate points will result in undefined behavior.
     * @param sorted If false, the points will be sorted by the x coordinate then the y coordinate, which is required by the convex
     * hull algorithm. If sorting is done the input array is not modified and count additional working memory is needed.
     * @return pairs of coordinates that describe the convex hull polygon in counterclockwise order. Note the returned array is
     * reused for later calls to the same method.
     */
    public FloatSeq computePolygon(float[] points, int offset, int count, boolean sorted){
        int end = offset + count;

        if(!sorted){
            if(sortedPoints == null || sortedPoints.length < count) sortedPoints = new float[count];
            System.arraycopy(points, offset, sortedPoints, 0, count);
            points = sortedPoints;
            offset = 0;
            sort(points, count);
        }

        FloatSeq hull = this.hull;
        hull.clear();

        // Lower hull.
        for(int i = offset; i < end; i += 2){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= 4 && ccw(x, y) <= 0)
                hull.size -= 2;
            hull.add(x);
            hull.add(y);
        }

        // Upper hull.
        for(int i = end - 4, t = hull.size + 2; i >= offset; i -= 2){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= t && ccw(x, y) <= 0)
                hull.size -= 2;
            hull.add(x);
            hull.add(y);
        }

        return hull;
    }

    /** @see #computeIndices(float[], int, int, boolean, boolean) */
    public IntSeq computeIndices(FloatSeq points, boolean sorted, boolean yDown){
        return computeIndices(points.items, 0, points.size, sorted, yDown);
    }

    /** @see #computeIndices(float[], int, int, boolean, boolean) */
    public IntSeq computeIndices(float[] polygon, boolean sorted, boolean yDown){
        return computeIndices(polygon, 0, polygon.length, sorted, yDown);
    }

    /** Computes a hull the same as {@link #computePolygon(float[], int, int, boolean)} but returns indices of the specified points. */
    public IntSeq computeIndices(float[] points, int offset, int count, boolean sorted, boolean yDown){
        int end = offset + count;

        if(!sorted){
            if(sortedPoints == null || sortedPoints.length < count) sortedPoints = new float[count];
            System.arraycopy(points, offset, sortedPoints, 0, count);
            points = sortedPoints;
            offset = 0;
            sortWithIndices(points, count, yDown);
        }

        IntSeq indices = this.indices;
        indices.clear();

        FloatSeq hull = this.hull;
        hull.clear();

        // Lower hull.
        for(int i = offset, index = i / 2; i < end; i += 2, index++){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= 4 && ccw(x, y) <= 0){
                hull.size -= 2;
                indices.size--;
            }
            hull.add(x);
            hull.add(y);
            indices.add(index);
        }

        // Upper hull.
        for(int i = end - 4, index = i / 2, t = hull.size + 2; i >= offset; i -= 2, index--){
            float x = points[i];
            float y = points[i + 1];
            while(hull.size >= t && ccw(x, y) <= 0){
                hull.size -= 2;
                indices.size--;
            }
            hull.add(x);
            hull.add(y);
            indices.add(index);
        }

        // Convert sorted to unsorted indices.
        if(!sorted){
            short[] originalIndicesArray = originalIndices.items;
            int[] indicesArray = indices.items;
            for(int i = 0, n = indices.size; i < n; i++)
                indicesArray[i] = originalIndicesArray[indicesArray[i]];
        }

        return indices;
    }

    /** Returns > 0 if the points are a counterclockwise turn, < 0 if clockwise, and 0 if colinear. */
    private float ccw(float p3x, float p3y){
        FloatSeq hull = this.hull;
        int size = hull.size;
        float p1x = hull.get(size - 4);
        float p1y = hull.get(size - 3);
        float p2x = hull.get(size - 2);
        float p2y = hull.peek();
        return (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x);
    }

    /**
     * Sorts x,y pairs of values by the x value, then the y value.
     * @param count Number of indices, must be even.
     */
    private void sort(float[] values, int count){
        int lower = 0;
        int upper = count - 1;
        IntSeq stack = quicksortStack;
        stack.add(lower);
        stack.add(upper - 1);
        while(stack.size > 0){
            upper = stack.pop();
            lower = stack.pop();
            if(upper <= lower) continue;
            int i = quicksortPartition(values, lower, upper);
            if(i - lower > upper - i){
                stack.add(lower);
                stack.add(i - 2);
            }
            stack.add(i + 2);
            stack.add(upper);
            if(upper - i >= i - lower){
                stack.add(lower);
                stack.add(i - 2);
            }
        }
    }

    private int quicksortPartition(final float[] values, int lower, int upper){
        float x = values[lower];
        float y = values[lower + 1];
        int up = upper;
        int down = lower;
        float temp;
        short tempIndex;
        while(down < up){
            while(down < up && values[down] <= x)
                down = down + 2;
            while(values[up] > x || (values[up] == x && values[up + 1] < y))
                up = up - 2;
            if(down < up){
                temp = values[down];
                values[down] = values[up];
                values[up] = temp;

                temp = values[down + 1];
                values[down + 1] = values[up + 1];
                values[up + 1] = temp;
            }
        }
        values[lower] = values[up];
        values[up] = x;

        values[lower + 1] = values[up + 1];
        values[up + 1] = y;

        return up;
    }

    /**
     * Sorts x,y pairs of values by the x value, then the y value and stores unsorted original indices.
     * @param count Number of indices, must be even.
     */
    private void sortWithIndices(float[] values, int count, boolean yDown){
        int pointCount = count / 2;
        originalIndices.clear();
        originalIndices.ensureCapacity(pointCount);
        short[] originalIndicesArray = originalIndices.items;
        for(short i = 0; i < pointCount; i++)
            originalIndicesArray[i] = i;

        int lower = 0;
        int upper = count - 1;
        IntSeq stack = quicksortStack;
        stack.add(lower);
        stack.add(upper - 1);
        while(stack.size > 0){
            upper = stack.pop();
            lower = stack.pop();
            if(upper <= lower) continue;
            int i = quicksortPartitionWithIndices(values, lower, upper, yDown, originalIndicesArray);
            if(i - lower > upper - i){
                stack.add(lower);
                stack.add(i - 2);
            }
            stack.add(i + 2);
            stack.add(upper);
            if(upper - i >= i - lower){
                stack.add(lower);
                stack.add(i - 2);
            }
        }
    }

    private int quicksortPartitionWithIndices(final float[] values, int lower, int upper, boolean yDown, short[] originalIndices){
        float x = values[lower];
        float y = values[lower + 1];
        int up = upper;
        int down = lower;
        float temp;
        short tempIndex;
        while(down < up){
            while(down < up && values[down] <= x)
                down = down + 2;
            if(yDown){
                while(values[up] > x || (values[up] == x && values[up + 1] < y))
                    up = up - 2;
            }else{
                while(values[up] > x || (values[up] == x && values[up + 1] > y))
                    up = up - 2;
            }
            if(down < up){
                temp = values[down];
                values[down] = values[up];
                values[up] = temp;

                temp = values[down + 1];
                values[down + 1] = values[up + 1];
                values[up + 1] = temp;

                tempIndex = originalIndices[down / 2];
                originalIndices[down / 2] = originalIndices[up / 2];
                originalIndices[up / 2] = tempIndex;
            }
        }
        values[lower] = values[up];
        values[up] = x;

        values[lower + 1] = values[up + 1];
        values[up + 1] = y;

        tempIndex = originalIndices[lower / 2];
        originalIndices[lower / 2] = originalIndices[up / 2];
        originalIndices[up / 2] = tempIndex;

        return up;
    }
}
