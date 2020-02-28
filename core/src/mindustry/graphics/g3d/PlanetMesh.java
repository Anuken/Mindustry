package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.graphics.g3d.PlanetGrid.*;

public class PlanetMesh extends GenericMesh{
    private Vec3 vec = new Vec3();
    private PlanetGrid grid;
    private Vec3 center = new Vec3();

    private boolean lines;
    private float radius, intensity = 0.2f;

    private final PlanetMesher gen;

    public PlanetMesh(int divisions, PlanetMesher gen){
        this(divisions, gen, 1f, false);
    }

    public PlanetMesh(int divisions, PlanetMesher gen, float radius, boolean lines){
        super(PlanetGrid.create(divisions).tiles.length * 12 * (3 + 3 + 1), lines ? Gl.lines : Gl.triangles);

        this.gen = gen;
        this.radius = radius;
        this.grid = PlanetGrid.create(divisions);
        this.lines = lines;

        generateMesh();
    }

    public @Nullable Vec3 intersect(Ray ray){
        boolean found = Intersector3D.intersectRaySphere(ray, center, radius, Tmp.v33);
        if(!found) return null;
        return Tmp.v33;
    }

    /** @return the sector that is hit by this ray, or null if nothing intersects it. */
    public @Nullable Ptile getTile(Ray ray){
        boolean found = Intersector3D.intersectRaySphere(ray, center, radius, Tmp.v33);
        if(!found) return null;
        return Structs.findMin(grid.tiles, t -> t.v.dst(Tmp.v33));
    }

    private void generateMesh(){
        for(Ptile tile : grid.tiles){

            Vec3 nor = Tmp.v31.setZero();
            Corner[] c = tile.corners;

            for(Corner corner : c){
                corner.bv.set(corner.v).setLength(radius);
            }

            for(Corner corner : c){
                corner.v.setLength(radius + elevation(corner.bv)*intensity);
            }

            for(Corner corner : c){
                nor.add(corner.v);
            }
            nor.nor();

            Vec3 realNormal = normal(c[0].v, c[2].v, c[4].v);
            nor.set(realNormal);

            Color color = color(tile.v);

            if(lines){
                nor.set(1f, 1f, 1f);

                for(int i = 0; i < c.length; i++){
                    Vec3 v1 = c[i].v;
                    Vec3 v2 = c[(i + 1) % c.length].v;

                    vert(v1, nor, color);
                    vert(v2, nor, color);
                }
            }else{
                verts(c[0].v, c[1].v, c[2].v, nor, color);
                verts(c[0].v, c[2].v, c[3].v, nor, color);
                verts(c[0].v, c[3].v, c[4].v, nor, color);

                if(c.length > 5){
                    verts(c[0].v, c[4].v, c[5].v, nor, color);
                }else{
                    verts(c[0].v, c[3].v, c[4].v, nor, color);
                }
            }
        }
    }

    private float elevation(Vec3 v){
        return gen.getHeight(vec.set(v).scl(1f / radius));
    }

    private Color color(Vec3 v){
        return gen.getColor(vec.set(v).scl(1f / radius));
    }
}
