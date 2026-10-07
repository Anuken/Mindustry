package arc.graphics.gl;

import arc.util.*;

import java.nio.*;

/**
 * <p>
 * In IndexBufferObject wraps OpenGL's index buffer functionality to be used in conjunction with VBOs.
 * </p>
 *
 * <p>
 * Uses indirect Buffers on Android 1.5/1.6 to fix GC invocation due to leaking PlatformAddress instances.
 * </p>
 *
 * <p>
 * You can also use this to store indices for vertex arrays. Do not call {@link #bind()} or {@link #unbind()} in this case but
 * rather use {@link #buffer()} to use the buffer directly with glDrawElements. You must also create the IndexBufferObject with
 * the second constructor and specify isDirect as true as glDrawElements in conjunction with vertex arrays needs direct buffers.
 * </p>
 *
 * <p>
 * VertexBufferObjects must be disposed via the {@link #dispose()} method when no longer needed.
 * </p>
 * @author mzechner, Thorsten Schleinzer
 */
public class IndexBufferObject implements Disposable{
    final ShortBuffer buffer;
    final ByteBuffer byteBuffer;
    final boolean isDirect;
    final int usage;
    // used to work around bug: https://android-review.googlesource.com/#/c/73175/
    final boolean empty;
    int bufferHandle;
    boolean dirty = true;
    boolean bound = false;
    boolean created;

    /**
     * Creates a new static IndexBufferObject to be used with vertex arrays.
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    public IndexBufferObject(int maxIndices){
        this(true, maxIndices);
    }

    /**
     * Creates a new IndexBufferObject.
     * @param isStatic whether the index buffer is static
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    public IndexBufferObject(boolean isStatic, int maxIndices){
        empty = maxIndices == 0;
        if(empty){
            maxIndices = 1; // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0
        }

        byteBuffer = Buffers.newUnsafeByteBuffer(maxIndices * 2);
        isDirect = true;

        buffer = byteBuffer.asShortBuffer();
        buffer.flip();
        byteBuffer.flip();
        usage = isStatic ? Gl.staticDraw : Gl.dynamicDraw;
    }

    /** @return the number of indices currently stored in this buffer */
    public int size(){
        return empty ? 0 : buffer.limit();
    }

    /** @return the maximum number of indices this IndexBufferObject can store. */
    public int max(){
        return empty ? 0 : buffer.capacity();
    }

    public void set(short[] indices){
        set(indices, 0, indices.length);
    }

    /**
     * <p>
     * Sets the indices of this IndexBufferObject, discarding the old indices. The count must equal the number of indices to be
     * copied to this IndexBufferObject.
     * </p>
     *
     * <p>
     * This can be called in between calls to {@link #bind()} and {@link #unbind()}. The index data will be updated instantly.
     * </p>
     * @param indices the vertex data
     * @param offset the offset to start copying the data from
     * @param count the number of shorts to copy
     */
    public void set(short[] indices, int offset, int count){
        dirty = true;
        buffer.clear();
        buffer.put(indices, offset, count);
        buffer.flip();
        byteBuffer.position(0);
        byteBuffer.limit(count << 1);

        if(bound){
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
    }

    public void set(ShortBuffer indices){
        dirty = true;
        int pos = indices.position();
        buffer.clear();
        buffer.put(indices);
        buffer.flip();
        indices.position(pos);
        byteBuffer.position(0);
        byteBuffer.limit(buffer.limit() << 1);

        if(bound){
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
    }

    public void update(int targetOffset, short[] indices, int offset, int count){
        dirty = true;
        final int pos = byteBuffer.position();
        byteBuffer.position(targetOffset * 2);
        Buffers.copy(indices, offset, byteBuffer, count);
        byteBuffer.position(pos);
        buffer.position(0);

        if(bound){
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
    }

    /**
     * <p>
     * Returns the underlying ShortBuffer. If you modify the buffer contents they wil be uploaded on the call to {@link #bind()}.
     * If you need immediate uploading use {@link #set(short[], int, int)}.
     * </p>
     * @return the underlying short buffer.
     */
    public ShortBuffer buffer(){
        dirty = true;
        return buffer;
    }

    /** Binds this IndexBufferObject for rendering with glDrawElements. */
    public void bind(){
        if(!created){
            bufferHandle = Gl.genBuffer();
            created = true;
        }
        if(bufferHandle == 0) throw new ArcRuntimeException("No buffer allocated!");

        Gl.bindBuffer(Gl.elementArrayBuffer, bufferHandle);
        if(dirty){
            byteBuffer.limit(buffer.limit() * 2);
            Gl.bufferData(Gl.elementArrayBuffer, byteBuffer.limit(), byteBuffer, usage);
            dirty = false;
        }
        bound = true;
    }

    /** Unbinds this IndexBufferObject. */
    public void unbind(){
        Gl.bindBuffer(Gl.elementArrayBuffer, 0);
        bound = false;
    }

    /** Disposes this IndexBufferObject and all its associated OpenGL resources. */
    @Override
    public void dispose(){
        Gl.bindBuffer(Gl.elementArrayBuffer, 0);
        Gl.deleteBuffer(bufferHandle);
        bufferHandle = 0;

        Buffers.disposeUnsafeByteBuffer(byteBuffer);
    }
}
