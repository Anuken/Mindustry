package arc.backend.robovm;

import arc.graphics.gl.*;

import java.nio.*;

public class IOSGLES30 extends IOSGLES20 implements GLProvider{

    public IOSGLES30(){
        init();
    }

    private static native void init();

    @Override
    public native void glReadBuffer(int mode);

    @Override
    public native void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices);

    @Override
    public native void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset);

    @Override
    public native void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, Buffer pixels);

    @Override
    public native void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, int offset);

    @Override
    public native void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, Buffer pixels);

    @Override
    public native void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, int offset);

    @Override
    public native void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height);

    @Override
    public native void glGenQueries(int n, IntBuffer ids);

    @Override
    public native void glDeleteQueries(int n, IntBuffer ids);

    @Override
    public native boolean glIsQuery(int id);

    @Override
    public native void glBeginQuery(int target, int id);

    @Override
    public native void glEndQuery(int target);

    @Override
    public native void glGetQueryiv(int target, int pname, IntBuffer params);

    @Override
    public native void glGetQueryObjectuiv(int id, int pname, IntBuffer params);

    @Override
    public native boolean glUnmapBuffer(int target);

    @Override
    public native Buffer glGetBufferPointerv(int target, int pname);

    @Override
    public native void glDrawBuffers(int n, IntBuffer bufs);

    @Override
    public native void glUniformMatrix2x3fv(int location, int count, boolean transpose, FloatBuffer value);

    @Override
    public native void glUniformMatrix3x2fv(int location, int count, boolean transpose, FloatBuffer value);

    @Override
    public native void glUniformMatrix2x4fv(int location, int count, boolean transpose, FloatBuffer value);

    @Override
    public native void glUniformMatrix4x2fv(int location, int count, boolean transpose, FloatBuffer value);

    @Override
    public native void glUniformMatrix3x4fv(int location, int count, boolean transpose, FloatBuffer value);

    @Override
    public native void glUniformMatrix4x3fv(int location, int count, boolean transpose, FloatBuffer value);

    @Override
    public native void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter);

    @Override
    public native void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height);

    @Override
    public native void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer);

    @Override
    public native void glFlushMappedBufferRange(int target, int offset, int length);

    @Override
    public native void glBindVertexArray(int array);

    @Override
    public native void glDeleteVertexArrays(int n, IntBuffer arrays);

    @Override
    public native void glGenVertexArrays(int n, IntBuffer arrays);

    @Override
    public native boolean glIsVertexArray(int array);

    @Override
    public native void glBeginTransformFeedback(int primitiveMode);

    @Override
    public native void glEndTransformFeedback();

    @Override
    public native void glBindBufferRange(int target, int index, int buffer, int offset, int size);

    @Override
    public native void glBindBufferBase(int target, int index, int buffer);

    @Override
    public native void glTransformFeedbackVaryings(int program, String[] varyings, int bufferMode);

    @Override
    public native void glVertexAttribIPointer(int index, int size, int type, int stride, int offset);

    @Override
    public native void glGetVertexAttribIiv(int index, int pname, IntBuffer params);

    @Override
    public native void glGetVertexAttribIuiv(int index, int pname, IntBuffer params);

    @Override
    public native void glVertexAttribI4i(int index, int x, int y, int z, int w);

    @Override
    public native void glVertexAttribI4ui(int index, int x, int y, int z, int w);

    @Override
    public native void glGetUniformuiv(int program, int location, IntBuffer params);

    @Override
    public native int glGetFragDataLocation(int program, String name);

    @Override
    public native void glUniform1uiv(int location, int count, IntBuffer value);

    @Override
    public native void glUniform3uiv(int location, int count, IntBuffer value);

    @Override
    public native void glUniform4uiv(int location, int count, IntBuffer value);

    @Override
    public native void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value);

    @Override
    public native void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value);

    @Override
    public native void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value);

    @Override
    public native void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil);

    @Override
    public native String glGetStringi(int name, int index);

    @Override
    public native void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size);

    @Override
    public native void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices);

    @Override
    public native void glGetActiveUniformsiv(int program, int uniformCount, IntBuffer uniformIndices, int pname, IntBuffer params);

    @Override
    public native int glGetUniformBlockIndex(int program, String uniformBlockName);

    @Override
    public native void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params);

    @Override
    public native void glGetActiveUniformBlockName(int program, int uniformBlockIndex, Buffer length, Buffer uniformBlockName);

    public native String glGetActiveUniformBlockName(int program, int uniformBlockIndex);

    @Override
    public native void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding);

    @Override
    public native void glDrawArraysInstanced(int mode, int first, int count, int instanceCount);

    @Override
    public native void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount);

    @Override
    public native void glGetInteger64v(int pname, LongBuffer params);

    @Override
    public native void glGetBufferParameteri64v(int target, int pname, LongBuffer params);

    @Override
    public native void glGenSamplers(int count, IntBuffer samplers);

    @Override
    public native void glDeleteSamplers(int count, IntBuffer samplers);

    @Override
    public native boolean glIsSampler(int sampler);

    @Override
    public native void glBindSampler(int unit, int sampler);

    @Override
    public native void glSamplerParameteri(int sampler, int pname, int param);

    @Override
    public native void glSamplerParameteriv(int sampler, int pname, IntBuffer param);

    @Override
    public native void glSamplerParameterf(int sampler, int pname, float param);

    @Override
    public native void glSamplerParameterfv(int sampler, int pname, FloatBuffer param);

    @Override
    public native void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params);

    @Override
    public native void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params);

    @Override
    public native void glVertexAttribDivisor(int index, int divisor);

    @Override
    public native void glBindTransformFeedback(int target, int id);

    @Override
    public native void glDeleteTransformFeedbacks(int n, IntBuffer ids);

    @Override
    public native void glGenTransformFeedbacks(int n, IntBuffer ids);

    @Override
    public native boolean glIsTransformFeedback(int id);

    @Override
    public native void glPauseTransformFeedback();

    @Override
    public native void glResumeTransformFeedback();

    @Override
    public native void glProgramParameteri(int program, int pname, int value);

    @Override
    public native void glInvalidateFramebuffer(int target, int numAttachments, IntBuffer attachments);

    @Override
    public native void glInvalidateSubFramebuffer(int target, int numAttachments, IntBuffer attachments, int x, int y, int width, int height);
}