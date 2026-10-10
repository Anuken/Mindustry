package arc.backend.sdl;

import arc.graphics.gl.*;
import arc.util.*;
import org.lwjgl.*;
import org.lwjgl.opengles.*;
import org.lwjgl.system.*;

import java.nio.*;

class SdlGLESProvider implements GLProvider{
    private ByteBuffer buffer = null;
    private FloatBuffer floatBuffer = null;
    private IntBuffer intBuffer = null;

    private void ensureBufferCapacity(int numBytes){
        if(buffer == null || buffer.capacity() < numBytes){
            buffer = Buffers.newByteBuffer(numBytes);
            floatBuffer = buffer.asFloatBuffer();
            intBuffer = buffer.asIntBuffer();
        }
    }

    private FloatBuffer toFloatBuffer(float[] v, int offset, int count){
        ensureBufferCapacity(count << 2);
        floatBuffer.clear();
        floatBuffer.limit(count);
        floatBuffer.put(v, offset, count);
        floatBuffer.position(0);
        return floatBuffer;
    }

    private IntBuffer toIntBuffer(int[] v, int offset, int count){
        ensureBufferCapacity(count << 2);
        intBuffer.clear();
        intBuffer.limit(count);
        intBuffer.put(v, offset, count);
        intBuffer.position(0);
        return intBuffer;
    }

    @Override
    public void glActiveTexture(int texture){
        GLES20.glActiveTexture(texture);
    }

    @Override
    public void glAttachShader(int program, int shader){
        GLES20.glAttachShader(program, shader);
    }

    @Override
    public void glBindAttribLocation(int program, int index, String name){
        GLES20.glBindAttribLocation(program, index, name);
    }

    @Override
    public void glBindBuffer(int target, int buffer){
        GLES20.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindTexture(int target, int texture){
        GLES20.glBindTexture(target, texture);
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha){
        GLES20.glBlendColor(red, green, blue, alpha);
    }

    @Override
    public void glBlendEquation(int mode){
        GLES20.glBlendEquation(mode);
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha){
        GLES20.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor){
        GLES20.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha){
        GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void glBufferData(int target, int size, Buffer data, int usage){
        if(data == null)
            GLES20.glBufferData(target, size, usage);
        else if(data instanceof ByteBuffer)
            GLES20.glBufferData(target, (ByteBuffer)data, usage);
        else if(data instanceof IntBuffer)
            GLES20.glBufferData(target, (IntBuffer)data, usage);
        else if(data instanceof FloatBuffer)
            GLES20.glBufferData(target, (FloatBuffer)data, usage);
        else if(data instanceof DoubleBuffer)
            throw new ArcRuntimeException("DoubleBuffer not supported by GLES");
        else if(data instanceof ShortBuffer) //
            GLES20.glBufferData(target, (ShortBuffer)data, usage);
    }

    @Override
    public void glBufferSubData(int target, int offset, int size, Buffer data){
        if(data == null)
            throw new ArcRuntimeException("Using null for the data not possible, blame LWJGL");
        else if(data instanceof ByteBuffer)
            GLES20.glBufferSubData(target, offset, (ByteBuffer)data);
        else if(data instanceof IntBuffer)
            GLES20.glBufferSubData(target, offset, (IntBuffer)data);
        else if(data instanceof FloatBuffer)
            GLES20.glBufferSubData(target, offset, (FloatBuffer)data);
        else if(data instanceof DoubleBuffer)
            throw new ArcRuntimeException("DoubleBuffer not supported by GLES");
        else if(data instanceof ShortBuffer) //
            GLES20.glBufferSubData(target, offset, (ShortBuffer)data);
    }

    @Override
    public void glClear(int mask){
        GLES20.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha){
        GLES20.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glClearDepthf(float depth){
        GLES20.glClearDepthf(depth);
    }

    @Override
    public void glClearStencil(int s){
        GLES20.glClearStencil(s);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha){
        GLES20.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCompileShader(int shader){
        GLES20.glCompileShader(shader);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                       int imageSize, Buffer data){
        if(data instanceof ByteBuffer){
            GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, border, (ByteBuffer)data);
        }else{
            throw new ArcRuntimeException("Can't use " + data.getClass().getName() + " with this method. Use ByteBuffer instead.");
        }
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
                                          int imageSize, Buffer data){
        throw new ArcRuntimeException("not implemented");
    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border){
        GLES20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height){
        GLES20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public int glCreateProgram(){
        return GLES20.glCreateProgram();
    }

    @Override
    public int glCreateShader(int type){
        return GLES20.glCreateShader(type);
    }

    @Override
    public void glCullFace(int mode){
        GLES20.glCullFace(mode);
    }

    @Override
    public void glDeleteBuffer(int buffer){
        GLES20.glDeleteBuffers(buffer);
    }

    @Override
    public void glDeleteShader(int shader){
        GLES20.glDeleteShader(shader);
    }

    @Override
    public void glDeleteTexture(int texture){
        GLES20.glDeleteTextures(texture);
    }

    @Override
    public void glDepthFunc(int func){
        GLES20.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag){
        GLES20.glDepthMask(flag);
    }

    @Override
    public void glDepthRangef(float zNear, float zFar){
        GLES20.glDepthRangef(zNear, zFar);
    }

    @Override
    public void glDetachShader(int program, int shader){
        GLES20.glDetachShader(program, shader);
    }

    @Override
    public void glDisable(int cap){
        GLES20.glDisable(cap);
    }

    @Override
    public void glDisableVertexAttribArray(int index){
        GLES20.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count){
        GLES20.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices){
        if(indices instanceof ShortBuffer && type == Gl.unsignedShort){
            GLES20.glDrawElements(mode, (ShortBuffer)indices);
        }else if(indices instanceof ByteBuffer && type == Gl.unsignedShort){
            ShortBuffer sb = ((ByteBuffer)indices).asShortBuffer();
            GLES20.glDrawElements(mode, sb);
        }else if(indices instanceof ByteBuffer && type == Gl.unsignedByte){
            GLES20.glDrawElements(mode, (ByteBuffer)indices);
        }else
            throw new ArcRuntimeException("Can't use " + indices.getClass().getName()
            + " with this method. Use ShortBuffer or ByteBuffer instead. Blame LWJGL");
    }

    @Override
    public void glEnable(int cap){
        GLES20.glEnable(cap);
    }

    @Override
    public void glEnableVertexAttribArray(int index){
        GLES20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glFinish(){
        GLES20.glFinish();
    }

    @Override
    public void glFlush(){
        GLES20.glFlush();
    }

    @Override
    public void glFrontFace(int mode){
        GLES20.glFrontFace(mode);
    }

    @Override
    public int glGenBuffer(){
        return GLES20.glGenBuffers();
    }

    @Override
    public int glGenTexture(){
        return GLES20.glGenTextures();
    }

    @Override
    public String glGetActiveAttrib(int program, int index, IntBuffer size, IntBuffer type){
        return GLES20.glGetActiveAttrib(program, index, 256, size, type);
    }

    @Override
    public String glGetActiveUniform(int program, int index, IntBuffer size, IntBuffer type){
        return GLES20.glGetActiveUniform(program, index, 256, size, type);
    }

    @Override
    public int glGetAttribLocation(int program, String name){
        return GLES20.glGetAttribLocation(program, name);
    }

    @Override
    public void glGetBooleanv(int pname, Buffer params){
        GLES20.glGetBooleanv(pname, (ByteBuffer)params);
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params){
        GLES20.glGetBufferParameteriv(target, pname, params);
    }

    @Override
    public int glGetError(){
        return GLES20.glGetError();
    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params){
        GLES20.glGetFloatv(pname, params);
    }

    @Override
    public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params){
        GLES20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer params){
        GLES20.glGetIntegerv(pname, params);
    }

    @Override
    public String glGetProgramInfoLog(int program){
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        GLES20.glGetProgramInfoLog(program, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params){
        GLES20.glGetProgramiv(program, pname, params);
    }

    @Override
    public String glGetShaderInfoLog(int shader){
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        GLES20.glGetShaderInfoLog(shader, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }

    @Override
    public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision){
        GLES20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision);
    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params){
        GLES20.glGetShaderiv(shader, pname, params);
    }

    @Override
    public String glGetString(int name){
        return GLES20.glGetString(name);
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params){
        GLES20.glGetTexParameterfv(target, pname, params);
    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params){
        GLES20.glGetTexParameteriv(target, pname, params);
    }

    @Override
    public int glGetUniformLocation(int program, String name){
        return GLES20.glGetUniformLocation(program, name);
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params){
        GLES20.glGetUniformfv(program, location, params);
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params){
        GLES20.glGetUniformiv(program, location, params);
    }

    @Override
    public void glGetVertexAttribfv(int index, int pname, FloatBuffer params){
        GLES20.glGetVertexAttribfv(index, pname, params);
    }

    @Override
    public void glGetVertexAttribiv(int index, int pname, IntBuffer params){
        GLES20.glGetVertexAttribiv(index, pname, params);
    }

    @Override
    public void glHint(int target, int mode){
        GLES20.glHint(target, mode);
    }

    @Override
    public boolean glIsBuffer(int buffer){
        return GLES20.glIsBuffer(buffer);
    }

    @Override
    public boolean glIsEnabled(int cap){
        return GLES20.glIsEnabled(cap);
    }

    @Override
    public boolean glIsProgram(int program){
        return GLES20.glIsProgram(program);
    }

    @Override
    public boolean glIsShader(int shader){
        return GLES20.glIsShader(shader);
    }

    @Override
    public boolean glIsTexture(int texture){
        return GLES20.glIsTexture(texture);
    }

    @Override
    public void glLineWidth(float width){
        GLES20.glLineWidth(width);
    }

    @Override
    public void glLinkProgram(int program){
        GLES20.glLinkProgram(program);
    }

    @Override
    public void glPixelStorei(int pname, int param){
        GLES20.glPixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units){
        GLES20.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels){
        if(pixels instanceof ByteBuffer)
            GLES20.glReadPixels(x, y, width, height, format, type, (ByteBuffer)pixels);
        else if(pixels instanceof ShortBuffer)
            GLES20.glReadPixels(x, y, width, height, format, type, (ShortBuffer)pixels);
        else if(pixels instanceof IntBuffer)
            GLES20.glReadPixels(x, y, width, height, format, type, (IntBuffer)pixels);
        else if(pixels instanceof FloatBuffer)
            GLES20.glReadPixels(x, y, width, height, format, type, (FloatBuffer)pixels);
        else
            throw new ArcRuntimeException("Can't use " + pixels.getClass().getName()
            + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer or FloatBuffer instead. Blame LWJGL");
    }

    @Override
    public void glReleaseShaderCompiler(){
        GLES20.glReleaseShaderCompiler();
    }

    @Override
    public void glSampleCoverage(float value, boolean invert){
        GLES20.glSampleCoverage(value, invert);
    }

    @Override
    public void glScissor(int x, int y, int width, int height){
        GLES20.glScissor(x, y, width, height);
    }

    @Override
    public void glShaderSource(int shader, String string){
        GLES20.glShaderSource(shader, string);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask){
        GLES20.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask){
        GLES20.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask){
        GLES20.glStencilMask(mask);
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask){
        GLES20.glStencilMaskSeparate(face, mask);
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass){
        GLES20.glStencilOp(fail, zfail, zpass);
    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass){
        GLES20.glStencilOpSeparate(face, fail, zfail, zpass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type,
                             Buffer pixels){

        if(pixels == null)
            GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, (ByteBuffer)null);
        else if(pixels instanceof ByteBuffer)
            GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, (ByteBuffer)pixels);
        else if(pixels instanceof ShortBuffer)
            GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, (ShortBuffer)pixels);
        else if(pixels instanceof IntBuffer)
            GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, (IntBuffer)pixels);
        else if(pixels instanceof FloatBuffer)
            GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, (FloatBuffer)pixels);
        else if(pixels instanceof DoubleBuffer)
            throw new ArcRuntimeException("DoubleBuffer not supported by GLES");
        else
            throw new ArcRuntimeException("Can't use " + pixels.getClass().getName()
            + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer or FloatBuffer instead. Blame LWJGL");
    }

    @Override
    public void glTexParameterf(int target, int pname, float param){
        GLES20.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params){
        GLES20.glTexParameterfv(target, pname, params);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param){
        GLES20.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexParameteriv(int target, int pname, IntBuffer params){
        GLES20.glTexParameteriv(target, pname, params);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type,
                                Buffer pixels){
        if(pixels instanceof ByteBuffer)
            GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (ByteBuffer)pixels);
        else if(pixels instanceof ShortBuffer)
            GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (ShortBuffer)pixels);
        else if(pixels instanceof IntBuffer)
            GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (IntBuffer)pixels);
        else if(pixels instanceof FloatBuffer)
            GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (FloatBuffer)pixels);
        else if(pixels instanceof DoubleBuffer)
            throw new ArcRuntimeException("DoubleBuffer not supported by GLES");
        else
            throw new ArcRuntimeException("Can't use " + pixels.getClass().getName()
            + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer or FloatBuffer instead. Blame LWJGL");
    }

    @Override
    public void glUniform1f(int location, float x){
        GLES20.glUniform1f(location, x);
    }

    @Override
    public void glUniform1fv(int location, int count, FloatBuffer v){
        GLES20.glUniform1fv(location, v);
    }

    @Override
    public void glUniform1fv(int location, int count, float[] v, int offset){
        GLES20.glUniform1fv(location, toFloatBuffer(v, offset, count));
    }

    @Override
    public void glUniform1i(int location, int x){
        GLES20.glUniform1i(location, x);
    }

    @Override
    public void glUniform1iv(int location, int count, IntBuffer v){
        GLES20.glUniform1iv(location, v);
    }

    @Override
    public void glUniform1iv(int location, int count, int[] v, int offset){
        GLES20.glUniform1iv(location, toIntBuffer(v, offset, count));
    }

    @Override
    public void glUniform2f(int location, float x, float y){
        GLES20.glUniform2f(location, x, y);
    }

    @Override
    public void glUniform2fv(int location, int count, FloatBuffer v){
        GLES20.glUniform2fv(location, v);
    }

    @Override
    public void glUniform2fv(int location, int count, float[] v, int offset){
        GLES20.glUniform2fv(location, toFloatBuffer(v, offset, count << 1));
    }

    @Override
    public void glUniform2i(int location, int x, int y){
        GLES20.glUniform2i(location, x, y);
    }

    @Override
    public void glUniform2iv(int location, int count, IntBuffer v){
        GLES20.glUniform2iv(location, v);
    }

    @Override
    public void glUniform2iv(int location, int count, int[] v, int offset){
        GLES20.glUniform2iv(location, toIntBuffer(v, offset, count << 1));
    }

    @Override
    public void glUniform3f(int location, float x, float y, float z){
        GLES20.glUniform3f(location, x, y, z);
    }

    @Override
    public void glUniform3fv(int location, int count, FloatBuffer v){
        GLES20.glUniform3fv(location, v);
    }

    @Override
    public void glUniform3fv(int location, int count, float[] v, int offset){
        GLES20.glUniform3fv(location, toFloatBuffer(v, offset, count * 3));
    }

    @Override
    public void glUniform3i(int location, int x, int y, int z){
        GLES20.glUniform3i(location, x, y, z);
    }

    @Override
    public void glUniform3iv(int location, int count, IntBuffer v){
        GLES20.glUniform3iv(location, v);
    }

    @Override
    public void glUniform3iv(int location, int count, int[] v, int offset){
        GLES20.glUniform3iv(location, toIntBuffer(v, offset, count * 3));
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w){
        GLES20.glUniform4f(location, x, y, z, w);
    }

    @Override
    public void glUniform4fv(int location, int count, FloatBuffer v){
        GLES20.glUniform4fv(location, v);
    }

    @Override
    public void glUniform4fv(int location, int count, float[] v, int offset){
        GLES20.glUniform4fv(location, toFloatBuffer(v, offset, count << 2));
    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w){
        GLES20.glUniform4i(location, x, y, z, w);
    }

    @Override
    public void glUniform4iv(int location, int count, IntBuffer v){
        GLES20.glUniform4iv(location, v);
    }

    @Override
    public void glUniform4iv(int location, int count, int[] v, int offset){
        GLES20.glUniform4iv(location, toIntBuffer(v, offset, count << 2));
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES20.glUniformMatrix2fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset){
        GLES20.glUniformMatrix2fv(location, transpose, toFloatBuffer(value, offset, count << 2));
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES20.glUniformMatrix3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset){
        GLES20.glUniformMatrix3fv(location, transpose, toFloatBuffer(value, offset, count * 9));
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES20.glUniformMatrix4fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset){
        GLES20.glUniformMatrix4fv(location, transpose, toFloatBuffer(value, offset, count << 4));
    }

    @Override
    public void glUseProgram(int program){
        GLES20.glUseProgram(program);
    }

    @Override
    public void glValidateProgram(int program){
        GLES20.glValidateProgram(program);
    }

    @Override
    public void glVertexAttrib1f(int indx, float x){
        GLES20.glVertexAttrib1f(indx, x);
    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values){
        GLES20.glVertexAttrib1f(indx, values.get());
    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y){
        GLES20.glVertexAttrib2f(indx, x, y);
    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values){
        GLES20.glVertexAttrib2f(indx, values.get(), values.get());
    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z){
        GLES20.glVertexAttrib3f(indx, x, y, z);
    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values){
        GLES20.glVertexAttrib3f(indx, values.get(), values.get(), values.get());
    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w){
        GLES20.glVertexAttrib4f(indx, x, y, z, w);
    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values){
        GLES20.glVertexAttrib4f(indx, values.get(), values.get(), values.get(), values.get());
    }

    @Override
    public void glViewport(int x, int y, int width, int height){
        GLES20.glViewport(x, y, width, height);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, int indices){
        GLES20.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr){
        GLES20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }


    @Override
    public void glReadBuffer(int mode){
        GLES30.glReadBuffer(mode);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices){
        if(indices instanceof ByteBuffer)
            GLES30.glDrawRangeElements(mode, start, end, (ByteBuffer)indices);
        else if(indices instanceof ShortBuffer)
            GLES30.glDrawRangeElements(mode, start, end, (ShortBuffer)indices);
        else if(indices instanceof IntBuffer)
            GLES30.glDrawRangeElements(mode, start, end, (IntBuffer)indices);
        else
            throw new ArcRuntimeException("indices must be byte, short or int buffer");
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset){
        GLES30.glDrawRangeElements(mode, start, end, count, type, offset);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format,
                             int type, Buffer pixels){
        if(pixels == null)
            GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, (ByteBuffer)null);
        else if(pixels instanceof ByteBuffer)
            GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, (ByteBuffer)pixels);
        else if(pixels instanceof ShortBuffer)
            GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, (ShortBuffer)pixels);
        else if(pixels instanceof IntBuffer)
            GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, (IntBuffer)pixels);
        else if(pixels instanceof FloatBuffer)
            GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, (FloatBuffer)pixels);
        else if(pixels instanceof DoubleBuffer)
            throw new ArcRuntimeException("DoubleBuffer not supported by GLES");
        else
            throw new ArcRuntimeException("Can't use " + pixels.getClass().getName()
            + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer or FloatBuffer instead. Blame LWJGL");
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format,
                             int type, int offset){
        GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, offset);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth,
                                int format, int type, Buffer pixels){
        if(pixels instanceof ByteBuffer)
            GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, (ByteBuffer)pixels);
        else if(pixels instanceof ShortBuffer)
            GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, (ShortBuffer)pixels);
        else if(pixels instanceof IntBuffer)
            GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, (IntBuffer)pixels);
        else if(pixels instanceof FloatBuffer)
            GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, (FloatBuffer)pixels);
        else if(pixels instanceof DoubleBuffer)
            throw new ArcRuntimeException("DoubleBuffer not supported by GLES");
        else
            throw new ArcRuntimeException("Can't use " + pixels.getClass().getName()
            + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer or FloatBuffer instead. Blame LWJGL");
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth,
                                int format, int type, int offset){
        GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, offset);
    }

    @Override
    public void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width,
                                    int height){
        GLES30.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
    }

    @Override
    public void glGenQueries(int n, IntBuffer ids){
        GLES30.glGenQueries(ids);
    }

    @Override
    public void glDeleteQueries(int n, IntBuffer ids){
        GLES30.glDeleteQueries(ids);
    }

    @Override
    public void glDeleteProgram(int program){
        GLES20.glDeleteProgram(program);
    }

    @Override
    public boolean glIsQuery(int id){
        return GLES30.glIsQuery(id);
    }

    @Override
    public void glBeginQuery(int target, int id){
        GLES30.glBeginQuery(target, id);
    }

    @Override
    public void glEndQuery(int target){
        GLES30.glEndQuery(target);
    }

    @Override
    public void glGetQueryiv(int target, int pname, IntBuffer params){
        GLES30.glGetQueryiv(target, pname, params);
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params){
        GLES30.glGetQueryObjectuiv(id, pname, params);
    }

    @Override
    public boolean glUnmapBuffer(int target){
        return GLES30.glUnmapBuffer(target);
    }

    @Override
    public Buffer glGetBufferPointerv(int target, int pname){
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void glDrawBuffers(int n, IntBuffer bufs){
        int limit = bufs.limit();
        ((Buffer)bufs).limit(n);
        GLES30.glDrawBuffers(bufs);
        ((Buffer)bufs).limit(limit);
    }

    @Override
    public void glUniformMatrix2x3fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES30.glUniformMatrix2x3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix3x2fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES30.glUniformMatrix3x2fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix2x4fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES30.glUniformMatrix2x4fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4x2fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES30.glUniformMatrix4x2fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix3x4fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES30.glUniformMatrix3x4fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4x3fv(int location, int count, boolean transpose, FloatBuffer value){
        GLES30.glUniformMatrix4x3fv(location, transpose, value);
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1,
                                  int mask, int filter){
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer){
        GLES30.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer){
        GLES30.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public int glCheckFramebufferStatus(int target){
        return GLES30.glCheckFramebufferStatus(target);
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer){
        GLES30.glDeleteFramebuffers(framebuffer);
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer){
        GLES30.glDeleteRenderbuffers(renderbuffer);
    }

    @Override
    public void glGenerateMipmap(int target){
        GLES30.glGenerateMipmap(target);
    }

    @Override
    public int glGenFramebuffer(){
        return GLES30.glGenFramebuffers();
    }

    @Override
    public int glGenRenderbuffer(){
        return GLES30.glGenRenderbuffers();
    }

    @Override
    public void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params){
        GLES30.glGetRenderbufferParameteriv(target, pname, params);
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer){
        return GLES30.glIsFramebuffer(framebuffer);
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer){
        return GLES30.glIsRenderbuffer(renderbuffer);
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height){
        GLES30.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height){
        GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level){
        GLES30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer){
        GLES30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer){
        GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }
    @Override
    public void glFlushMappedBufferRange(int target, int offset, int length){
        GLES30.glFlushMappedBufferRange(target, offset, length);
    }

    @Override
    public void glBindVertexArray(int array){
        GLES30.glBindVertexArray(array);
    }
    @Override
    public void glDeleteVertexArrays(int n, IntBuffer arrays){
        GLES30.glDeleteVertexArrays(arrays);
    }

    @Override
    public void glGenVertexArrays(int n, IntBuffer arrays){
        GLES30.glGenVertexArrays(arrays);
    }

    @Override
    public boolean glIsVertexArray(int array){
        return GLES30.glIsVertexArray(array);
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode){
        GLES30.glBeginTransformFeedback(primitiveMode);
    }

    @Override
    public void glEndTransformFeedback(){
        GLES30.glEndTransformFeedback();
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, int offset, int size){
        GLES30.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer){
        GLES30.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glTransformFeedbackVaryings(int program, String[] varyings, int bufferMode){
        GLES30.glTransformFeedbackVaryings(program, varyings, bufferMode);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, int offset){
        GLES30.glVertexAttribIPointer(index, size, type, stride, offset);
    }

    @Override
    public void glGetVertexAttribIiv(int index, int pname, IntBuffer params){
        GLES30.glGetVertexAttribIiv(index, pname, params);
    }

    @Override
    public void glGetVertexAttribIuiv(int index, int pname, IntBuffer params){
        GLES30.glGetVertexAttribIuiv(index, pname, params);
    }

    @Override
    public void glVertexAttribI4i(int index, int x, int y, int z, int w){
        GLES30.glVertexAttribI4i(index, x, y, z, w);
    }

    @Override
    public void glVertexAttribI4ui(int index, int x, int y, int z, int w){
        GLES30.glVertexAttribI4ui(index, x, y, z, w);
    }

    @Override
    public void glGetUniformuiv(int program, int location, IntBuffer params){
        GLES30.glGetUniformuiv(program, location, params);
    }

    @Override
    public int glGetFragDataLocation(int program, String name){
        return GLES30.glGetFragDataLocation(program, name);
    }

    @Override
    public void glUniform1uiv(int location, int count, IntBuffer value){
        GLES30.glUniform1uiv(location, value);
    }

    @Override
    public void glUniform3uiv(int location, int count, IntBuffer value){
        GLES30.glUniform3uiv(location, value);
    }

    @Override
    public void glUniform4uiv(int location, int count, IntBuffer value){
        GLES30.glUniform4uiv(location, value);
    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value){
        GLES30.glClearBufferiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value){
        GLES30.glClearBufferuiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value){
        GLES30.glClearBufferfv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil){
        GLES30.glClearBufferfi(buffer, drawbuffer, depth, stencil);
    }

    @Override
    public String glGetStringi(int name, int index){
        return GLES30.glGetStringi(name, index);
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size){
        GLES30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices){
        try(MemoryStack stack = MemoryStack.stackPush()){
            PointerBuffer names = stack.mallocPointer(uniformNames.length);
            for(String name : uniformNames){
                names.put(stack.UTF8(name));
            }
            names.flip();
            GLES30.glGetUniformIndices(program, names, uniformIndices);
        }
    }

    @Override
    public void glGetActiveUniformsiv(int program, int uniformCount, IntBuffer uniformIndices, int pname, IntBuffer params){
        GLES30.glGetActiveUniformsiv(program, uniformIndices, pname, params);
    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName){
        return GLES30.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params){
        GLES30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
    }

    @Override
    public void glGetActiveUniformBlockName(int program, int uniformBlockIndex, Buffer length, Buffer uniformBlockName){
        GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex, (IntBuffer)length, (ByteBuffer)uniformBlockName);
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding){
        GLES30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instanceCount){
        GLES30.glDrawArraysInstanced(mode, first, count, instanceCount);
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount){
        GLES30.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount);
    }

    @Override
    public void glGetInteger64v(int pname, LongBuffer params){
        GLES30.glGetInteger64v(pname, params);
    }

    @Override
    public void glGetBufferParameteri64v(int target, int pname, LongBuffer params){
        params.put(GLES30.glGetBufferParameteri64(target, pname));
    }

    @Override
    public void glGenSamplers(int count, IntBuffer samplers){
        GLES30.glGenSamplers(samplers);
    }

    @Override
    public void glDeleteSamplers(int count, IntBuffer samplers){
        GLES30.glDeleteSamplers(samplers);
    }

    @Override
    public boolean glIsSampler(int sampler){
        return GLES30.glIsSampler(sampler);
    }

    @Override
    public void glBindSampler(int unit, int sampler){
        GLES30.glBindSampler(unit, sampler);
    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param){
        GLES30.glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void glSamplerParameteriv(int sampler, int pname, IntBuffer param){
        GLES30.glSamplerParameteriv(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param){
        GLES30.glSamplerParameterf(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterfv(int sampler, int pname, FloatBuffer param){
        GLES30.glSamplerParameterfv(sampler, pname, param);
    }

    @Override
    public void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params){
        GLES30.glGetSamplerParameteriv(sampler, pname, params);
    }

    @Override
    public void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params){
        GLES30.glGetSamplerParameterfv(sampler, pname, params);
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor){
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glBindTransformFeedback(int target, int id){
        GLES30.glBindTransformFeedback(target, id);
    }

    @Override
    public void glDeleteTransformFeedbacks(int n, IntBuffer ids){
        GLES30.glDeleteTransformFeedbacks(ids);
    }

    @Override
    public void glGenTransformFeedbacks(int n, IntBuffer ids){
        GLES30.glGenTransformFeedbacks(ids);
    }

    @Override
    public boolean glIsTransformFeedback(int id){
        return GLES30.glIsTransformFeedback(id);
    }

    @Override
    public void glPauseTransformFeedback(){
        GLES30.glPauseTransformFeedback();
    }

    @Override
    public void glResumeTransformFeedback(){
        GLES30.glResumeTransformFeedback();
    }

    @Override
    public void glProgramParameteri(int program, int pname, int value){
        //not implemented, and it's a hint anyway
    }

    @Override
    public void glInvalidateFramebuffer(int target, int numAttachments, IntBuffer attachments){
        GLES30.glInvalidateFramebuffer(target, attachments);
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, int numAttachments, IntBuffer attachments, int x, int y, int width,
                                           int height){
        GLES30.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
    }
}