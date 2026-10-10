package arc.math.geom;

/**
 * Interface that specifies a path of type T within the window 0.0<=t<=1.0.
 * @author Xoppa
 */
public interface Path<T>{
    T derivativeAt(T out, float t);

    /** @return The value of the path at t where 0<=t<=1 */
    T valueAt(T out, float t);

    /**
     * @return The approximated value (between 0 and 1) on the path which is closest to the specified value. Note that the
     * implementation of this method might be optimized for speed against precision, see {@link #locate(Object)} for a more
     * precise (but more intensive) method.
     */
    float approximate(T v);

    /**
     * @return The precise location (between 0 and 1) on the path which is closest to the specified value. Note that the
     * implementation of this method might be CPU intensive, see {@link #approximate(Object)} for a faster (but less
     * precise) method.
     */
    float locate(T v);


    /**
     * @param samples The amount of divisions used to approximate length. Higher values will produce more precise results,
     * but will be more CPU intensive.
     * @return An approximated length of the spline through sampling the curve and accumulating the euclidean distances between
     * the sample points.
     */
    float approxLength(int samples);

}