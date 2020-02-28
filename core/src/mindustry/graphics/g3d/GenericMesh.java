package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.graphics.VertexAttributes.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.graphics.*;

public class GenericMesh{
    protected final float[] floats = new float[3 + 3 + 1];
    protected final int primitiveType;
    protected final Mesh mesh;

    public GenericMesh(int vertices, int primitiveType){
        this.primitiveType = primitiveType;
        mesh = new Mesh(true, vertices, 0,
            new VertexAttribute(Usage.position, 3, Shader.positionAttribute),
            new VertexAttribute(Usage.normal, 3, Shader.normalAttribute),
            new VertexAttribute(Usage.colorPacked, 4, Shader.colorAttribute)
        );

        mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
        mesh.getVerticesBuffer().position(0);
    }

    public void render(Mat3D mat){
        render(mat, Shaders.planet);
    }

    public void render(Mat3D mat, Shader shader){
        shader.begin();
        shader.setUniformMatrix4("u_projModelView", mat.val);
        shader.apply();
        mesh.render(shader, primitiveType);
        shader.end();
    }

    protected Vec3 normal(Vec3 v1, Vec3 v2, Vec3 v3){
        return Tmp.v32.set(v2).sub(v1).crs(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z).nor();
    }

    protected void verts(Vec3 a, Vec3 b, Vec3 c, Vec3 normal, Color color){
        vert(a, normal, color);
        vert(b, normal, color);
        vert(c, normal, color);
    }

    protected void vert(Vec3 a, Vec3 normal, Color color){
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
