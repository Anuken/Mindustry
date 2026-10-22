package arc.math.geom;

import arc.math.*;
import arc.math.geom.Plane.*;

/** A truncated rectangular pyramid. Used to define the viewable region and its projection onto the screen. */
public class Frustum{
    protected static final Vec3[] clipSpacePlanePoints = {
        new Vec3(-1, -1, -1), new Vec3(1, -1, -1), new Vec3(1, 1, -1), new Vec3(-1, 1, -1), // near clip
        new Vec3(-1, -1, 1), new Vec3(1, -1, 1), new Vec3(1, 1, 1), new Vec3(-1, 1, 1) // far clip
    };
    protected static final float[] clipSpacePlanePointsArray = new float[8 * 3];
    private final static Vec3 tmpV = new Vec3();

    static{
        int j = 0;
        for(Vec3 v : clipSpacePlanePoints){
            clipSpacePlanePointsArray[j++] = v.x;
            clipSpacePlanePointsArray[j++] = v.y;
            clipSpacePlanePointsArray[j++] = v.z;
        }
    }

    /** the six clipping planes, near, far, left, right, top, bottom **/
    public final Plane[] planes = new Plane[6];

    /** eight points making up the near and far clipping "rectangles". order is counterclockwise, starting at bottom left **/
    public final Vec3[] planePoints = {new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3(), new Vec3()};
    protected final float[] planePointsArray = new float[8 * 3];

    public Frustum(){
        for(int i = 0; i < 6; i++){
            planes[i] = new Plane(new Vec3(), 0);
        }
    }

    /**
     * Updates the clipping plane's based on the given inverse combined projection and view matrix.
     * @param inverseProjectionView the combined projection and view matrices.
     */
    public void update(Mat3D inverseProjectionView){
        System.arraycopy(clipSpacePlanePointsArray, 0, planePointsArray, 0, clipSpacePlanePointsArray.length);
        Mat3D.prj(inverseProjectionView.val, planePointsArray, 0, 8, 3);
        for(int i = 0, j = 0; i < 8; i++){
            Vec3 v = planePoints[i];
            v.x = planePointsArray[j++];
            v.y = planePointsArray[j++];
            v.z = planePointsArray[j++];
        }

        planes[0].set(planePoints[1], planePoints[0], planePoints[2]);
        planes[1].set(planePoints[4], planePoints[5], planePoints[7]);
        planes[2].set(planePoints[0], planePoints[4], planePoints[3]);
        planes[3].set(planePoints[5], planePoints[1], planePoints[6]);
        planes[4].set(planePoints[2], planePoints[3], planePoints[6]);
        planes[5].set(planePoints[4], planePoints[0], planePoints[1]);
    }

    /** @return whether the point is in the frustum. */
    public boolean containsPoint(Vec3 point){
        for(Plane plane : planes){
            if(plane.testPoint(point) == PlaneSide.back) return false;
        }
        return true;
    }

    /**
     * @param x The X coordinate of the point
     * @param y The Y coordinate of the point
     * @param z The Z coordinate of the point
     * @return whether the point is in the frustum.
     */
    public boolean containsPoint(float x, float y, float z){
        for(Plane plane : planes){
            if(plane.testPoint(x, y, z) == PlaneSide.back) return false;
        }
        return true;
    }

    /**
     * @param center The center of the sphere
     * @param radius The radius of the sphere
     * @return whether the sphere is in the frustum
     */
    public boolean containsSphere(Vec3 center, float radius){
        for(int i = 0; i < 6; i++)
            if((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * Returns whether the given sphere is in the frustum.
     * @param x The X coordinate of the center of the sphere
     * @param y The Y coordinate of the center of the sphere
     * @param z The Z coordinate of the center of the sphere
     * @param radius The radius of the sphere
     * @return whether the sphere is in the frustum
     */
    public boolean containsSphere(float x, float y, float z, float radius){
        for(int i = 0; i < 6; i++)
            if((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d)) return false;
        return true;
    }

    /**
     * @param center The center of the sphere
     * @param radius The radius of the sphere
     * @return whether the sphere is in the frustum,  not checking whether it is behind the near and far clipping plane.
     */
    public boolean containsSphereWithoutNearFar(Vec3 center, float radius){
        for(int i = 2; i < 6; i++)
            if((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
                return false;
        return true;
    }

    /**
     * @param x The X coordinate of the center of the sphere
     * @param y The Y coordinate of the center of the sphere
     * @param z The Z coordinate of the center of the sphere
     * @param radius The radius of the sphere
     * @return Whether the sphere is in the frustum,  not checking whether it is behind the near and far clipping plane.
     */
    public boolean containsSphereWithoutNearFar(float x, float y, float z, float radius){
        for(int i = 2; i < 6; i++)
            if((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d)) return false;
        return true;
    }

    /** @return Whether the bounding box is in the frustum */
    public boolean containsBounds(BoundingBox bounds){
        for(Plane plane : planes){
            if(plane.testPoint(bounds.getCorner000(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner001(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner010(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner011(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner100(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner101(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner110(tmpV)) != PlaneSide.back) continue;
            if(plane.testPoint(bounds.getCorner111(tmpV)) != PlaneSide.back) continue;
            return false;
        }

        return true;
    }

    /** @return Whether the bounding box is in the frustum */
    public boolean containsBounds(Vec3 center, Vec3 dimensions){
        return containsBounds(center.x, center.y, center.z, dimensions.x / 2, dimensions.y / 2, dimensions.z / 2);
    }

    /** @return Whether the bounding box is in the frustum */
    public boolean containsBounds(float x, float y, float z, float halfWidth, float halfHeight, float halfDepth){
        for(Plane plane : planes){
            if(plane.testPoint(x + halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x + halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x + halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x + halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.back) continue;
            if(plane.testPoint(x - halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.back) continue;
            return false;
        }

        return true;
    }
}