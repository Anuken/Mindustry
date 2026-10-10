package arc.backend.robovm;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class IOSGLES20{
    /** last viewport set, needed because MGLKView resets the viewport on each call to render... amazing **/
    public static int x, y, width, height;

    public IOSGLES20(){
        init();
    }

    private static native void init();

    public native void glActiveTexture(int texture);

    public native void glAttachShader(int program, int shader);

    public native void glBindAttribLocation(int program, int index, String name);

    public native void glBindBuffer(int target, int buffer);

    public native void glBindFramebuffer(int target, int framebuffer);

    public native void glBindRenderbuffer(int target, int renderbuffer);

    public native void glBindTexture(int target, int texture);

    public native void glBlendColor(float red, float green, float blue, float alpha);

    public native void glBlendEquation(int mode);

    public native void glBlendEquationSeparate(int modeRGB, int modeAlpha);

    public native void glBlendFunc(int sfactor, int dfactor);

    public native void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

    public native void glBufferData(int target, int size, Buffer data, int usage);

    public native void glBufferSubData(int target, int offset, int size, Buffer data);

    public native int glCheckFramebufferStatus(int target);

    public native void glClear(int mask);

    public native void glClearColor(float red, float green, float blue, float alpha);

    public native void glClearDepthf(float depth);

    public native void glClearStencil(int s);

    public native void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    public native void glCompileShader(int shader);

    public native void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data);

    public native void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer data);

    public native void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border);

    public native void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);

    public native int glCreateProgram();

    public native int glCreateShader(int type);

    public native void glCullFace(int mode);

    public native void glDeleteBuffer(int buffer);

    public native void glDeleteFramebuffer(int framebuffer);

    public native void glDeleteProgram(int program);

    public native void glDeleteRenderbuffer(int renderbuffer);

    public native void glDeleteShader(int shader);

    public native void glDeleteTexture(int texture);

    public native void glDepthFunc(int func);

    public native void glDepthMask(boolean flag);

    public native void glDepthRangef(float zNear, float zFar);

    public native void glDetachShader(int program, int shader);

    public native void glDisable(int cap);

    public native void glDisableVertexAttribArray(int index);

    public native void glDrawArrays(int mode, int first, int count);

    public native void glDrawElements(int mode, int count, int type, Buffer indices);

    public native void glDrawElements(int mode, int count, int type, int indices);

    public native void glEnable(int cap);

    public native void glEnableVertexAttribArray(int index);

    public native void glFinish();

    public native void glFlush();

    public native void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);

    public native void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

    public native void glFrontFace(int mode);

    public native int glGenBuffer();

    public native void glGenerateMipmap(int target);

    public native int glGenFramebuffer();

    public native int glGenRenderbuffer();

    public native int glGenTexture();

    public native String glGetActiveAttrib(int program, int index, IntBuffer size, IntBuffer type);

    public native String glGetActiveUniform(int program, int index, IntBuffer size, IntBuffer type);

    public native int glGetAttribLocation(int program, String name);

    public native void glGetBooleanv(int pname, Buffer params);

    public native void glGetBufferParameteriv(int target, int pname, IntBuffer params);

    public native int glGetError();

    public native void glGetFloatv(int pname, FloatBuffer params);

    public native void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params);

    public native void glGetIntegerv(int pname, IntBuffer params);

    public native void glGetProgramiv(int program, int pname, IntBuffer params);

    public native String glGetProgramInfoLog(int program);

    public native void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params);

    public native void glGetShaderiv(int shader, int pname, IntBuffer params);

    public native String glGetShaderInfoLog(int shader);

    public native void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision);

    public native void glGetShaderSource(int shader, int bufsize, Buffer length, String source);

    public native String glGetString(int name);

    public native void glGetTexParameterfv(int target, int pname, FloatBuffer params);

    public native void glGetTexParameteriv(int target, int pname, IntBuffer params);

    public native void glGetUniformfv(int program, int location, FloatBuffer params);

    public native void glGetUniformiv(int program, int location, IntBuffer params);

    public native int glGetUniformLocation(int program, String name);

    public native void glGetVertexAttribfv(int index, int pname, FloatBuffer params);

    public native void glGetVertexAttribiv(int index, int pname, IntBuffer params);

    public native void glHint(int target, int mode);

    public native boolean glIsBuffer(int buffer);

    public native boolean glIsEnabled(int cap);

    public native boolean glIsFramebuffer(int framebuffer);

    public native boolean glIsProgram(int program);

    public native boolean glIsRenderbuffer(int renderbuffer);

    public native boolean glIsShader(int shader);

    public native boolean glIsTexture(int texture);

    public native void glLineWidth(float width);

    public native void glLinkProgram(int program);

    public native void glPixelStorei(int pname, int param);

    public native void glPolygonOffset(float factor, float units);

    public native void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels);

    public native void glReleaseShaderCompiler();

    public native void glRenderbufferStorage(int target, int internalformat, int width, int height);

    public native void glSampleCoverage(float value, boolean invert);

    public native void glScissor(int x, int y, int width, int height);

    public native void glShaderSource(int shader, String string);

    public native void glStencilFunc(int func, int ref, int mask);

    public native void glStencilFuncSeparate(int face, int func, int ref, int mask);

    public native void glStencilMask(int mask);

    public native void glStencilMaskSeparate(int face, int mask);

    public native void glStencilOp(int fail, int zfail, int zpass);

    public native void glStencilOpSeparate(int face, int fail, int zfail, int zpass);

    public native void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels);

    public native void glTexParameterf(int target, int pname, float param);

    public native void glTexParameterfv(int target, int pname, FloatBuffer params);

    public native void glTexParameteri(int target, int pname, int param);

    public native void glTexParameteriv(int target, int pname, IntBuffer params);

    public native void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels);

    public native void glUniform1f(int location, float x);

    public native void glUniform1fv(int location, int count, FloatBuffer v);

    public native void glUniform1fv(int location, int count, float[] v, int offset);

    public native void glUniform1i(int location, int x);

    public native void glUniform1iv(int location, int count, IntBuffer v);

    public native void glUniform1iv(int location, int count, int[] v, int offset);

    public native void glUniform2f(int location, float x, float y);

    public native void glUniform2fv(int location, int count, FloatBuffer v);

    public native void glUniform2fv(int location, int count, float[] v, int offset);

    public native void glUniform2i(int location, int x, int y);

    public native void glUniform2iv(int location, int count, IntBuffer v);

    public native void glUniform2iv(int location, int count, int[] v, int offset);

    public native void glUniform3f(int location, float x, float y, float z);

    public native void glUniform3fv(int location, int count, FloatBuffer v);

    public native void glUniform3fv(int location, int count, float[] v, int offset);

    public native void glUniform3i(int location, int x, int y, int z);

    public native void glUniform3iv(int location, int count, IntBuffer v);

    public native void glUniform3iv(int location, int count, int[] v, int offset);

    public native void glUniform4f(int location, float x, float y, float z, float w);

    public native void glUniform4fv(int location, int count, FloatBuffer v);

    public native void glUniform4fv(int location, int count, float[] v, int offset);

    public native void glUniform4i(int location, int x, int y, int z, int w);

    public native void glUniform4iv(int location, int count, IntBuffer v);

    public native void glUniform4iv(int location, int count, int[] v, int offset);

    public native void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value);

    public native void glUniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset);

    public native void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value);

    public native void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset);

    public native void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value);

    public native void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset);

    public native void glUseProgram(int program);

    public native void glValidateProgram(int program);

    public native void glVertexAttrib1f(int indx, float x);

    public native void glVertexAttrib1fv(int indx, FloatBuffer values);

    public native void glVertexAttrib2f(int indx, float x, float y);

    public native void glVertexAttrib2fv(int indx, FloatBuffer values);

    public native void glVertexAttrib3f(int indx, float x, float y, float z);

    public native void glVertexAttrib3fv(int indx, FloatBuffer values);

    public native void glVertexAttrib4f(int indx, float x, float y, float z, float w);

    public native void glVertexAttrib4fv(int indx, FloatBuffer values);

    public native void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr);

    public native void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr);

    public void glViewport(int x, int y, int width, int height){
        IOSGLES20.x = x;
        IOSGLES20.y = y;
        IOSGLES20.width = width;
        IOSGLES20.height = height;
        glViewportJni(x, y, width, height);
    }

    public native void glViewportJni(int x, int y, int width, int height);
}