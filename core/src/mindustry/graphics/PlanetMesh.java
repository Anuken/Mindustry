package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.VertexAttributes.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.graphics.PlanetGrid.*;
import mindustry.maps.planet.*;

public class PlanetMesh{
    private float[] floats = new float[3 + 3 + 1];
    private Vec3 vec = new Vec3();
    private Mesh mesh;
    private PlanetGrid grid;
    private Vec3 center = new Vec3();

    private boolean lines;
    private float radius, intensity = 0.2f;

    private final PlanetGenerator gen;

    public PlanetMesh(int divisions, PlanetGenerator gen){
        this(divisions, gen, 1f, false);
    }

    public PlanetMesh(int divisions, PlanetGenerator gen, float radius, boolean lines){
        this.gen = gen;
        this.radius = radius;
        this.grid = PlanetGrid.newGrid(divisions);
        this.lines = lines;

        int vertices = grid.tiles.length * 12 * (3 + 3 + 1);

        mesh = new Mesh(true, vertices, 0,
            new VertexAttribute(Usage.position, 3, Shader.positionAttribute),
            new VertexAttribute(Usage.normal, 3, Shader.normalAttribute),
            new VertexAttribute(Usage.colorPacked, 4, Shader.colorAttribute));

        mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
        mesh.getVerticesBuffer().position(0);

        generateMesh();
    }

    /** @return the sector that is hit by this ray, or null if nothing intersects it. */
    public @Nullable Ptile getTile(Ray ray){
        boolean found = Intersector3D.intersectRaySphere(ray, center, radius, Tmp.v33);
        if(!found) return null;
        return Structs.findMin(grid.tiles, t -> t.v.dst(Tmp.v33));
    }

    public void render(Mat3D mat){
        Shaders.planet.begin();
        Shaders.planet.setUniformMatrix4("u_projModelView", mat.val);
        mesh.render(Shaders.planet, lines ? Gl.lines : Gl.triangles);
        Shaders.planet.end();
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

    private Vec3 normal(Vec3 v1, Vec3 v2, Vec3 v3){
        return Tmp.v32.set(v2).sub(v1).crs(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).nor();
    }

    private float elevation(Vec3 v){
        return gen.getHeight(vec.set(v).scl(1f / radius));
    }

    private Color color(Vec3 v){
        return gen.getColor(vec.set(v).scl(1f / radius));
    }

    private void verts(Vec3 a, Vec3 b, Vec3 c, Vec3 normal, Color color){
        vert(a, normal, color);
        vert(b, normal, color);
        vert(c, normal, color);
    }

    private void vert(Vec3 a, Vec3 normal, Color color){
        floats[0] = a.x;
        floats[1] = a.y;
        floats[2] = a.z;

        floats[3] = normal.x;
        floats[4] = normal.y;
        floats[5] = normal.z;

        floats[6] = color.toFloatBits();
        mesh.getVerticesBuffer().put(floats);
    }
}
