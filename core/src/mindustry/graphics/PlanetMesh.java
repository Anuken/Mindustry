package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.VertexAttributes.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.graphics.PlanetGrid.*;

public class PlanetMesh{
    private static final Vec3 v1 = new Vec3(), v2 = new Vec3(), v3 = new Vec3(), v4 = new Vec3();

    private float[] floats = new float[3 + 3 + 1];
    private Vec3 vec = new Vec3();
    private Mesh mesh;
    private PlanetGrid grid;
    private Vec3 center = new Vec3();

    private boolean lines;
    private float radius, intensity = 0.2f;

    private final PlanetMesher gen;

    public PlanetMesh(int divisions, PlanetMesher gen){
        this(divisions, gen, 1f, false);
    }

    public PlanetMesh(int divisions, PlanetMesher gen, float radius, boolean lines){
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

    public void render(Mat3D mat){
        render(mat, Shaders.planet);
    }

    public void render(Mat3D mat, Shader shader){
        shader.begin();
        shader.setUniformMatrix4("u_projModelView", mat.val);
        shader.apply();
        mesh.render(shader, lines ? Gl.lines : Gl.triangles);
        shader.end();
    }


    private void generateMesh(){
        for(Ptile tile : grid.tiles){

            Vec3 nor = v1.setZero();
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

    //unused, but functional
    private void createIcosphere(){
        MeshResult result = Icosphere.create(5);
        for(int i = 0; i < result.indices.size; i+= 3){
            v1.set(result.vertices.items, result.indices.items[i] * 3).setLength(radius).setLength(radius + elevation(v1)*intensity);
            v2.set(result.vertices.items, result.indices.items[i + 1] * 3).setLength(radius).setLength(radius + elevation(v2)*intensity);
            v3.set(result.vertices.items, result.indices.items[i + 2] * 3).setLength(radius).setLength(radius + elevation(v3)*intensity);

            verts(v1, v3, v2,
                normal(v1, v2, v3).scl(-1f),
                color(v4.set(v1).add(v2).add(v3).scl(1f / 3f))
            );
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
