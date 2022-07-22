package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.util.*;

public class IndexedRenderer implements Disposable{
    private static final int vsize = 5;

    private final static Shader program = new Shader(
    """
    attribute vec4 a_position;
    attribute vec4 a_color;
    attribute vec2 a_texCoord0;
    uniform mat4 u_projTrans;
    varying vec4 v_color;
    varying vec2 v_texCoords;
    void main(){
       v_color = a_color;
       v_color.a = v_color.a * (255.0/254.0);
       v_texCoords = a_texCoord0;
       gl_Position = u_projTrans * a_position;
    }
    """,

    """
    varying lowp vec4 v_color;
    varying vec2 v_texCoords;
    uniform sampler2D u_texture;
    void main(){
      gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
    }
    """
    );
    private static final float[] tmpVerts = new float[vsize * 6];

    private Mesh mesh;

    private Mat projMatrix = new Mat();
    private Mat transMatrix = new Mat();
    private Mat combined = new Mat();
    private float color = Color.white.toFloatBits();

    public IndexedRenderer(int sprites){
        resize(sprites);
    }

    public void render(Texture texture){
        Gl.enable(Gl.blend);

        updateMatrix();

        program.bind();
        texture.bind();

        program.setUniformMatrix4("u_projTrans", combined);

        mesh.render(program, Gl.triangles, 0, mesh.getMaxVertices());
    }

    public void setColor(Color color){
        this.color = color.toFloatBits();
    }

    public void draw(int index, TextureRegion region, float x, float y, float w, float h){
        float fx2 = x + w;
        float fy2 = y + h;
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;

        float[] vertices = tmpVerts;
        float color = this.color;

        int idx = 0;
        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        vertices[idx++] = x;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        //tri2
        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;

        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        mesh.updateVertices(index * vsize * 6, vertices);
    }

    public void draw(int index, TextureRegion region, float x, float y, float w, float h, float rotation){
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;

        float originX = w / 2, originY = h / 2;

        float cos = Mathf.cosDeg(rotation);
        float sin = Mathf.sinDeg(rotation);

        float fx = -originX;
        float fy = -originY;
        float fx2 = w - originX;
        float fy2 = h - originY;

        float worldOriginX = x + originX;
        float worldOriginY = y + originY;

        float x1 = cos * fx - sin * fy + worldOriginX;
        float y1 = sin * fx + cos * fy + worldOriginY;
        float x2 = cos * fx - sin * fy2 + worldOriginX;
        float y2 = sin * fx + cos * fy2 + worldOriginY;
        float x3 = cos * fx2 - sin * fy2 + worldOriginX;
        float y3 = sin * fx2 + cos * fy2 + worldOriginY;
        float x4 = x1 + (x3 - x2);
        float y4 = y3 - (y2 - y1);

        float[] vertices = tmpVerts;
        float color = this.color;

        int idx = 0;
        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;

        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        //tri2
        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        vertices[idx++] = x4;
        vertices[idx++] = y4;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;

        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        mesh.updateVertices(index * vsize * 6, vertices);
    }

    public Mat getTransformMatrix(){
        return transMatrix;
    }

    public void setProjectionMatrix(Mat matrix){
        projMatrix = matrix;
    }

    public void resize(int sprites){
        if(mesh != null) mesh.dispose();

        mesh = new Mesh(true, 6 * sprites, 0,
        VertexAttribute.position,
        VertexAttribute.color,
        VertexAttribute.texCoords);

        //TODO why is this the only way to get it working properly? it should not need an array
        mesh.setVertices(new float[6 * sprites * vsize]);
    }

    private void updateMatrix(){
        combined.set(projMatrix).mul(transMatrix);
    }

    @Override
    public void dispose(){
        mesh.dispose();
    }
}
