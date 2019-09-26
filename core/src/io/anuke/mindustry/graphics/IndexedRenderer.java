package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.VertexAttributes.Usage;
import io.anuke.arc.graphics.g2d.BatchShader;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.graphics.glutils.Shader;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.Matrix3;
import io.anuke.arc.util.Disposable;
import io.anuke.arc.util.Strings;

//TODO this class is a trainwreck, remove it
public class IndexedRenderer implements Disposable{
    private final static int vsize = 5;

    private Shader program = new Shader(
    Strings.join("\n",
    "attribute vec4 " + Shader.POSITION_ATTRIBUTE + ";",
    "attribute vec4 " + Shader.COLOR_ATTRIBUTE + ";",
    "attribute vec2 " + Shader.TEXCOORD_ATTRIBUTE + "0;",
    "uniform mat4 u_projTrans;",
    "varying vec4 v_color;",
    "varying vec2 v_texCoords;",
    "",
    "void main(){",
    "   v_color = " + Shader.COLOR_ATTRIBUTE + ";",
    "   v_color.a = v_color.a * (255.0/254.0);",
    "   v_texCoords = " + Shader.TEXCOORD_ATTRIBUTE + "0;",
    "   gl_Position = u_projTrans * " + Shader.POSITION_ATTRIBUTE + ";",
    "}"),
    Strings.join("\n",
    "#ifdef GL_ES",
    "#define LOWP lowp",
    "precision mediump float;",
    "#else",
    "#define LOWP ",
    "#endif",
    "",
    "varying LOWP vec4 v_color;",
    "varying vec2 v_texCoords;",
    "uniform sampler2D u_texture;",
    "",
    "void main(){",
    "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);",
    "}"
    ));
    private Mesh mesh;
    private float[] tmpVerts = new float[vsize * 6];
    private float[] vertices;

    private Matrix3 projMatrix = new Matrix3();
    private Matrix3 transMatrix = new Matrix3();
    private Matrix3 combined = new Matrix3();
    private float color = Color.white.toFloatBits();

    public IndexedRenderer(int sprites){
        resize(sprites);
    }

    public void render(Texture texture){
        Core.gl.glEnable(GL20.GL_BLEND);

        updateMatrix();

        program.begin();

        texture.bind();

        program.setUniformMatrix4("u_projTrans", BatchShader.copyTransform(combined));
        program.setUniformi("u_texture", 0);

        mesh.render(program, GL20.GL_TRIANGLES, 0, vertices.length / vsize);

        program.end();
    }

    public void setColor(Color color){
        this.color = color.toFloatBits();
    }

    public void draw(int index, TextureRegion region, float x, float y, float w, float h){
        final float fx2 = x + w;
        final float fy2 = y + h;
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        float[] vertices = tmpVerts;

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
        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        mesh.updateVertices(index * vsize * 6, vertices);
    }

    public void draw(int index, TextureRegion region, float x, float y, float w, float h, float rotation){
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        final float originX = w / 2, originY = h / 2;

        final float cos = Mathf.cosDeg(rotation);
        final float sin = Mathf.sinDeg(rotation);

        float fx = -originX;
        float fy = -originY;
        float fx2 = w - originX;
        float fy2 = h - originY;

        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;

        float x1 = cos * fx - sin * fy;
        float y1 = sin * fx + cos * fy;

        float x2 = cos * fx - sin * fy2;
        float y2 = sin * fx + cos * fy2;

        float x3 = cos * fx2 - sin * fy2;
        float y3 = sin * fx2 + cos * fy2;

        float x4 = x1 + (x3 - x2);
        float y4 = y3 - (y2 - y1);

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        float[] vertices = tmpVerts;

        int idx = 0;
        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        vertices[idx++] = x4;
        vertices[idx++] = y4;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;

        //tri2
        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;

        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;

        mesh.updateVertices(index * vsize * 6, vertices);
    }

    public Matrix3 getTransformMatrix(){
        return transMatrix;
    }

    public void setProjectionMatrix(Matrix3 matrix){
        projMatrix = matrix;
    }

    public void resize(int sprites){
        if(mesh != null) mesh.dispose();

        mesh = new Mesh(true, 6 * sprites, 0,
        new VertexAttribute(Usage.Position, 2, "a_position"),
        new VertexAttribute(Usage.ColorPacked, 4, "a_color"),
        new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));
        vertices = new float[6 * sprites * vsize];
        mesh.setVertices(vertices);
    }

    private void updateMatrix(){
        combined.set(projMatrix).mul(transMatrix);
    }

    @Override
    public void dispose(){
        mesh.dispose();
        program.dispose();
    }
}
