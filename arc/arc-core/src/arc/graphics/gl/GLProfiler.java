package arc.graphics.gl;

import arc.*;
import arc.math.*;

import java.nio.*;

/**
 * @author Daniel Holderbaum
 * @author Jan Polák
 */
public class GLProfiler implements GLProvider{
    private static GLProvider glProvider;
    private static boolean enabled = false;
    private static GLErrorListener listener = GLErrorListener.loggingListener;

    public static final FloatCounter vertexCount = new FloatCounter(0);
    public static int calls;
    public static int textureBindings;
    public static int drawCalls;
    public static int shaderSwitches;
    public static int stateChanges;

    /** Enabled profiling with a logging listener. */
    public static void enable(){
        enable(GLErrorListener.loggingListener);
    }

    /** Enables profiling with the specified error listener. */
    public static void enable(GLErrorListener errorListener){
        if(enabled) return;

        listener = errorListener;
        glProvider = Core.glProvider;
        Core.glProvider = new GLProfiler();

        enabled = true;
    }

    /** Disables profiling. */
    public static void disable(){
        if(!enabled) return;

        Core.glProvider = glProvider;

        enabled = false;
        glProvider = null;
    }

    public static void setListener(GLErrorListener errorListener){
        listener = errorListener;
    }

    public static String resolveErrorNumber(int error){
        switch(error){
            case Gl.invalidValue: return "GL_INVALID_VALUE";
            case Gl.invalidOperation: return "GL_INVALID_OPERATION";
            case Gl.invalidFramebufferOperation: return "GL_INVALID_FRAMEBUFFER_OPERATION";
            case Gl.invalidEnum: return "GL_INVALID_ENUM";
            case Gl.outOfMemory: return "GL_OUT_OF_MEMORY";
            default: return "number " + error;
        }
    }

    /** Resets all statistics. Should generally be called at the end of a frame. */
    public static void reset(){
        calls = 0;
        textureBindings = 0;
        drawCalls = 0;
        shaderSwitches = 0;
        stateChanges = 0;
        vertexCount.reset();
    }

    private void check(){
        if(!Core.app.isOnMainThread()){
            listener.onError("GL call on wrong thread: " + Thread.currentThread());
        }
        int error = glProvider.glGetError();
        while(error != Gl.noError){
            listener.onError(resolveErrorNumber(error));
            error = glProvider.glGetError();
        }
    }

    @Override
    public void glActiveTexture(int texture){
        calls++;
        glProvider.glActiveTexture(texture);
        check();
    }

    @Override
    public void glBindTexture(int target, int texture){
        textureBindings++;
        calls++;
        glProvider.glBindTexture(target, texture);
        check();
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor){
        calls++;
        glProvider.glBlendFunc(sfactor, dfactor);
        check();
    }

    @Override
    public void glClear(int mask){
        calls++;
        glProvider.glClear(mask);
        check();
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha){
        calls++;
        glProvider.glClearColor(red, green, blue, alpha);
        check();
    }

    @Override
    public void glClearDepthf(float depth){
        calls++;
        glProvider.glClearDepthf(depth);
        check();
    }

    @Override
    public void glClearStencil(int s){
        calls++;
        glProvider.glClearStencil(s);
        check();
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha){
        calls++;
        glProvider.glColorMask(red, green, blue, alpha);
        check();
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                       int imageSize, Buffer data){
        calls++;
        glProvider.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
        check();
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
                                          int imageSize, Buffer data){
        calls++;
        glProvider.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data);
        check();
    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border){
        calls++;
        glProvider.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
        check();
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height){
        calls++;
        glProvider.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
        check();
    }

    @Override
    public void glCullFace(int mode){
        calls++;
        glProvider.glCullFace(mode);
        check();
    }

    @Override
    public void glDeleteTexture(int texture){
        calls++;
        glProvider.glDeleteTexture(texture);
        check();
    }

    @Override
    public void glDepthFunc(int func){
        calls++;
        glProvider.glDepthFunc(func);
        check();
    }

    @Override
    public void glDepthMask(boolean flag){
        calls++;
        glProvider.glDepthMask(flag);
        check();
    }

    @Override
    public void glDepthRangef(float zNear, float zFar){
        calls++;
        glProvider.glDepthRangef(zNear, zFar);
        check();
    }

    @Override
    public void glDisable(int cap){
        calls++;
        glProvider.glDisable(cap);
        check();
    }

    @Override
    public void glDrawArrays(int mode, int first, int count){
        vertexCount.put(count);
        drawCalls++;
        calls++;
        glProvider.glDrawArrays(mode, first, count);
        check();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices){
        vertexCount.put(count);
        drawCalls++;
        calls++;
        glProvider.glDrawElements(mode, count, type, indices);
        check();
    }

    @Override
    public void glEnable(int cap){
        calls++;
        glProvider.glEnable(cap);
        check();
    }

    @Override
    public void glFinish(){
        calls++;
        glProvider.glFinish();
        check();
    }

    @Override
    public void glFlush(){
        calls++;
        glProvider.glFlush();
        check();
    }

    @Override
    public void glFrontFace(int mode){
        calls++;
        glProvider.glFrontFace(mode);
        check();
    }

    @Override
    public int glGenTexture(){
        calls++;
        int result = glProvider.glGenTexture();
        check();
        return result;
    }

    @Override
    public int glGetError(){
        calls++;
        //Errors by glGetError are undetectable
        return glProvider.glGetError();
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer params){
        calls++;
        glProvider.glGetIntegerv(pname, params);
        check();
    }

    @Override
    public String glGetString(int name){
        calls++;
        String result = glProvider.glGetString(name);
        check();
        return result;
    }

    @Override
    public void glHint(int target, int mode){
        calls++;
        glProvider.glHint(target, mode);
        check();
    }

    @Override
    public void glLineWidth(float width){
        calls++;
        glProvider.glLineWidth(width);
        check();
    }

    @Override
    public void glPixelStorei(int pname, int param){
        calls++;
        glProvider.glPixelStorei(pname, param);
        check();
    }

    @Override
    public void glPolygonOffset(float factor, float units){
        calls++;
        glProvider.glPolygonOffset(factor, units);
        check();
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels){
        calls++;
        glProvider.glReadPixels(x, y, width, height, format, type, pixels);
        check();
    }

    @Override
    public void glScissor(int x, int y, int width, int height){
        calls++;
        glProvider.glScissor(x, y, width, height);
        check();
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask){
        calls++;
        glProvider.glStencilFunc(func, ref, mask);
        check();
    }

    @Override
    public void glStencilMask(int mask){
        calls++;
        glProvider.glStencilMask(mask);
        check();
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass){
        calls++;
        glProvider.glStencilOp(fail, zfail, zpass);
        check();
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type,
                             Buffer pixels){
        calls++;
        glProvider.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
        check();
    }

    @Override
    public void glTexParameterf(int target, int pname, float param){
        calls++;
        glProvider.glTexParameterf(target, pname, param);
        check();
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type,
                                Buffer pixels){
        calls++;
        glProvider.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
        check();
    }

    @Override
    public void glViewport(int x, int y, int width, int height){
        calls++;
        glProvider.glViewport(x, y, width, height);
        check();
    }

    @Override
    public void glAttachShader(int program, int shader){
        calls++;
        glProvider.glAttachShader(program, shader);
        check();
    }

    @Override
    public void glBindAttribLocation(int program, int index, String name){
        calls++;
        glProvider.glBindAttribLocation(program, index, name);
        check();
    }

    @Override
    public void glBindBuffer(int target, int buffer){
        calls++;
        glProvider.glBindBuffer(target, buffer);
        check();
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer){
        calls++;
        glProvider.glBindFramebuffer(target, framebuffer);
        check();
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer){
        calls++;
        glProvider.glBindRenderbuffer(target, renderbuffer);
        check();
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha){
        calls++;
        glProvider.glBlendColor(red, green, blue, alpha);
        check();
    }

    @Override
    public void glBlendEquation(int mode){
        calls++;
        glProvider.glBlendEquation(mode);
        check();
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha){
        calls++;
        glProvider.glBlendEquationSeparate(modeRGB, modeAlpha);
        check();
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha){
        calls++;
        glProvider.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        check();
    }

    @Override
    public void glBufferData(int target, int size, Buffer data, int usage){
        calls++;
        glProvider.glBufferData(target, size, data, usage);
        check();
    }

    @Override
    public void glBufferSubData(int target, int offset, int size, Buffer data){
        calls++;
        glProvider.glBufferSubData(target, offset, size, data);
        check();
    }

    @Override
    public int glCheckFramebufferStatus(int target){
        calls++;
        int result = glProvider.glCheckFramebufferStatus(target);
        check();
        return result;
    }

    @Override
    public void glCompileShader(int shader){
        calls++;
        glProvider.glCompileShader(shader);
        check();
    }

    @Override
    public int glCreateProgram(){
        calls++;
        int result = glProvider.glCreateProgram();
        check();
        return result;
    }

    @Override
    public int glCreateShader(int type){
        calls++;
        int result = glProvider.glCreateShader(type);
        check();
        return result;
    }

    @Override
    public void glDeleteBuffer(int buffer){
        calls++;
        glProvider.glDeleteBuffer(buffer);
        check();
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer){
        calls++;
        glProvider.glDeleteFramebuffer(framebuffer);
        check();
    }

    @Override
    public void glDeleteProgram(int program){
        calls++;
        glProvider.glDeleteProgram(program);
        check();
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer){
        calls++;
        glProvider.glDeleteRenderbuffer(renderbuffer);
        check();
    }

    @Override
    public void glDeleteShader(int shader){
        calls++;
        glProvider.glDeleteShader(shader);
        check();
    }

    @Override
    public void glDetachShader(int program, int shader){
        calls++;
        glProvider.glDetachShader(program, shader);
        check();
    }

    @Override
    public void glDisableVertexAttribArray(int index){
        calls++;
        glProvider.glDisableVertexAttribArray(index);
        check();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, int indices){
        vertexCount.put(count);
        drawCalls++;
        calls++;
        glProvider.glDrawElements(mode, count, type, indices);
        check();
    }

    @Override
    public void glEnableVertexAttribArray(int index){
        calls++;
        glProvider.glEnableVertexAttribArray(index);
        check();
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer){
        calls++;
        glProvider.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        check();
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level){
        calls++;
        glProvider.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        check();
    }

    @Override
    public int glGenBuffer(){
        calls++;
        int result = glProvider.glGenBuffer();
        check();
        return result;
    }

    @Override
    public void glGenerateMipmap(int target){
        calls++;
        glProvider.glGenerateMipmap(target);
        check();
    }

    @Override
    public int glGenFramebuffer(){
        calls++;
        int result = glProvider.glGenFramebuffer();
        check();
        return result;
    }

    @Override
    public int glGenRenderbuffer(){
        calls++;
        int result = glProvider.glGenRenderbuffer();
        check();
        return result;
    }

    @Override
    public String glGetActiveAttrib(int program, int index, IntBuffer size, IntBuffer type){
        calls++;
        String result = glProvider.glGetActiveAttrib(program, index, size, type);
        check();
        return result;
    }

    @Override
    public String glGetActiveUniform(int program, int index, IntBuffer size, IntBuffer type){
        calls++;
        String result = glProvider.glGetActiveUniform(program, index, size, type);
        check();
        return result;
    }

    @Override
    public int glGetAttribLocation(int program, String name){
        calls++;
        int result = glProvider.glGetAttribLocation(program, name);
        check();
        return result;
    }

    @Override
    public void glGetBooleanv(int pname, Buffer params){
        calls++;
        glProvider.glGetBooleanv(pname, params);
        check();
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params){
        calls++;
        glProvider.glGetBufferParameteriv(target, pname, params);
        check();
    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params){
        calls++;
        glProvider.glGetFloatv(pname, params);
        check();
    }

    @Override
    public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params){
        calls++;
        glProvider.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
        check();
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params){
        calls++;
        glProvider.glGetProgramiv(program, pname, params);
        check();
    }

    @Override
    public String glGetProgramInfoLog(int program){
        calls++;
        String result = glProvider.glGetProgramInfoLog(program);
        check();
        return result;
    }

    @Override
    public void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params){
        calls++;
        glProvider.glGetRenderbufferParameteriv(target, pname, params);
        check();
    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params){
        calls++;
        glProvider.glGetShaderiv(shader, pname, params);
        check();
    }

    @Override
    public String glGetShaderInfoLog(int shader){
        calls++;
        String result = glProvider.glGetShaderInfoLog(shader);
        check();
        return result;
    }

    @Override
    public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision){
        calls++;
        glProvider.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision);
        check();
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params){
        calls++;
        glProvider.glGetTexParameterfv(target, pname, params);
        check();
    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params){
        calls++;
        glProvider.glGetTexParameteriv(target, pname, params);
        check();
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params){
        calls++;
        glProvider.glGetUniformfv(program, location, params);
        check();
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params){
        calls++;
        glProvider.glGetUniformiv(program, location, params);
        check();
    }

    @Override
    public int glGetUniformLocation(int program, String name){
        calls++;
        int result = glProvider.glGetUniformLocation(program, name);
        check();
        return result;
    }

    @Override
    public void glGetVertexAttribfv(int index, int pname, FloatBuffer params){
        calls++;
        glProvider.glGetVertexAttribfv(index, pname, params);
        check();
    }

    @Override
    public void glGetVertexAttribiv(int index, int pname, IntBuffer params){
        calls++;
        glProvider.glGetVertexAttribiv(index, pname, params);
        check();
    }

    @Override
    public boolean glIsBuffer(int buffer){
        calls++;
        boolean result = glProvider.glIsBuffer(buffer);
        check();
        return result;
    }

    @Override
    public boolean glIsEnabled(int cap){
        calls++;
        boolean result = glProvider.glIsEnabled(cap);
        check();
        return result;
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer){
        calls++;
        boolean result = glProvider.glIsFramebuffer(framebuffer);
        check();
        return result;
    }

    @Override
    public boolean glIsProgram(int program){
        calls++;
        boolean result = glProvider.glIsProgram(program);
        check();
        return result;
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer){
        calls++;
        boolean result = glProvider.glIsRenderbuffer(renderbuffer);
        check();
        return result;
    }

    @Override
    public boolean glIsShader(int shader){
        calls++;
        boolean result = glProvider.glIsShader(shader);
        check();
        return result;
    }

    @Override
    public boolean glIsTexture(int texture){
        calls++;
        boolean result = glProvider.glIsTexture(texture);
        check();
        return result;
    }

    @Override
    public void glLinkProgram(int program){
        calls++;
        glProvider.glLinkProgram(program);
        check();
    }

    @Override
    public void glReleaseShaderCompiler(){
        calls++;
        glProvider.glReleaseShaderCompiler();
        check();
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height){
        calls++;
        glProvider.glRenderbufferStorage(target, internalformat, width, height);
        check();
    }

    @Override
    public void glSampleCoverage(float value, boolean invert){
        calls++;
        glProvider.glSampleCoverage(value, invert);
        check();
    }

    @Override
    public void glShaderSource(int shader, String string){
        calls++;
        glProvider.glShaderSource(shader, string);
        check();
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask){
        calls++;
        glProvider.glStencilFuncSeparate(face, func, ref, mask);
        check();
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask){
        calls++;
        glProvider.glStencilMaskSeparate(face, mask);
        check();
    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass){
        calls++;
        glProvider.glStencilOpSeparate(face, fail, zfail, zpass);
        check();
    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params){
        calls++;
        glProvider.glTexParameterfv(target, pname, params);
        check();
    }

    @Override
    public void glTexParameteri(int target, int pname, int param){
        calls++;
        glProvider.glTexParameteri(target, pname, param);
        check();
    }

    @Override
    public void glTexParameteriv(int target, int pname, IntBuffer params){
        calls++;
        glProvider.glTexParameteriv(target, pname, params);
        check();
    }

    @Override
    public void glUniform1f(int location, float x){
        calls++;
        glProvider.glUniform1f(location, x);
        check();
    }

    @Override
    public void glUniform1fv(int location, int count, FloatBuffer v){
        calls++;
        glProvider.glUniform1fv(location, count, v);
        check();
    }

    @Override
    public void glUniform1fv(int location, int count, float[] v, int offset){
        calls++;
        glProvider.glUniform1fv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniform1i(int location, int x){
        calls++;
        glProvider.glUniform1i(location, x);
        check();
    }

    @Override
    public void glUniform1iv(int location, int count, IntBuffer v){
        calls++;
        glProvider.glUniform1iv(location, count, v);
        check();
    }

    @Override
    public void glUniform1iv(int location, int count, int[] v, int offset){
        calls++;
        glProvider.glUniform1iv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniform2f(int location, float x, float y){
        calls++;
        glProvider.glUniform2f(location, x, y);
        check();
    }

    @Override
    public void glUniform2fv(int location, int count, FloatBuffer v){
        calls++;
        glProvider.glUniform2fv(location, count, v);
        check();
    }

    @Override
    public void glUniform2fv(int location, int count, float[] v, int offset){
        calls++;
        glProvider.glUniform2fv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniform2i(int location, int x, int y){
        calls++;
        glProvider.glUniform2i(location, x, y);
        check();
    }

    @Override
    public void glUniform2iv(int location, int count, IntBuffer v){
        calls++;
        glProvider.glUniform2iv(location, count, v);
        check();
    }

    @Override
    public void glUniform2iv(int location, int count, int[] v, int offset){
        calls++;
        glProvider.glUniform2iv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniform3f(int location, float x, float y, float z){
        calls++;
        glProvider.glUniform3f(location, x, y, z);
        check();
    }

    @Override
    public void glUniform3fv(int location, int count, FloatBuffer v){
        calls++;
        glProvider.glUniform3fv(location, count, v);
        check();
    }

    @Override
    public void glUniform3fv(int location, int count, float[] v, int offset){
        calls++;
        glProvider.glUniform3fv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniform3i(int location, int x, int y, int z){
        calls++;
        glProvider.glUniform3i(location, x, y, z);
        check();
    }

    @Override
    public void glUniform3iv(int location, int count, IntBuffer v){
        calls++;
        glProvider.glUniform3iv(location, count, v);
        check();
    }

    @Override
    public void glUniform3iv(int location, int count, int[] v, int offset){
        calls++;
        glProvider.glUniform3iv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w){
        calls++;
        glProvider.glUniform4f(location, x, y, z, w);
        check();
    }

    @Override
    public void glUniform4fv(int location, int count, FloatBuffer v){
        calls++;
        glProvider.glUniform4fv(location, count, v);
        check();
    }

    @Override
    public void glUniform4fv(int location, int count, float[] v, int offset){
        calls++;
        glProvider.glUniform4fv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w){
        calls++;
        glProvider.glUniform4i(location, x, y, z, w);
        check();
    }

    @Override
    public void glUniform4iv(int location, int count, IntBuffer v){
        calls++;
        glProvider.glUniform4iv(location, count, v);
        check();
    }

    @Override
    public void glUniform4iv(int location, int count, int[] v, int offset){
        calls++;
        glProvider.glUniform4iv(location, count, v, offset);
        check();
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix2fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset){
        calls++;
        glProvider.glUniformMatrix2fv(location, count, transpose, value, offset);
        check();
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix3fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset){
        calls++;
        glProvider.glUniformMatrix3fv(location, count, transpose, value, offset);
        check();
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix4fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset){
        calls++;
        glProvider.glUniformMatrix4fv(location, count, transpose, value, offset);
        check();
    }

    @Override
    public void glUseProgram(int program){
        shaderSwitches++;
        calls++;
        glProvider.glUseProgram(program);
        check();
    }

    @Override
    public void glValidateProgram(int program){
        calls++;
        glProvider.glValidateProgram(program);
        check();
    }

    @Override
    public void glVertexAttrib1f(int indx, float x){
        calls++;
        glProvider.glVertexAttrib1f(indx, x);
        check();
    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values){
        calls++;
        glProvider.glVertexAttrib1fv(indx, values);
        check();
    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y){
        calls++;
        glProvider.glVertexAttrib2f(indx, x, y);
        check();
    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values){
        calls++;
        glProvider.glVertexAttrib2fv(indx, values);
        check();
    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z){
        calls++;
        glProvider.glVertexAttrib3f(indx, x, y, z);
        check();
    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values){
        calls++;
        glProvider.glVertexAttrib3fv(indx, values);
        check();
    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w){
        calls++;
        glProvider.glVertexAttrib4f(indx, x, y, z, w);
        check();
    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values){
        calls++;
        glProvider.glVertexAttrib4fv(indx, values);
        check();
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr){
        calls++;
        glProvider.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
        check();
    }

    // GL30 Unique

    @Override
    public void glReadBuffer(int mode){
        calls++;
        glProvider.glReadBuffer(mode);
        check();
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices){
        vertexCount.put(count);
        drawCalls++;
        calls++;
        glProvider.glDrawRangeElements(mode, start, end, count, type, indices);
        check();
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset){
        vertexCount.put(count);
        drawCalls++;
        calls++;
        glProvider.glDrawRangeElements(mode, start, end, count, type, offset);
        check();
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format,
                             int type, Buffer pixels){
        calls++;
        glProvider.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
        check();
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format,
                             int type, int offset){
        calls++;
        glProvider.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, offset);
        check();
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth,
                                int format, int type, Buffer pixels){
        calls++;
        glProvider.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels);
        check();
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth,
                                int format, int type, int offset){
        calls++;
        glProvider.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, offset);
        check();
    }

    @Override
    public void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width,
                                    int height){
        calls++;
        glProvider.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
        check();
    }

    @Override
    public void glGenQueries(int n, IntBuffer ids){
        calls++;
        glProvider.glGenQueries(n, ids);
        check();
    }

    @Override
    public void glDeleteQueries(int n, IntBuffer ids){
        calls++;
        glProvider.glDeleteQueries(n, ids);
        check();
    }

    @Override
    public boolean glIsQuery(int id){
        calls++;
        final boolean result = glProvider.glIsQuery(id);
        check();
        return result;
    }

    @Override
    public void glBeginQuery(int target, int id){
        calls++;
        glProvider.glBeginQuery(target, id);
        check();
    }

    @Override
    public void glEndQuery(int target){
        calls++;
        glProvider.glEndQuery(target);
        check();
    }

    @Override
    public void glGetQueryiv(int target, int pname, IntBuffer params){
        calls++;
        glProvider.glGetQueryiv(target, pname, params);
        check();
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params){
        calls++;
        glProvider.glGetQueryObjectuiv(id, pname, params);
        check();
    }

    @Override
    public boolean glUnmapBuffer(int target){
        calls++;
        final boolean result = glProvider.glUnmapBuffer(target);
        check();
        return result;
    }

    @Override
    public Buffer glGetBufferPointerv(int target, int pname){
        calls++;
        final Buffer result = glProvider.glGetBufferPointerv(target, pname);
        check();
        return result;
    }

    @Override
    public void glDrawBuffers(int n, IntBuffer bufs){
        drawCalls++;
        calls++;
        glProvider.glDrawBuffers(n, bufs);
        check();
    }

    @Override
    public void glUniformMatrix2x3fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix2x3fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix3x2fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix3x2fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix2x4fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix2x4fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix4x2fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix4x2fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix3x4fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix3x4fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glUniformMatrix4x3fv(int location, int count, boolean transpose, FloatBuffer value){
        calls++;
        glProvider.glUniformMatrix4x3fv(location, count, transpose, value);
        check();
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1,
                                  int mask, int filter){
        calls++;
        glProvider.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        check();
    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height){
        calls++;
        glProvider.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
        check();
    }

    @Override
    public void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer){
        calls++;
        glProvider.glFramebufferTextureLayer(target, attachment, texture, level, layer);
        check();
    }

    @Override
    public void glFlushMappedBufferRange(int target, int offset, int length){
        calls++;
        glProvider.glFlushMappedBufferRange(target, offset, length);
        check();
    }

    @Override
    public void glBindVertexArray(int array){
        calls++;
        glProvider.glBindVertexArray(array);
        check();
    }

    @Override
    public void glDeleteVertexArrays(int n, IntBuffer arrays){
        calls++;
        glProvider.glDeleteVertexArrays(n, arrays);
        check();
    }

    @Override
    public void glGenVertexArrays(int n, IntBuffer arrays){
        calls++;
        glProvider.glGenVertexArrays(n, arrays);
        check();
    }

    @Override
    public boolean glIsVertexArray(int array){
        calls++;
        final boolean result = glProvider.glIsVertexArray(array);
        check();
        return result;
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode){
        calls++;
        glProvider.glBeginTransformFeedback(primitiveMode);
        check();
    }

    @Override
    public void glEndTransformFeedback(){
        calls++;
        glProvider.glEndTransformFeedback();
        check();
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, int offset, int size){
        calls++;
        glProvider.glBindBufferRange(target, index, buffer, offset, size);
        check();
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer){
        calls++;
        glProvider.glBindBufferBase(target, index, buffer);
        check();
    }

    @Override
    public void glTransformFeedbackVaryings(int program, String[] varyings, int bufferMode){
        calls++;
        glProvider.glTransformFeedbackVaryings(program, varyings, bufferMode);
        check();
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, int offset){
        calls++;
        glProvider.glVertexAttribIPointer(index, size, type, stride, offset);
        check();
    }

    @Override
    public void glGetVertexAttribIiv(int index, int pname, IntBuffer params){
        calls++;
        glProvider.glGetVertexAttribIiv(index, pname, params);
        check();
    }

    @Override
    public void glGetVertexAttribIuiv(int index, int pname, IntBuffer params){
        calls++;
        glProvider.glGetVertexAttribIuiv(index, pname, params);
        check();
    }

    @Override
    public void glVertexAttribI4i(int index, int x, int y, int z, int w){
        calls++;
        glProvider.glVertexAttribI4i(index, x, y, z, w);
        check();
    }

    @Override
    public void glVertexAttribI4ui(int index, int x, int y, int z, int w){
        calls++;
        glProvider.glVertexAttribI4ui(index, x, y, z, w);
        check();
    }

    @Override
    public void glGetUniformuiv(int program, int location, IntBuffer params){
        calls++;
        glProvider.glGetUniformuiv(program, location, params);
        check();
    }

    @Override
    public int glGetFragDataLocation(int program, String name){
        calls++;
        final int result = glProvider.glGetFragDataLocation(program, name);
        check();
        return result;
    }

    @Override
    public void glUniform1uiv(int location, int count, IntBuffer value){
        calls++;
        glProvider.glUniform1uiv(location, count, value);
        check();
    }

    @Override
    public void glUniform3uiv(int location, int count, IntBuffer value){
        calls++;
        glProvider.glUniform3uiv(location, count, value);
        check();
    }

    @Override
    public void glUniform4uiv(int location, int count, IntBuffer value){
        calls++;
        glProvider.glUniform4uiv(location, count, value);
        check();
    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value){
        calls++;
        glProvider.glClearBufferiv(buffer, drawbuffer, value);
        check();
    }

    @Override
    public void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value){
        calls++;
        glProvider.glClearBufferuiv(buffer, drawbuffer, value);
        check();
    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value){
        calls++;
        glProvider.glClearBufferfv(buffer, drawbuffer, value);
        check();
    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil){
        calls++;
        glProvider.glClearBufferfi(buffer, drawbuffer, depth, stencil);
        check();
    }

    @Override
    public String glGetStringi(int name, int index){
        calls++;
        final String result = glProvider.glGetStringi(name, index);
        check();
        return result;
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size){
        calls++;
        glProvider.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        check();
    }

    @Override
    public void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices){
        calls++;
        glProvider.glGetUniformIndices(program, uniformNames, uniformIndices);
        check();
    }

    @Override
    public void glGetActiveUniformsiv(int program, int uniformCount, IntBuffer uniformIndices, int pname, IntBuffer params){
        calls++;
        glProvider.glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params);
        check();
    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName){
        calls++;
        final int result = glProvider.glGetUniformBlockIndex(program, uniformBlockName);
        check();
        return result;
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params){
        calls++;
        glProvider.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
        check();
    }

    @Override
    public void glGetActiveUniformBlockName(int program, int uniformBlockIndex, Buffer length, Buffer uniformBlockName){
        calls++;
        glProvider.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName);
        check();
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding){
        calls++;
        glProvider.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        check();
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instanceCount){
        vertexCount.put(count);
        drawCalls++;
        calls++;
        glProvider.glDrawArraysInstanced(mode, first, count, instanceCount);
        check();
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount){
        vertexCount.put(count);
        drawCalls++;
        calls++;
        glProvider.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount);
        check();
    }

    @Override
    public void glGetInteger64v(int pname, LongBuffer params){
        calls++;
        glProvider.glGetInteger64v(pname, params);
        check();
    }

    @Override
    public void glGetBufferParameteri64v(int target, int pname, LongBuffer params){
        calls++;
        glProvider.glGetBufferParameteri64v(target, pname, params);
        check();
    }

    @Override
    public void glGenSamplers(int count, IntBuffer samplers){
        calls++;
        glProvider.glGenSamplers(count, samplers);
        check();
    }

    @Override
    public void glDeleteSamplers(int count, IntBuffer samplers){
        calls++;
        glProvider.glDeleteSamplers(count, samplers);
        check();
    }

    @Override
    public boolean glIsSampler(int sampler){
        calls++;
        final boolean result = glProvider.glIsSampler(sampler);
        check();
        return result;
    }

    @Override
    public void glBindSampler(int unit, int sampler){
        calls++;
        glProvider.glBindSampler(unit, sampler);
        check();
    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param){
        calls++;
        glProvider.glSamplerParameteri(sampler, pname, param);
        check();
    }

    @Override
    public void glSamplerParameteriv(int sampler, int pname, IntBuffer param){
        calls++;
        glProvider.glSamplerParameteriv(sampler, pname, param);
        check();
    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param){
        calls++;
        glProvider.glSamplerParameterf(sampler, pname, param);
        check();
    }

    @Override
    public void glSamplerParameterfv(int sampler, int pname, FloatBuffer param){
        calls++;
        glProvider.glSamplerParameterfv(sampler, pname, param);
        check();
    }

    @Override
    public void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params){
        calls++;
        glProvider.glGetSamplerParameteriv(sampler, pname, params);
        check();
    }

    @Override
    public void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params){
        calls++;
        glProvider.glGetSamplerParameterfv(sampler, pname, params);
        check();
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor){
        calls++;
        glProvider.glVertexAttribDivisor(index, divisor);
        check();
    }

    @Override
    public void glBindTransformFeedback(int target, int id){
        calls++;
        glProvider.glBindTransformFeedback(target, id);
        check();
    }

    @Override
    public void glDeleteTransformFeedbacks(int n, IntBuffer ids){
        calls++;
        glProvider.glDeleteTransformFeedbacks(n, ids);
        check();
    }

    @Override
    public void glGenTransformFeedbacks(int n, IntBuffer ids){
        calls++;
        glProvider.glGenTransformFeedbacks(n, ids);
        check();
    }

    @Override
    public boolean glIsTransformFeedback(int id){
        calls++;
        final boolean result = glProvider.glIsTransformFeedback(id);
        check();
        return result;
    }

    @Override
    public void glPauseTransformFeedback(){
        calls++;
        glProvider.glPauseTransformFeedback();
        check();
    }

    @Override
    public void glResumeTransformFeedback(){
        calls++;
        glProvider.glResumeTransformFeedback();
        check();
    }

    @Override
    public void glProgramParameteri(int program, int pname, int value){
        calls++;
        glProvider.glProgramParameteri(program, pname, value);
        check();
    }

    @Override
    public void glInvalidateFramebuffer(int target, int numAttachments, IntBuffer attachments){
        calls++;
        glProvider.glInvalidateFramebuffer(target, numAttachments, attachments);
        check();
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, int numAttachments, IntBuffer attachments, int x, int y, int width,
                                           int height){
        calls++;
        glProvider.glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height);
        check();
    }
}
