package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.VertexAttributes.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.util.*;

public class CubemapMesh implements Disposable{
    private static final float[] vertices = {
    -1.0f,  1.0f, -1.0f,
    -1.0f, -1.0f, -1.0f,
    1.0f, -1.0f, -1.0f,
    1.0f, -1.0f, -1.0f,
    1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f, -1.0f,

    -1.0f, -1.0f,  1.0f,
    -1.0f, -1.0f, -1.0f,
    -1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f,  1.0f,
    -1.0f, -1.0f,  1.0f,

    1.0f, -1.0f, -1.0f,
    1.0f, -1.0f,  1.0f,
    1.0f,  1.0f,  1.0f,
    1.0f,  1.0f,  1.0f,
    1.0f,  1.0f, -1.0f,
    1.0f, -1.0f, -1.0f,

    -1.0f, -1.0f,  1.0f,
    -1.0f,  1.0f,  1.0f,
    1.0f,  1.0f,  1.0f,
    1.0f,  1.0f,  1.0f,
    1.0f, -1.0f,  1.0f,
    -1.0f, -1.0f,  1.0f,

    -1.0f,  1.0f, -1.0f,
    1.0f,  1.0f, -1.0f,
    1.0f,  1.0f,  1.0f,
    1.0f,  1.0f,  1.0f,
    -1.0f,  1.0f,  1.0f,
    -1.0f,  1.0f, -1.0f,

    -1.0f, -1.0f, -1.0f,
    -1.0f, -1.0f,  1.0f,
    1.0f, -1.0f, -1.0f,
    1.0f, -1.0f, -1.0f,
    -1.0f, -1.0f,  1.0f,
    1.0f, -1.0f,  1.0f
    };

    private final Mesh mesh;
    private final Shader shader;
    private Cubemap map;

    public CubemapMesh(Cubemap map){
        this.map = map;
        this.map.setFilter(TextureFilter.linear);
        this.mesh = new Mesh(true, vertices.length, 0,
            new VertexAttribute(Usage.position, 3, "a_position")
        );
        mesh.getVerticesBuffer().limit(vertices.length);
        mesh.getVerticesBuffer().put(vertices, 0, vertices.length);

        shader = new Shader(Core.files.internal("shaders/cubemap.vert"), Core.files.internal("shaders/cubemap.frag"));
    }

    public void setCubemap(Cubemap map){
        this.map = map;
    }

    public void render(Mat3D projection){
        map.bind();
        shader.bind();
        shader.setUniformi("u_cubemap", 0);
        shader.setUniformMatrix4("u_proj", projection.val);
        mesh.render(shader, Gl.triangles);
    }

    @Override
    public void dispose(){
        mesh.dispose();
        map.dispose();
    }
}
