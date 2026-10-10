package arc.graphics.g3d;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;

public class VertexBatch3D{
    private final int maxVertices;
    private final Mesh mesh;
    private final boolean hasTexCoords;
    private final int vertexSize;
    private final int normalOffset;
    private final int colorOffset;
    private final int texCoordOffset;
    private final Mat3D proj = new Mat3D();
    private final float[] vertices;

    private int vertexIdx;
    private int numSetTexCoords;
    private int numVertices;
    private Shader shader;
    private boolean ownsShader;

    public VertexBatch3D(boolean hasNormals, boolean hasColors, boolean hasTexCoords){
        this(5000, hasNormals, hasColors, hasTexCoords, createDefaultShader(hasNormals, hasColors, hasTexCoords));
        ownsShader = true;
    }

    public VertexBatch3D(int maxVertices, boolean hasNormals, boolean hasColors, boolean hasTexCoords){
        this(maxVertices, hasNormals, hasColors, hasTexCoords, createDefaultShader(hasNormals, hasColors, hasTexCoords));
        ownsShader = true;
    }

    public VertexBatch3D(int maxVertices, boolean hasNormals, boolean hasColors, boolean hasTexCoords, Shader shader){
        this.maxVertices = maxVertices;
        this.hasTexCoords = hasTexCoords;
        this.shader = shader;

        VertexAttribute[] attribs = buildVertexAttributes(hasNormals, hasColors, hasTexCoords);
        mesh = new Mesh(false, maxVertices, 0, attribs);

        vertices = new float[maxVertices * (mesh.vertexSize / 4)];
        vertexSize = mesh.vertexSize / 4;

        int offset = 3;

        if(hasNormals){
            normalOffset = offset;
            offset += 3;
        }else{
            normalOffset = 0;
        }

        if(hasColors){
            colorOffset = offset;
            offset += 1;
        }else{
            colorOffset = 0;
        }

        texCoordOffset = offset;
    }

    private static String createVertexShader(boolean hasNormals, boolean hasColors, boolean hasTexCoords){
        StringBuilder shader = new StringBuilder("attribute vec4 " + Shader.positionAttribute + ";\n"
        + (hasNormals ? "attribute vec3 " + Shader.normalAttribute + ";\n" : "")
        + (hasColors ? "attribute vec4 " + Shader.colorAttribute + ";\n" : ""));

        if(hasTexCoords){
            shader.append("attribute vec3 " + Shader.texcoordAttribute + "0").append(";\n");
        }

        shader.append("uniform mat4 u_proj;\n");
        shader.append(hasColors ? "varying vec4 v_col;\n" : "");

        if(hasTexCoords){
            shader.append("varying vec3 v_tex").append(";\n");
        }

        shader.append("void main() {\n" + "   gl_Position = u_proj * " + Shader.positionAttribute + ";\n").append(hasColors ? "   v_col = " + Shader.colorAttribute + ";\n" : "");

        if(hasTexCoords){
            shader.append("   v_tex").append(" = ").append(Shader.texcoordAttribute + "0").append(";\n");
        }

        shader.append("   gl_PointSize = 1.0;\n");
        shader.append("}\n");
        return shader.toString();
    }

    private static String createFragmentShader(boolean hasNormals, boolean hasColors, boolean hasTexCoords){
        StringBuilder shader = new StringBuilder();

        if(hasColors) shader.append("varying vec4 v_col;\n");
        if(hasTexCoords){
            shader.append("varying vec3 v_tex").append(";\n");
            shader.append("uniform highp sampler2DArray u_sampler").append(";\n");
        }

        shader.append("void main(){\n   gl_FragColor = ").append(hasColors ? "v_col" : "vec4(1, 1, 1, 1)");

        if(hasTexCoords) shader.append(" * ");

        if(hasTexCoords){
            shader.append(" texture(u_sampler, v_tex)");
        }

        shader.append(";\n}");
        return shader.toString();
    }

    /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified. */
    public static Shader createDefaultShader(boolean hasNormals, boolean hasColors, boolean hasTexCoords){
        return new Shader(createVertexShader(hasNormals, hasColors, hasTexCoords), createFragmentShader(hasNormals, hasColors, hasTexCoords));
    }

    private VertexAttribute[] buildVertexAttributes(boolean hasNormals, boolean hasColor, boolean hasTexCoords){
        Seq<VertexAttribute> attribs = new Seq<>();
        attribs.add(VertexAttribute.position3);
        if(hasNormals) attribs.add(VertexAttribute.normal);
        if(hasColor) attribs.add(VertexAttribute.color);
        if(hasTexCoords) attribs.add(new VertexAttribute(3, Shader.texcoordAttribute + "0"));
        return attribs.toArray(VertexAttribute.class);
    }

    public void setShader(Shader shader){
        if(ownsShader) this.shader.dispose();
        this.shader = shader;
        ownsShader = false;
    }

    public void color(Color color){
        vertices[vertexIdx + colorOffset] = color.toFloatBits();
    }

    public void color(float r, float g, float b, float a){
        vertices[vertexIdx + colorOffset] = Color.toFloatBits(r, g, b, a);
    }

    public void color(float colorBits){
        vertices[vertexIdx + colorOffset] = colorBits;
    }

    public void texCoord(float u, float v){
        final int idx = vertexIdx + texCoordOffset;
        vertices[idx + numSetTexCoords] = u;
        vertices[idx + numSetTexCoords + 1] = v;
        numSetTexCoords += 2;
    }

    public void normal(Vec3 v){
        normal(v.x, v.y, v.z);
    }

    public void normal(float x, float y, float z){
        final int idx = vertexIdx + normalOffset;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = z;
    }

    public void tri2(Vec3 v1, Vec3 v2, Vec3 v3, Color color){
        tri(v1, v2, v3, color);
        tri(v1, v3, v2, color);
    }

    public void tri(Vec3 v1, Vec3 v2, Vec3 v3, Color color){
        tri(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z, color);
    }

    public void tri(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, Color color){
        float bits = color.toFloatBits();
        vertex(x1, y1, z1, bits);
        vertex(x2, y2, z2, bits);
        vertex(x3, y3, z3, bits);
    }

    public void quad(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, Color color){
        tri(v1, v2, v3, color);
        tri(v1, v3, v4, color);
    }

    public void vertex(Vec3 v, Color color){
        color(color);
        vertex(v.x, v.y, v.z);
    }

    public void vertex(Vec3 v){
        vertex(v.x, v.y, v.z);
    }

    public void vertex(float x, float y, float z, float color){
        final int idx = vertexIdx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = z;
        vertices[idx + colorOffset] = color;

        numSetTexCoords = 0;
        vertexIdx += vertexSize;
        numVertices++;
    }

    public void vertex(float x, float y, float z){
        final int idx = vertexIdx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = z;

        numSetTexCoords = 0;
        vertexIdx += vertexSize;
        numVertices++;
    }

    public void vertex(float[] floats){
        System.arraycopy(floats, 0, vertices, vertexIdx, vertexSize);
        vertexIdx += vertexSize;
        numVertices ++;
    }

    public Mat3D proj(){
        return proj;
    }

    public void proj(Mat3D projModelView){
        this.proj.set(projModelView);
    }

    public void flush(int primitiveType){
        flush(primitiveType, this.shader);
    }

    public void flush(int primitiveType, Shader shader){
        if(numVertices == 0) return;
        shader.bind();
        shader.apply();
        shader.setUniformMatrix4("u_proj", proj.val);
        shader.setUniformi("u_sampler", 0);
        mesh.setVertices(vertices, 0, vertexIdx);
        mesh.render(shader, primitiveType);

        numSetTexCoords = 0;
        vertexIdx = 0;
        numVertices = 0;
    }

    public int getNumVertices(){
        return numVertices;
    }

    public int getMaxVertices(){
        return maxVertices;
    }

    public void dispose(){
        if(ownsShader && shader != null) shader.dispose();
        mesh.dispose();
    }
}
