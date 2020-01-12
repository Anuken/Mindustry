package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.VertexAttributes.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.graphics.Pgrid.*;

public class PlanetMesh{
    private float[] floats = new float[3 + 3 + 1];
    private Shader shader = new Shader(Core.files.internal("shaders/planet.vertex.glsl").readString(), Core.files.internal("shaders/planet.fragment.glsl").readString());
    private Mesh mesh;
    private Pgrid grid;

    private float color;
    private boolean lines;
    private float length;

    public PlanetMesh(int divisions, float length, boolean lines, Color color){
        this.length = length;
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
        Gl.enable(Gl.depthTest);

        shader.begin();
        shader.setUniformMatrix4("u_projModelView", mat.val);
        mesh.render(shader, lines ? Gl.lines : Gl.triangles);
        shader.end();

        Gl.disable(Gl.depthTest);
    }

    private void generateMesh(){

        for(Ptile tile : grid.tiles){

            Vec3 nor = Tmp.v31.setZero();
            Corner[] c = tile.corners;

            for(Corner corner : c){
                corner.v.setLength(length);
                nor.add(corner.v);
            }
            nor.nor();

            if(lines){
                nor.set(1f, 1f, 1f);

                for(int i = 0; i < c.length; i++){
                    Vec3 v1 = c[i].v;
                    Vec3 v2 = c[(i + 1) % c.length].v;

                    vert(v1, nor);
                    vert(v2, nor);
                }
            }else{
                verts(c[0].v, c[1].v, c[2].v, nor);
                verts(c[0].v, c[2].v, c[3].v, nor);
                verts(c[0].v, c[3].v, c[4].v, nor);

                if(c.length > 5){
                    verts(c[0].v, c[4].v, c[5].v, nor);
                }else{
                    verts(c[0].v, c[3].v, c[4].v, nor);
                }
            }
        }
    }

    private void verts(Vec3 a, Vec3 b, Vec3 c, Vec3 normal){
        vert(a, normal);
        vert(b, normal);
        vert(c, normal);
    }

    private void vert(Vec3 a, Vec3 normal){
        floats[0] = a.x;
        floats[1] = a.y;
        floats[2] = a.z;

        floats[3] = normal.x;
        floats[4] = normal.y;
        floats[5] = normal.z;

        floats[6] = color;
        mesh.getVerticesBuffer().put(floats);
    }
}
