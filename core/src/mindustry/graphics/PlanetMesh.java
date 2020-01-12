package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.VertexAttributes.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.graphics.Pgrid.*;

public class PlanetMesh{
    private float[] floats = new float[3 + 3 + 1];
    private Vec3 center = new Vec3(0, 0, 0);
    private Mesh mesh;
    private Pgrid grid;

    private float color;
    private boolean lines;
    private float radius;

    private Simplex sim = new Simplex();
    private Color[] colors = {Color.royal, Color.royal, Color.royal, Color.tan, Color.valueOf("3f9a50"), Color.valueOf("3f9a50"), Color.gray, Color.white, Color.white};

    public PlanetMesh(int divisions, float radius, boolean lines, Color color){
        this.radius = radius;
        this.lines = lines;
        this.color = color.toFloatBits();
        this.grid = Pgrid.newGrid(divisions);

        int vertices = grid.tiles.length * 12 * (3 + 3 + 1);

        mesh = new Mesh(true, vertices, 0,
            new VertexAttribute(Usage.position, 3, Shader.positionAttribute),
            new VertexAttribute(Usage.normal, 3, Shader.normalAttribute),
            new VertexAttribute(Usage.colorPacked, 4, Shader.colorAttribute));

        mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
        mesh.getVerticesBuffer().position(0);

        generateMesh();
    }

    public void render(Mat3D mat){
        Shaders.planet.begin();
        Shaders.planet.setUniformMatrix4("u_projModelView", mat.val);
        mesh.render(Shaders.planet, lines ? Gl.lines : Gl.triangles);
        Shaders.planet.end();
    }

    public @Nullable Ptile getTile(Ray ray){
        Vec3 vec = intersect(ray);
        if(vec == null) return null;
        //TODO fix O(N) search
        return Structs.findMin(grid.tiles, t -> t.v.dst(vec));
    }

    public @Nullable Vec3 intersect(Ray ray){
        if(Intersector3D.intersectRaySphere(ray, center, radius, Tmp.v33)){
            return Tmp.v33;
        }
        return null;
    }

    private void generateMesh(){

        for(Ptile tile : grid.tiles){

            Vec3 nor = Tmp.v31.setZero();
            Corner[] c = tile.corners;

            for(Corner corner : c){
                corner.v.setLength(radius);
                nor.add(corner.v);
            }
            nor.nor();

            Color color = color(nor);

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

    private Color color(Vec3 v){
        float f = ((float)sim.octaveNoise3D(6, 0.6, 1 / 2.0, v.x, v.y, v.z));
        return colors[Mathf.clamp((int)(f * colors.length), 0, colors.length - 1)].cpy().mul(Mathf.round(Mathf.lerp(f*2f, 2f, 0.2f), 0.2f)).a(1f);
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
