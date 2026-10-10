package arc.graphics;

import arc.graphics.gl.*;
import arc.util.*;

import java.nio.*;

/**
 * <p>
 * A Mesh holds vertices composed of attributes specified by an array of {@link VertexAttribute} instances. The vertices are held either in
 * VRAM in form of vertex buffer objects.
 * </p>
 *
 * <p>
 * A Mesh consists of vertices and optionally indices which specify which vertices define a triangle. Each vertex is composed of
 * attributes such as position, normal, color or texture coordinate. Note that not all of this attributes must be given, except
 * for position which is non-optional. Each attribute has an alias which is used when rendering a Mesh. The alias
 * is used to bind a specific vertex attribute to a shader attribute. The shader source and the alias of the attribute must match
 * exactly for this to work.
 * </p>
 * @author mzechner, Dave Clayton <contact@redskyforge.com>, Xoppa
 */
public class Mesh implements Disposable{
    /** The size of one vertex, in bytes. */
    public final int vertexSize;
    /** Do not modify. */
    public final VertexAttribute[] attributes;

    public VertexBufferObject vertices;
    public IndexBufferObject indices;

    /**
     * Creates a new Mesh with the given attributes.
     * @param isStatic whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices the maximum number of indices this mesh can hold
     */
    public Mesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttribute... attributes){
        int count = 0;
        for(VertexAttribute attribute : attributes){
            count += attribute.size;
        }

        this.vertexSize = count;
        this.attributes = attributes;

        vertices = new VertexBufferObject(isStatic, maxVertices, this);
        indices = new IndexBufferObject(isStatic, maxIndices);
    }

    /**
     * Binds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} if indices where given. Use this with OpenGL
     * ES 2.0 and when auto-bind is disabled.
     */
    public void bind(final Shader shader){
        vertices.bind(shader);
        if(indices.size() > 0) indices.bind();
    }

    /**
     * Unbinds the underlying {@link VertexBufferObject} and {@link IndexBufferObject} is indices were given. Use this when auto-bind is disabled.
     */
    public void unbind(final Shader shader){
        vertices.unbind(shader);
        if(indices.size() > 0) indices.unbind();
    }

    /** @see #render(Shader, int, int, int, boolean) */
    public void render(Shader shader, int primitiveType){
        render(shader, primitiveType, 0, indices.max() > 0 ? getNumIndices() : getNumVertices(), true);
    }

    /** @see #render(Shader, int, int, int, boolean) */
    public void render(Shader shader, int primitiveType, int offset, int count){
        render(shader, primitiveType, offset, count, true);
    }

    /**
     * <p>
     * Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     * </p>
     * <p>
     * This method will automatically bind each vertex attribute as specified at construction time to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     * </p>
     * <p>
     * This method must only be called after the {@link Shader#bind()} method has been called!
     * </p>
     * @param shader the shader to be used
     * @param primitiveType the primitive type
     * @param offset the offset into the vertex or index buffer
     * @param count number of vertices or indices to use
     * @param autoBind overrides the autoBind member of this Mesh
     */
    public void render(Shader shader, int primitiveType, int offset, int count, boolean autoBind){
        if(count == 0) return;

        if(autoBind) bind(shader);

        vertices.render(indices, primitiveType, offset, count);

        if(autoBind) unbind(shader);
    }

    public Mesh setVertices(float[] vertices){
        this.vertices.set(vertices, 0, vertices.length);

        return this;
    }

    public Mesh setVertices(float[] vertices, int offset, int count){
        this.vertices.set(vertices, offset, count);

        return this;
    }

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source the vertex data to update the mesh part with
     */
    public Mesh updateVertices(int targetOffset, float[] source){
        return updateVertices(targetOffset, source, 0, source.length);
    }

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source the vertex data to update the mesh part with
     * @param sourceOffset the offset in number of floats within the source array
     * @param count the number of floats to update
     */
    public Mesh updateVertices(int targetOffset, float[] source, int sourceOffset, int count){
        this.vertices.update(targetOffset, source, sourceOffset, count);
        return this;
    }

    public Mesh setIndices(short[] indices){
        this.indices.set(indices, 0, indices.length);

        return this;
    }

    public Mesh setIndices(short[] indices, int offset, int count){
        this.indices.set(indices, offset, count);

        return this;
    }

    /** @return the number of defined indices */
    public int getNumIndices(){
        return indices.size();
    }

    /** @return the number of defined vertices */
    public int getNumVertices(){
        return vertices.size();
    }

    /** @return the maximum number of vertices this mesh can hold */
    public int getMaxVertices(){
        return vertices.max();
    }

    /** @return the maximum number of indices this mesh can hold */
    public int getMaxIndices(){
        return indices.max();
    }

    /** @return the backing FloatBuffer holding the vertices. Does not have to be a direct buffer on Android! */
    public FloatBuffer getVertices(){
        return vertices.buffer();
    }

    /** @return the backing shortbuffer holding the indices. Does not have to be a direct buffer on Android! */
    public ShortBuffer getIndices(){
        return indices.buffer();
    }

    /** Frees all resources associated with this Mesh */
    @Override
    public void dispose(){
        vertices.dispose();
        indices.dispose();
    }
}
