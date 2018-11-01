package io.anuke.ucore.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

public class IndexedRenderer implements Disposable{
    private final static int vsize = 5;

    private ShaderProgram program = createDefaultShader();
    private Mesh mesh;
    private float[] tmpVerts = new float[vsize * 6];
    private float[] vertices;

    private Matrix4 projMatrix = new Matrix4();
    private Matrix4 transMatrix = new Matrix4();
    private Matrix4 combined = new Matrix4();

    public IndexedRenderer(int sprites){
        resize(sprites);
    }

    public void render(Texture texture){
        Gdx.gl.glEnable(GL20.GL_BLEND);

        updateMatrix();

        program.begin();

        texture.bind();

        program.setUniformMatrix("u_projTrans", combined);
        program.setUniformi("u_texture", 0);

        mesh.render(program, GL20.GL_TRIANGLES, 0, vertices.length);

        program.end();
    }

    public void draw(int index, TextureRegion region, float x, float y, float z, float w, float h){
        final float fx2 = x + w;
        final float fy2 = y + h;
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        float[] vertices = tmpVerts;

        int idx = 0;
        vertices[idx ++] = x;
        vertices[idx ++] = y;
        vertices[idx ++] = z;
        vertices[idx ++] = u;
        vertices[idx ++] = v;

        vertices[idx ++] = x;
        vertices[idx ++] = fy2;
        vertices[idx ++] = z;
        vertices[idx ++] = u;
        vertices[idx ++] = v2;

        vertices[idx ++] = fx2;
        vertices[idx ++] = fy2;
        vertices[idx ++] = z;
        vertices[idx ++] = u2;
        vertices[idx ++] = v2;

        //tri2
        vertices[idx ++] = x;
        vertices[idx ++] = y;
        vertices[idx ++] = z;
        vertices[idx ++] = u;
        vertices[idx ++] = v;

        vertices[idx ++] = fx2;
        vertices[idx ++] = y;
        vertices[idx ++] = z;
        vertices[idx ++] = u2;
        vertices[idx ++] = v;

        vertices[idx ++] = fx2;
        vertices[idx ++] = fy2;
        vertices[idx ++] = z;
        vertices[idx ++] = u2;
        vertices[idx ++] = v2;

        mesh.updateVertices(index * vsize * 6, vertices);
    }

    public void setProjectionMatrix(Matrix4 matrix){
        projMatrix = matrix;
    }

    public void setTransformMatrix(Matrix4 matrix){
        transMatrix = matrix;
    }

    public Matrix4 getTransformMatrix() {
        return transMatrix;
    }

    public Matrix4 getProjectionMatrix() {
        return projMatrix;
    }

    public void resize(int sprites){
        if(mesh != null) mesh.dispose();

        mesh = new Mesh(true, 6*sprites, 0,
                new VertexAttribute(Usage.Position, 3, "a_position"),
                new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));
        vertices = new float[6*sprites*vsize];
        mesh.setVertices(vertices);
    }

    private void updateMatrix(){
        combined.set(projMatrix).mul(transMatrix);
    }

    @Override
    public void dispose() {
        mesh.dispose();
        program.dispose();
    }

    static public ShaderProgram createDefaultShader () {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "uniform mat4 u_projTrans;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "\n" //
                + "void main()\n" //
                + "{\n" //
                + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "}\n";
        String fragmentShader = "#ifdef GL_ES\n" //
                + "#define LOWP lowp\n" //
                + "precision mediump float;\n" //
                + "#else\n" //
                + "#define LOWP \n" //
                + "#endif\n" //
                + "varying vec2 v_texCoords;\n" //
                + "uniform sampler2D u_texture;\n" //
                + "void main()\n"//
                + "{\n" //
                + "  gl_FragColor = texture2D(u_texture, v_texCoords);\n" //
                + "}";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }
}
