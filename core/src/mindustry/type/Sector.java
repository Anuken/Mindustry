package mindustry.type;

import arc.math.geom.*;
import arc.util.*;
import mindustry.graphics.PlanetGrid.*;

/** A small section of a planet. */
//TODO should this be content?
public class Sector{
    public final SectorRect rect;
    public final Planet planet;
    public final Ptile tile;

    public Sector(Planet planet, Ptile tile){
        this.planet = planet;
        this.tile = tile;
        this.rect = makeRect();
    }

    /** Projects this sector onto a 4-corner square for use in map gen.
     * Allocates a new object. Do not call in the main loop. */
    private SectorRect makeRect(){
        Vec3[] corners = new Vec3[tile.corners.length];
        for(int i = 0; i < corners.length; i++){
            corners[i] = tile.corners[i].v.cpy().setLength(planet.radius);
        }

        Tmp.v33.setZero();
        for(Vec3 c : corners){
            Tmp.v33.add(c);
        }
        //v33 is now the center of this shape
        Vec3 center = Tmp.v33.scl(1f / corners.length).cpy();
        //radius of circle
        float radius = Tmp.v33.dst(corners[0]) * 0.9f;

        //get plane that these points are on
        Plane plane = new Plane();
        plane.set(corners[0], corners[2], corners[4]);

        Vec3 planeTop = plane.project(center.cpy().add(0f, 1f, 0f)).sub(center).setLength(radius).add(center);
        Vec3 planeRight = plane.project(center.cpy().rotate(Vec3.Y, -4f)).sub(center).setLength(radius).add(center);

        return new SectorRect(radius, center, planeTop.sub(center), planeRight.sub(center));
    }

    public static class SectorRect{
        public final Vec3 center, top, right;
        public final Vec3 result = new Vec3();
        public final float radius;

        public SectorRect(float radius, Vec3 center, Vec3 top, Vec3 right){
            this.center = center;
            this.top = top;
            this.right = right;
            this.radius = radius;
        }

        /** Project a coordinate into 3D space.
         * Both coordinates should be normalized to floats in the range [0, 1] */
        public Vec3 project(float x, float y){
            float nx = (x - 0.5f) * 2f, ny = (y - 0.5f) * 2f;
            return result.set(center).add(right, nx).add(top, ny);
        }
    }
}
