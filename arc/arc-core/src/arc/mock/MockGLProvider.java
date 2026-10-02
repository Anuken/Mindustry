package arc.mock;

import arc.graphics.gl.*;

import java.nio.*;

public class MockGLProvider implements GLProvider{
    @Override
    public void glActiveTexture(int texture){

    }

    @Override
    public void glBindTexture(int target, int texture){

    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor){

    }

    @Override
    public void glClear(int mask){

    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha){

    }

    @Override
    public void glClearDepthf(float depth){

    }

    @Override
    public void glClearStencil(int s){

    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha){

    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data){

    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer data){

    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border){

    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height){

    }

    @Override
    public void glCullFace(int mode){

    }

    @Override
    public void glDeleteTexture(int texture){

    }

    @Override
    public void glDepthFunc(int func){

    }

    @Override
    public void glDepthMask(boolean flag){

    }

    @Override
    public void glDepthRangef(float zNear, float zFar){

    }

    @Override
    public void glDisable(int cap){

    }

    @Override
    public void glDrawArrays(int mode, int first, int count){

    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices){

    }

    @Override
    public void glEnable(int cap){

    }

    @Override
    public void glFinish(){

    }

    @Override
    public void glFlush(){

    }

    @Override
    public void glFrontFace(int mode){

    }

    @Override
    public int glGenTexture(){
        return 0;
    }

    @Override
    public int glGetError(){
        return 0;
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer params){

    }

    @Override
    public String glGetString(int name){
        return "";
    }

    @Override
    public void glHint(int target, int mode){

    }

    @Override
    public void glLineWidth(float width){

    }

    @Override
    public void glPixelStorei(int pname, int param){

    }

    @Override
    public void glPolygonOffset(float factor, float units){

    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels){

    }

    @Override
    public void glScissor(int x, int y, int width, int height){

    }

    @Override
    public void glStencilFunc(int func, int ref, int mask){

    }

    @Override
    public void glStencilMask(int mask){

    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass){

    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels){

    }

    @Override
    public void glTexParameterf(int target, int pname, float param){

    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels){

    }

    @Override
    public void glViewport(int x, int y, int width, int height){

    }

    @Override
    public void glAttachShader(int program, int shader){

    }

    @Override
    public void glBindAttribLocation(int program, int index, String name){

    }

    @Override
    public void glBindBuffer(int target, int buffer){

    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer){

    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer){

    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha){

    }

    @Override
    public void glBlendEquation(int mode){

    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha){

    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha){

    }

    @Override
    public void glBufferData(int target, int size, Buffer data, int usage){

    }

    @Override
    public void glBufferSubData(int target, int offset, int size, Buffer data){

    }

    @Override
    public int glCheckFramebufferStatus(int target){
        return 0;
    }

    @Override
    public void glCompileShader(int shader){

    }

    @Override
    public int glCreateProgram(){
        return 0;
    }

    @Override
    public int glCreateShader(int type){
        return 0;
    }

    @Override
    public void glDeleteBuffer(int buffer){

    }

    @Override
    public void glDeleteFramebuffer(int framebuffer){

    }

    @Override
    public void glDeleteProgram(int program){

    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer){

    }

    @Override
    public void glDeleteShader(int shader){

    }

    @Override
    public void glDetachShader(int program, int shader){

    }

    @Override
    public void glDisableVertexAttribArray(int index){

    }

    @Override
    public void glDrawElements(int mode, int count, int type, int indices){

    }

    @Override
    public void glEnableVertexAttribArray(int index){

    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer){

    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level){

    }

    @Override
    public int glGenBuffer(){
        return 0;
    }

    @Override
    public void glGenerateMipmap(int target){

    }

    @Override
    public int glGenFramebuffer(){
        return 0;
    }

    @Override
    public int glGenRenderbuffer(){
        return 0;
    }

    @Override
    public String glGetActiveAttrib(int program, int index, IntBuffer size, IntBuffer type){
        return "";
    }

    @Override
    public String glGetActiveUniform(int program, int index, IntBuffer size, IntBuffer type){
        return "";
    }

    @Override
    public int glGetAttribLocation(int program, String name){
        return 0;
    }

    @Override
    public void glGetBooleanv(int pname, Buffer params){

    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params){

    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params){

    }

    @Override
    public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params){

    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params){

    }

    @Override
    public String glGetProgramInfoLog(int program){
        return "";
    }

    @Override
    public void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params){

    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params){

    }

    @Override
    public String glGetShaderInfoLog(int shader){
        return "";
    }

    @Override
    public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision){

    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params){

    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params){

    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params){

    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params){

    }

    @Override
    public int glGetUniformLocation(int program, String name){
        return 0;
    }

    @Override
    public void glGetVertexAttribfv(int index, int pname, FloatBuffer params){

    }

    @Override
    public void glGetVertexAttribiv(int index, int pname, IntBuffer params){

    }

    @Override
    public boolean glIsBuffer(int buffer){
        return false;
    }

    @Override
    public boolean glIsEnabled(int cap){
        return false;
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer){
        return false;
    }

    @Override
    public boolean glIsProgram(int program){
        return false;
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer){
        return false;
    }

    @Override
    public boolean glIsShader(int shader){
        return false;
    }

    @Override
    public boolean glIsTexture(int texture){
        return false;
    }

    @Override
    public void glLinkProgram(int program){

    }

    @Override
    public void glReleaseShaderCompiler(){

    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height){

    }

    @Override
    public void glSampleCoverage(float value, boolean invert){

    }

    @Override
    public void glShaderSource(int shader, String string){

    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask){

    }

    @Override
    public void glStencilMaskSeparate(int face, int mask){

    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass){

    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params){

    }

    @Override
    public void glTexParameteri(int target, int pname, int param){

    }

    @Override
    public void glTexParameteriv(int target, int pname, IntBuffer params){

    }

    @Override
    public void glUniform1f(int location, float x){

    }

    @Override
    public void glUniform1fv(int location, int count, FloatBuffer v){

    }

    @Override
    public void glUniform1fv(int location, int count, float[] v, int offset){

    }

    @Override
    public void glUniform1i(int location, int x){

    }

    @Override
    public void glUniform1iv(int location, int count, IntBuffer v){

    }

    @Override
    public void glUniform1iv(int location, int count, int[] v, int offset){

    }

    @Override
    public void glUniform2f(int location, float x, float y){

    }

    @Override
    public void glUniform2fv(int location, int count, FloatBuffer v){

    }

    @Override
    public void glUniform2fv(int location, int count, float[] v, int offset){

    }

    @Override
    public void glUniform2i(int location, int x, int y){

    }

    @Override
    public void glUniform2iv(int location, int count, IntBuffer v){

    }

    @Override
    public void glUniform2iv(int location, int count, int[] v, int offset){

    }

    @Override
    public void glUniform3f(int location, float x, float y, float z){

    }

    @Override
    public void glUniform3fv(int location, int count, FloatBuffer v){

    }

    @Override
    public void glUniform3fv(int location, int count, float[] v, int offset){

    }

    @Override
    public void glUniform3i(int location, int x, int y, int z){

    }

    @Override
    public void glUniform3iv(int location, int count, IntBuffer v){

    }

    @Override
    public void glUniform3iv(int location, int count, int[] v, int offset){

    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w){

    }

    @Override
    public void glUniform4fv(int location, int count, FloatBuffer v){

    }

    @Override
    public void glUniform4fv(int location, int count, float[] v, int offset){

    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w){

    }

    @Override
    public void glUniform4iv(int location, int count, IntBuffer v){

    }

    @Override
    public void glUniform4iv(int location, int count, int[] v, int offset){

    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset){

    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset){

    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset){

    }

    @Override
    public void glUseProgram(int program){

    }

    @Override
    public void glValidateProgram(int program){

    }

    @Override
    public void glVertexAttrib1f(int indx, float x){

    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values){

    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y){

    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values){

    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z){

    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values){

    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w){

    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values){

    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr){

    }

    @Override
    public void glReadBuffer(int mode){

    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices){

    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset){

    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, Buffer pixels){

    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, int offset){

    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, Buffer pixels){

    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, int offset){

    }

    @Override
    public void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height){

    }

    @Override
    public void glGenQueries(int n, IntBuffer ids){

    }

    @Override
    public void glDeleteQueries(int n, IntBuffer ids){

    }

    @Override
    public boolean glIsQuery(int id){
        return false;
    }

    @Override
    public void glBeginQuery(int target, int id){

    }

    @Override
    public void glEndQuery(int target){

    }

    @Override
    public void glGetQueryiv(int target, int pname, IntBuffer params){

    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params){

    }

    @Override
    public boolean glUnmapBuffer(int target){
        return false;
    }

    @Override
    public Buffer glGetBufferPointerv(int target, int pname){
        return null;
    }

    @Override
    public void glDrawBuffers(int n, IntBuffer bufs){

    }

    @Override
    public void glUniformMatrix2x3fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix3x2fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix2x4fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix4x2fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix3x4fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glUniformMatrix4x3fv(int location, int count, boolean transpose, FloatBuffer value){

    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter){

    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height){

    }

    @Override
    public void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer){

    }

    @Override
    public void glFlushMappedBufferRange(int target, int offset, int length){

    }

    @Override
    public void glBindVertexArray(int array){

    }

    @Override
    public void glDeleteVertexArrays(int n, IntBuffer arrays){

    }

    @Override
    public void glGenVertexArrays(int n, IntBuffer arrays){

    }

    @Override
    public boolean glIsVertexArray(int array){
        return false;
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode){

    }

    @Override
    public void glEndTransformFeedback(){

    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, int offset, int size){

    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer){

    }

    @Override
    public void glTransformFeedbackVaryings(int program, String[] varyings, int bufferMode){

    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, int offset){

    }

    @Override
    public void glGetVertexAttribIiv(int index, int pname, IntBuffer params){

    }

    @Override
    public void glGetVertexAttribIuiv(int index, int pname, IntBuffer params){

    }

    @Override
    public void glVertexAttribI4i(int index, int x, int y, int z, int w){

    }

    @Override
    public void glVertexAttribI4ui(int index, int x, int y, int z, int w){

    }

    @Override
    public void glGetUniformuiv(int program, int location, IntBuffer params){

    }

    @Override
    public int glGetFragDataLocation(int program, String name){
        return 0;
    }

    @Override
    public void glUniform1uiv(int location, int count, IntBuffer value){

    }

    @Override
    public void glUniform3uiv(int location, int count, IntBuffer value){

    }

    @Override
    public void glUniform4uiv(int location, int count, IntBuffer value){

    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value){

    }

    @Override
    public void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value){

    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value){

    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil){

    }

    @Override
    public String glGetStringi(int name, int index){
        return "";
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size){

    }

    @Override
    public void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices){

    }

    @Override
    public void glGetActiveUniformsiv(int program, int uniformCount, IntBuffer uniformIndices, int pname, IntBuffer params){

    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName){
        return 0;
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params){

    }

    @Override
    public void glGetActiveUniformBlockName(int program, int uniformBlockIndex, Buffer length, Buffer uniformBlockName){

    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding){

    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instanceCount){

    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount){

    }

    @Override
    public void glGetInteger64v(int pname, LongBuffer params){

    }

    @Override
    public void glGetBufferParameteri64v(int target, int pname, LongBuffer params){

    }

    @Override
    public void glGenSamplers(int count, IntBuffer samplers){

    }

    @Override
    public void glDeleteSamplers(int count, IntBuffer samplers){

    }

    @Override
    public boolean glIsSampler(int sampler){
        return false;
    }

    @Override
    public void glBindSampler(int unit, int sampler){

    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param){

    }

    @Override
    public void glSamplerParameteriv(int sampler, int pname, IntBuffer param){

    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param){

    }

    @Override
    public void glSamplerParameterfv(int sampler, int pname, FloatBuffer param){

    }

    @Override
    public void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params){

    }

    @Override
    public void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params){

    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor){

    }

    @Override
    public void glBindTransformFeedback(int target, int id){

    }

    @Override
    public void glDeleteTransformFeedbacks(int n, IntBuffer ids){

    }

    @Override
    public void glGenTransformFeedbacks(int n, IntBuffer ids){

    }

    @Override
    public boolean glIsTransformFeedback(int id){
        return false;
    }

    @Override
    public void glPauseTransformFeedback(){

    }

    @Override
    public void glResumeTransformFeedback(){

    }

    @Override
    public void glProgramParameteri(int program, int pname, int value){

    }

    @Override
    public void glInvalidateFramebuffer(int target, int numAttachments, IntBuffer attachments){

    }

    @Override
    public void glInvalidateSubFramebuffer(int target, int numAttachments, IntBuffer attachments, int x, int y, int width, int height){

    }
}
