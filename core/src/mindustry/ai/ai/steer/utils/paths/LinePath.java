package mindustry.ai.ai.steer.utils.paths;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.ai.ai.steer.utils.Path;
import mindustry.ai.ai.steer.utils.paths.LinePath.*;

/**
 * A {@code LinePath} is a path for path following behaviors that is made up of a series of waypoints. Each waypoint is connected
 * to the successor with a {@link Segment}.
 * @author davebaol
 * @author Daniel Holderbaum
 */
public class LinePath implements Path<LinePathParam>{

    private Array<Segment> segments;
    private boolean isOpen;
    private float pathLength;
    private Vec2 nearestPointOnCurrentSegment;
    private Vec2 nearestPointOnPath;
    private Vec2 tmpB;
    private Vec2 tmpC;

    /**
     * Creates a closed {@code LinePath} for the specified {@code waypoints}.
     * @param waypoints the points making up the path
     * @throws IllegalArgumentException if {@code waypoints} is {@code null} or has less than two (2) waypoints.
     */
    public LinePath(Array waypoints){
        this(waypoints, false);
    }

    /**
     * Creates a {@code LinePath} for the specified {@code waypoints}.
     * @param waypoints the points making up the path
     * @param isOpen a flag indicating whether the path is open or not
     * @throws IllegalArgumentException if {@code waypoints} is {@code null} or has less than two (2) waypoints.
     */
    public LinePath(Array<Vec2> waypoints, boolean isOpen){
        this.isOpen = isOpen;
        createPath(waypoints);
        nearestPointOnCurrentSegment = waypoints.first().cpy();
        nearestPointOnPath = waypoints.first().cpy();
        tmpB = waypoints.first().cpy();
        tmpC = waypoints.first().cpy();
    }

    @Override
    public boolean isOpen(){
        return isOpen;
    }

    @Override
    public float getLength(){
        return pathLength;
    }

    @Override
    public Vec2 getStartPoint(){
        return segments.first().begin;
    }

    @Override
    public Vec2 getEndPoint(){
        return segments.peek().end;
    }

    /**
     * Returns the square distance of the nearest point on line segment {@code a-b}, from point {@code c}. Also, the {@code out}
     * vector is assigned to the nearest point.
     * @param out the output vector that contains the nearest point on return
     * @param a the start point of the line segment
     * @param b the end point of the line segment
     * @param c the point to calculate the distance from
     */
    public float calculatePointSegmentSquareDistance(Vec2 out, Vec2 a, Vec2 b, Vec2 c){
        out.set(a);
        tmpB.set(b);
        tmpC.set(c);

        Vec2 ab = tmpB.sub(a);
        float abLen2 = ab.len2();
        if(abLen2 != 0){
            float t = (tmpC.sub(a)).dot(ab) / abLen2;
            out.mulAdd(ab, Mathf.clamp(t, 0, 1));
        }

        return out.dst2(c);
    }

    @Override
    public LinePathParam createParam(){
        return new LinePathParam();
    }

    // We pass the last parameter value to the path in order to calculate the current
    // parameter value. This is essential to avoid nasty problems when lines are close together.
    // We should limit the algorithm to only considering areas of the path close to the previous
    // parameter value. The character is unlikely to have moved far, after all.
    // This technique, assuming the new value is close to the old one, is called coherence, and it is a
    // feature of many geometric algorithms.
    // TODO: Currently coherence is not implemented.
    @Override
    public float calculateDistance(Vec2 agentCurrPos, LinePathParam parameter){
        // Find the nearest segment
        float smallestDistance2 = Float.POSITIVE_INFINITY;
        Segment nearestSegment = null;
        for(int i = 0; i < segments.size; i++){
            Segment segment = segments.get(i);
            float distance2 = calculatePointSegmentSquareDistance(nearestPointOnCurrentSegment, segment.begin, segment.end,
            agentCurrPos);

            // first point
            if(distance2 < smallestDistance2){
                nearestPointOnPath.set(nearestPointOnCurrentSegment);
                smallestDistance2 = distance2;
                nearestSegment = segment;
                parameter.segmentIndex = i;
            }
        }

        // Distance from path start
        float lengthOnPath = nearestSegment.cumulativeLength - nearestPointOnPath.dst(nearestSegment.end);

        parameter.setDistance(lengthOnPath);

        return lengthOnPath;
    }

    @Override
    public void calculateTargetPosition(Vec2 out, LinePathParam param, float targetDistance){
        if(isOpen){
            // Open path support
            if(targetDistance < 0){
                // Clamp target distance to the min
                targetDistance = 0;
            }else if(targetDistance > pathLength){
                // Clamp target distance to the max
                targetDistance = pathLength;
            }
        }else{
            // Closed path support
            if(targetDistance < 0){
                // Backwards
                targetDistance = pathLength + (targetDistance % pathLength);
            }else if(targetDistance > pathLength){
                // Forward
                targetDistance = targetDistance % pathLength;
            }
        }

        // Walk through lines to see on which line we are
        Segment desiredSegment = null;
        for(int i = 0; i < segments.size; i++){
            Segment segment = segments.get(i);
            if(segment.cumulativeLength >= targetDistance){
                desiredSegment = segment;
                break;
            }
        }

        // begin-------targetPos-------end
        float distance = desiredSegment.cumulativeLength - targetDistance;

        out.set(desiredSegment.begin).sub(desiredSegment.end).scl(distance / desiredSegment.length).add(desiredSegment.end);
    }

    /**
     * Sets up this {@link Path} using the given way points.
     * @param waypoints The way points of this path.
     * @throws IllegalArgumentException if {@code waypoints} is {@code null} or empty.
     */
    public void createPath(Array<Vec2> waypoints){
        if(waypoints == null || waypoints.size < 2)
            throw new IllegalArgumentException("waypoints cannot be null and must contain at least two (2) waypoints");

        segments = new Array<>(waypoints.size);
        pathLength = 0;
        Vec2 curr = waypoints.first();
        Vec2 prev = null;
        for(int i = 1; i <= waypoints.size; i++){
            prev = curr;
            if(i < waypoints.size)
                curr = waypoints.get(i);
            else if(isOpen)
                break; // keep the path open
            else
                curr = waypoints.first(); // close the path
            Segment segment = new Segment(prev, curr);
            pathLength += segment.length;
            segment.cumulativeLength = pathLength;
            segments.add(segment);
        }
    }

    public Array<Segment> getSegments(){
        return segments;
    }

    /**
     * A {@code LinePathParam} contains the status of a {@link LinePath}.
     * @author davebaol
     */
    public static class LinePathParam implements Path.PathParam{
        int segmentIndex;
        float distance;

        @Override
        public float getDistance(){
            return distance;
        }

        @Override
        public void setDistance(float distance){
            this.distance = distance;
        }

        /** Returns the index of the current segment along the path */
        public int getSegmentIndex(){
            return segmentIndex;
        }

    }

    /**
     * A {@code Segment} connects two consecutive waypoints of a {@link LinePath}.
     * @author davebaol
     */
    public static class Segment{
        Vec2 begin;
        Vec2 end;
        float length;
        float cumulativeLength;

        /**
         * Creates a {@code Segment} for the 2 given points.
         */
        Segment(Vec2 begin, Vec2 end){
            this.begin = begin;
            this.end = end;
            this.length = begin.dst(end);
        }

        /** Returns the start point of this segment. */
        public Vec2 getBegin(){
            return begin;
        }

        /** Returns the end point of this segment. */
        public Vec2 getEnd(){
            return end;
        }

        /** Returns the length of this segment. */
        public float getLength(){
            return length;
        }

        /** Returns the cumulative length from the first waypoint of the {@link LinePath} this segment belongs to. */
        public float getCumulativeLength(){
            return cumulativeLength;
        }
    }
}
