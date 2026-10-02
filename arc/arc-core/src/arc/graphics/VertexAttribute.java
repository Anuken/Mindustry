package arc.graphics;

import arc.graphics.gl.*;

/**
 * A single vertex attribute defined by its number of components and its shader alias. The number of components
 * defines how many components the attribute has. The alias defines to which shader attribute this attribute should bind. The alias
 * is used by a {@link Mesh} when drawing with a {@link Shader}.
 * @author mzechner
 */
public final class VertexAttribute{

    public static final VertexAttribute

    position = new VertexAttribute(2, Shader.positionAttribute),
    position3 = new VertexAttribute(3, Shader.positionAttribute),
    packedPosition = new VertexAttribute(2, Gl.unsignedShort, true, Shader.positionAttribute),
    texCoords = new VertexAttribute(2, Shader.texcoordAttribute + "0"),
    packedTexCoords = new VertexAttribute(2, Gl.unsignedShort, true, Shader.texcoordAttribute + "0"),
    depthCoords = new VertexAttribute(1, "a_depth"),
    texCoords3 = new VertexAttribute(3, Shader.texcoordAttribute + "0"),
    normal = new VertexAttribute(3, Shader.normalAttribute),
    packedNormal = new VertexAttribute(4, Gl.int2101010Rev, true, Shader.normalAttribute),
    color = new VertexAttribute(4, Gl.unsignedByte, true, Shader.colorAttribute),
    mixColor = new VertexAttribute(4, Gl.unsignedByte, true, Shader.mixColorAttribute);

    /** the number of components this attribute has **/
    public final int components;
    /** For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned) */
    public final boolean normalized;
    /** the OpenGL type of each component, e.g. {@link Gl#floatV} or {@link Gl#unsignedByte} */
    public final int type;
    /** the alias for the attribute used in a {@link Shader} **/
    public final String alias;
    /** the size (in bytes) of this attribute */
    public final int size;

    /**
     * Constructs a new VertexAttribute. The GL data type is automatically selected based on the usage.
     * @param components the number of components of this attribute, must be between 1 and 4.
     * @param alias the alias used in a shader for this attribute. Can be changed after construction.
     */
    public VertexAttribute(int components, String alias){
        this(components, Gl.floatV, false, alias);
    }

    /**
     * Constructs a new VertexAttribute.
     * @param components the number of components of this attribute, must be between 1 and 4.
     * @param type the OpenGL type of each component, e.g. {@link Gl#floatV} or {@link Gl#unsignedByte}. Since {@link Mesh}
     * stores vertex data in 32bit floats, the total size of this attribute (type size times number of components) must be a
     * multiple of four bytes.
     * @param normalized For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned)
     * @param alias The alias used in a shader for this attribute. Can be changed after construction.
     */
    public VertexAttribute(int components, int type, boolean normalized, String alias){
        this.components = components;
        this.type = type;
        this.normalized = normalized;
        this.alias = alias;

        //calculate final size based on components & type
        int realSize = 0;
        switch(type){
            case Gl.floatV:
            case Gl.fixed:
                realSize = 4 * components;
                break;
            case Gl.int2101010Rev:
            case Gl.unsignedInt2101010Rev:
            case Gl.unsignedByte:
            case Gl.byteV:
                realSize = components;
                break;
            case Gl.unsignedShort:
            case Gl.halfFloat:
            case Gl.shortV:
                realSize = 2 * components;
                break;
        }
        size = realSize;
    }
}
