#ifdef __APPLE__
#include <TargetConditionals.h>

#if TARGET_OS_IPHONE
#include "iosgl30.h"
#include <OpenGLES/ES3/gl.h>
#include <OpenGLES/ES3/glext.h>
#include <stdio.h>

static jclass bufferClass;
static jclass byteBufferClass;
static jclass charBufferClass;
static jclass shortBufferClass;
static jclass intBufferClass;
static jclass longBufferClass;
static jclass floatBufferClass;
static jclass doubleBufferClass;
static jclass OOMEClass;
static jclass UOEClass;
static jclass IAEClass;

static jmethodID positionID;


static void
nativeClassInitBuffer(JNIEnv *_env)
{
    jclass bufferClassLocal = _env->FindClass("java/nio/Buffer");
    bufferClass = (jclass) _env->NewGlobalRef(bufferClassLocal);

    byteBufferClass = (jclass) _env->NewGlobalRef(_env->FindClass("java/nio/ByteBuffer"));
    charBufferClass = (jclass) _env->NewGlobalRef(_env->FindClass("java/nio/CharBuffer"));
    shortBufferClass = (jclass) _env->NewGlobalRef(_env->FindClass("java/nio/ShortBuffer"));
    intBufferClass = (jclass) _env->NewGlobalRef(_env->FindClass("java/nio/IntBuffer"));
    longBufferClass = (jclass) _env->NewGlobalRef(_env->FindClass("java/nio/LongBuffer"));
    floatBufferClass = (jclass) _env->NewGlobalRef(_env->FindClass("java/nio/FloatBuffer"));
    doubleBufferClass = (jclass) _env->NewGlobalRef(_env->FindClass("java/nio/DoubleBuffer"));

    positionID = _env->GetMethodID(bufferClass, "position","()I");
    if(positionID == 0) _env->ThrowNew(IAEClass, "Couldn't fetch position() method");
}

static void
nativeClassInit(JNIEnv *_env)
{
    nativeClassInitBuffer(_env);

    jclass IAEClassLocal =
        _env->FindClass("java/lang/IllegalArgumentException");
    jclass OOMEClassLocal =
         _env->FindClass("java/lang/OutOfMemoryError");
    jclass UOEClassLocal =
         _env->FindClass("java/lang/UnsupportedOperationException");

    IAEClass = (jclass) _env->NewGlobalRef(IAEClassLocal);
    OOMEClass = (jclass) _env->NewGlobalRef(OOMEClassLocal);
    UOEClass = (jclass) _env->NewGlobalRef(UOEClassLocal);
}

static jint getElementSizeShift(JNIEnv *_env, jobject buffer) {
	if(_env->IsInstanceOf(buffer, byteBufferClass)) return 0;
	if(_env->IsInstanceOf(buffer, floatBufferClass)) return 2;
	if(_env->IsInstanceOf(buffer, shortBufferClass)) return 1;

	if(_env->IsInstanceOf(buffer, charBufferClass)) return 1;
	if(_env->IsInstanceOf(buffer, intBufferClass)) return 2;
	if(_env->IsInstanceOf(buffer, longBufferClass)) return 3;
	if(_env->IsInstanceOf(buffer, doubleBufferClass)) return 3;

	_env->ThrowNew(IAEClass, "buffer type unkown! (Not a ByteBuffer, ShortBuffer, etc.)");
	return 0;
}

inline jint getBufferPosition(JNIEnv *env, jobject buffer)
{
	jint ret = env->CallIntMethodA(buffer, positionID, 0);
	return  ret;
}

static void *
getDirectBufferPointer(JNIEnv *_env, jobject buffer) {
    if (!buffer) {
        return NULL;
    }
    void* buf = _env->GetDirectBufferAddress(buffer);
    if (buf) {
        jint position = getBufferPosition(_env, buffer);
        jint elementSizeShift = getElementSizeShift(_env, buffer);
        buf = ((char*) buf) + (position << elementSizeShift);
    } else {
        _env->ThrowNew(IAEClass, "Must use a native order direct Buffer");
    }
    return buf;
}

static const char* getString( JNIEnv *env, jstring string )
{
	return (const char*)env->GetStringUTFChars(string, NULL);
}

static void releaseString( JNIEnv *env, jstring string, const char* cString )
{
	env->ReleaseStringUTFChars(string, cString);
}

JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_init
  (JNIEnv *env, jclass)
{
	nativeClassInit( env );
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glReadBuffer
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glReadBuffer
  (JNIEnv *env, jobject, jint mode) {
      glReadBuffer(mode);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDrawRangeElements
 * Signature: (IIIIILjava/nio/Buffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDrawRangeElements__IIIIILjava_nio_Buffer_2
  (JNIEnv *env, jobject, jint mode, jint start, jint end, jint count, jint type, jobject indices) {
  void* dataPtr = getDirectBufferPointer( env, indices );
  glDrawRangeElements(mode, start, end, count, type, dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDrawRangeElements
 * Signature: (IIIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDrawRangeElements__IIIIII
  (JNIEnv *env, jobject, jint mode, jint start, jint end, jint count, jint type, jint offset) {
    glDrawRangeElements(mode, start, end, count, type, (void*)offset);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glTexImage3D
 * Signature: (IIIIIIIIILjava/nio/Buffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glTexImage3D__IIIIIIIIILjava_nio_Buffer_2
  (JNIEnv *env, jobject, jint target, jint level, jint internalformat, jint width, jint height, jint depth, jint border, jint format, jint type, jobject pixels) {
    void* dataPtr = getDirectBufferPointer( env, pixels );
    glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glTexImage3D
 * Signature: (IIIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glTexImage3D__IIIIIIIIII
  (JNIEnv *env, jobject, jint target, jint level, jint internalformat, jint width, jint height, jint depth, jint border, jint format, jint type, jint offset) {
    glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, (void*)offset);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glTexSubImage3D
 * Signature: (IIIIIIIIIILjava/nio/Buffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glTexSubImage3D__IIIIIIIIIILjava_nio_Buffer_2
  (JNIEnv *env, jobject, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint type, jobject pixels) {
    void* dataPtr = getDirectBufferPointer( env, pixels );
    glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glTexSubImage3D
 * Signature: (IIIIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glTexSubImage3D__IIIIIIIIIII
  (JNIEnv *env, jobject, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint type, jint offset) {
    glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, (void*)offset);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glCopyTexSubImage3D
 * Signature: (IIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glCopyTexSubImage3D
  (JNIEnv *env, jobject, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint x, jint y, jint width, jint height) {
    glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenQueries
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenQueries__I_3II
  (JNIEnv *env, jobject, jint n, jintArray ids, jint offset) {
    int* v = (int*)env->GetPrimitiveArrayCritical(ids, 0);
    glGenQueries(n, (GLuint*)&v[offset]);
    env->ReleasePrimitiveArrayCritical(ids, v, 0);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenQueries
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenQueries__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint n, jobject ids) {
    void* dataPtr = getDirectBufferPointer( env, ids );
    glGenQueries(n, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteQueries
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteQueries__I_3II
  (JNIEnv *env, jobject, jint n, jintArray ids, jint offset) {
    void* dataPtr = getDirectBufferPointer( env, ids );
    glDeleteQueries(n, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteQueries
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteQueries__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint n, jobject ids) {
    void* dataPtr = getDirectBufferPointer( env, ids );
    glDeleteQueries(n, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glIsQuery
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_arc_backend_robovm_IOSGLES30_glIsQuery
  (JNIEnv *env, jobject, jint id) {
  return glIsQuery(id);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBeginQuery
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBeginQuery
  (JNIEnv *env, jobject, jint target, jint id) {
    glBeginQuery(target, id);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glEndQuery
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glEndQuery
  (JNIEnv *env, jobject, jint target) {
    glEndQuery(target);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetQueryiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetQueryiv
  (JNIEnv *env, jobject, jint target, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetQueryiv(target, pname, (GLint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetQueryObjectuiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetQueryObjectuiv
  (JNIEnv *env, jobject, jint target, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetQueryObjectuiv(target, pname, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUnmapBuffer
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_arc_backend_robovm_IOSGLES30_glUnmapBuffer
  (JNIEnv *env, jobject, jint target) {
    return glUnmapBuffer(target);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetBufferPointerv
 * Signature: (II)Ljava/nio/Buffer;
 */
JNIEXPORT jobject JNICALL Java_arc_backend_robovm_IOSGLES30_glGetBufferPointerv
  (JNIEnv *env, jobject, jint target, jint pname) {
//FIXME glGetBufferPointerv (GLenum target, GLenum pname, void **params);
    env->ThrowNew(IAEClass, "Unsupported method");
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDrawBuffers
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDrawBuffers
  (JNIEnv *env, jobject, jint n, jobject bufs) {
    void* dataPtr = getDirectBufferPointer( env, bufs );
    glDrawBuffers(n, (GLenum*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniformMatrix2x3fv
 * Signature: (IIZLjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniformMatrix2x3fv
  (JNIEnv *env, jobject, jint location, jint count, jboolean transpose, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniformMatrix2x3fv(location, count, transpose, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniformMatrix3x2fv
 * Signature: (IIZLjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniformMatrix3x2fv
  (JNIEnv *env, jobject, jint location, jint count, jboolean transpose, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniformMatrix3x2fv(location, count, transpose, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniformMatrix2x4fv
 * Signature: (IIZLjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniformMatrix2x4fv
  (JNIEnv *env, jobject, jint location, jint count, jboolean transpose, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniformMatrix2x4fv(location, count, transpose, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniformMatrix4x2fv
 * Signature: (IIZLjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniformMatrix4x2fv
  (JNIEnv *env, jobject, jint location, jint count, jboolean transpose, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniformMatrix4x2fv(location, count, transpose, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniformMatrix3x4fv
 * Signature: (IIZLjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniformMatrix3x4fv
  (JNIEnv *env, jobject, jint location, jint count, jboolean transpose, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniformMatrix3x4fv(location, count, transpose, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniformMatrix4x3fv
 * Signature: (IIZLjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniformMatrix4x3fv
  (JNIEnv *env, jobject, jint location, jint count, jboolean transpose, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniformMatrix4x3fv(location, count, transpose, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBlitFramebuffer
 * Signature: (IIIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBlitFramebuffer
  (JNIEnv *env, jobject, jint srcX0, jint srcY0, jint srcX1, jint srcY1, jint dstX0, jint dstY0, jint dstX1, jint dstY1, jint mask, jint filter) {
    glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glRenderbufferStorageMultisample
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glRenderbufferStorageMultisample
  (JNIEnv *env, jobject, jint target, jint samples, jint internalformat, jint width, jint height) {
    glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glFramebufferTextureLayer
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glFramebufferTextureLayer
  (JNIEnv *env, jobject, jint target, jint attachment, jint texture, jint level, jint layer) {
    glFramebufferTextureLayer(target, attachment, texture, level, layer);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glFlushMappedBufferRange
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glFlushMappedBufferRange
  (JNIEnv *env, jobject, jint target, jint offset, jint length) {
    glFlushMappedBufferRange(target, offset, length);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBindVertexArray
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBindVertexArray
  (JNIEnv *env, jobject, jint array) {
    glBindVertexArray(array);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteVertexArrays
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteVertexArrays__I_3II
  (JNIEnv *env, jobject, jint n, jintArray arrays, jint offset) {
    int* v = (int*)env->GetPrimitiveArrayCritical(arrays, 0);
    glDeleteVertexArrays(n, (GLuint*)&v[offset]);
    env->ReleasePrimitiveArrayCritical(arrays, v, 0);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteVertexArrays
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteVertexArrays__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint n, jobject arrays) {
    void* dataPtr = getDirectBufferPointer( env, arrays );
    glDeleteVertexArrays(n, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenVertexArrays
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenVertexArrays__I_3II
  (JNIEnv *env, jobject, jint n, jintArray arrays, jint offset) {
    int* v = (int*)env->GetPrimitiveArrayCritical(arrays, 0);
    glGenVertexArrays(n, (GLuint*)&v[offset]);
    env->ReleasePrimitiveArrayCritical(arrays, v, 0);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenVertexArrays
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenVertexArrays__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint n, jobject arrays) {
    void* dataPtr = getDirectBufferPointer( env, arrays );
    glGenVertexArrays(n, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glIsVertexArray
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_arc_backend_robovm_IOSGLES30_glIsVertexArray
  (JNIEnv *env, jobject, jint array) {
    return glIsVertexArray(array);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBeginTransformFeedback
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBeginTransformFeedback
  (JNIEnv *env, jobject, jint primitiveMode) {
    glBeginTransformFeedback(primitiveMode);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glEndTransformFeedback
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glEndTransformFeedback
  (JNIEnv *env, jobject) {
    glEndTransformFeedback();
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBindBufferRange
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBindBufferRange
  (JNIEnv *env, jobject, jint target, jint index, jint buffer, jint offset, jint size) {
    glBindBufferRange(target, index, buffer, offset, size);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBindBufferBase
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBindBufferBase
  (JNIEnv *env, jobject, jint target, jint index, jint buffer) {
    glBindBufferBase(target, index, buffer);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glTransformFeedbackVaryings
 * Signature: (I[Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glTransformFeedbackVaryings
  (JNIEnv *env, jobject, jint program, jobjectArray varyings, jint buffermode) {
//FIXME: convert String[] to char**
    env->ThrowNew(IAEClass, "Unsupported method");
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glVertexAttribIPointer
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glVertexAttribIPointer
  (JNIEnv *env, jobject, jint index, jint size, jint type, jint stride, jint offset) {
    glVertexAttribIPointer(index, size, type, stride, (void*)offset);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetVertexAttribIiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetVertexAttribIiv
  (JNIEnv *env, jobject, jint index, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetVertexAttribIiv(index, pname, (GLint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetVertexAttribIuiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetVertexAttribIuiv
  (JNIEnv *env, jobject, jint index, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetVertexAttribIuiv(index, pname, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glVertexAttribI4i
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glVertexAttribI4i
  (JNIEnv *env, jobject, jint index, jint x, jint y, jint z, jint w) {
    glVertexAttribI4i(index, x, y, z, w);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glVertexAttribI4ui
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glVertexAttribI4ui
  (JNIEnv *env, jobject, jint index, jint x, jint y, jint z, jint w) {
    glVertexAttribI4ui(index, x, y, z, w);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetUniformuiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetUniformuiv
  (JNIEnv *env, jobject, jint program, jint location, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetUniformuiv(program, location, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetFragDataLocation
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_arc_backend_robovm_IOSGLES30_glGetFragDataLocation
  (JNIEnv *env, jobject, jint program, jstring name) {
    const char* cname = getString( env, name );
    int loc = glGetFragDataLocation(program, cname);
    releaseString( env, name, cname );
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniform1uiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniform1uiv
  (JNIEnv *env, jobject, jint location, jint count, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniform1uiv(location, count, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniform3uiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniform3uiv
  (JNIEnv *env, jobject, jint location, jint count, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniform3uiv(location, count, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniform4uiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniform4uiv
  (JNIEnv *env, jobject, jint location, jint count, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glUniform4uiv(location, count, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glClearBufferiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glClearBufferiv
  (JNIEnv *env, jobject, jint buffer, jint drawbuffer, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glClearBufferiv(buffer, drawbuffer, (GLint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glClearBufferuiv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glClearBufferuiv
  (JNIEnv *env, jobject, jint buffer, jint drawbuffer, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glClearBufferuiv(buffer, drawbuffer, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glClearBufferfv
 * Signature: (IILjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glClearBufferfv
  (JNIEnv *env, jobject, jint buffer, jint drawbuffer, jobject value) {
    void* dataPtr = getDirectBufferPointer( env, value );
    glClearBufferfv(buffer, drawbuffer, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glClearBufferfi
 * Signature: (IIFI)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glClearBufferfi
  (JNIEnv *env, jobject, jint buffer, jint drawbuffer, jfloat depth, jint stencil) {
    glClearBufferfi(buffer, drawbuffer, depth, stencil);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetStringi
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_arc_backend_robovm_IOSGLES30_glGetStringi
  (JNIEnv *env, jobject, jint name, jint index) {
    return env->NewStringUTF((const char *)glGetStringi(name, index));
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glCopyBufferSubData
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glCopyBufferSubData
  (JNIEnv *env, jobject, jint readTarget, jint writeTarget, jint readOffset, jint writeOffset, jint size) {
    glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetUniformIndices
 * Signature: (I[Ljava/lang/String;Ljava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetUniformIndices
  (JNIEnv *env, jobject, jint, jobjectArray, jobject) {
//FIXME: glGetUniformIndices (GLuint program, GLsizei uniformCount, const GLchar *const*uniformNames, GLuint *uniformIndices);
    env->ThrowNew(IAEClass, "Unsupported method");
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetActiveUniformsiv
 * Signature: (IILjava/nio/IntBuffer;ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetActiveUniformsiv
  (JNIEnv *env, jobject, jint program, jint uniformCount, jobject indices, jint pname, jobject params) {
    void* indicesPtr = getDirectBufferPointer( env, indices );
    void* paramsPtr = getDirectBufferPointer( env, params );
    glGetActiveUniformsiv(program, uniformCount, (GLuint*)indicesPtr, pname, (GLint*)paramsPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetUniformBlockIndex
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_arc_backend_robovm_IOSGLES30_glGetUniformBlockIndex
  (JNIEnv *env, jobject, jint program, jstring uniformBlockName) {
    const char* cname = getString( env, uniformBlockName );
    int loc = glGetUniformBlockIndex(program, cname);
    releaseString( env, uniformBlockName, cname );
    return loc;
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetActiveUniformBlockiv
 * Signature: (IIILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetActiveUniformBlockiv
  (JNIEnv *env, jobject, jint program, jint uniformBlockIndex, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, (GLint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetActiveUniformBlockName
 * Signature: (IILjava/nio/Buffer;Ljava/nio/Buffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetActiveUniformBlockName__IILjava_nio_Buffer_2Ljava_nio_Buffer_2
  (JNIEnv *env, jobject, jint, jint, jobject, jobject) {
//FIXME: glGetActiveUniformBlockName (GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length, GLchar *uniformBlockName);
    env->ThrowNew(IAEClass, "Unsupported method");
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetActiveUniformBlockName
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_arc_backend_robovm_IOSGLES30_glGetActiveUniformBlockName__II
  (JNIEnv *env, jobject, jint, jint) {
//FIXME: glGetActiveUniformBlockName (GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length, GLchar *uniformBlockName);
    env->ThrowNew(IAEClass, "Unsupported method");
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glUniformBlockBinding
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glUniformBlockBinding
  (JNIEnv *env, jobject, jint program, jint uniformBlockIndex, jint uniformBlockBinding) {
    glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDrawArraysInstanced
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDrawArraysInstanced
  (JNIEnv *env, jobject, jint mode, jint first, jint count, jint instancecount) {
    glDrawArraysInstanced(mode, first, count, instancecount);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDrawElementsInstanced
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDrawElementsInstanced
  (JNIEnv *env, jobject, jint mode, jint count, jint type, jint indicesOffset, jint instancecount) {
    glDrawElementsInstanced(mode, count, type, (GLvoid*)indicesOffset, instancecount);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetInteger64v
 * Signature: (ILjava/nio/LongBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetInteger64v
  (JNIEnv *env, jobject, jint pname, jobject data) {
    void* dataPtr = getDirectBufferPointer( env, data );
    glGetInteger64v(pname, (GLint64*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetBufferParameteri64v
 * Signature: (IILjava/nio/LongBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetBufferParameteri64v
  (JNIEnv *env, jobject, jint target, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetBufferParameteri64v(target, pname, (GLint64*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenSamplers
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenSamplers__I_3II
  (JNIEnv *env, jobject, jint count, jintArray samplers, jint offset) {
    int* v = (int*)env->GetPrimitiveArrayCritical(samplers, 0);
    glGenSamplers(count, (GLuint*)&v[offset]);
    env->ReleasePrimitiveArrayCritical(samplers, v, 0);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenSamplers
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenSamplers__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint count, jobject samplers) {
    void* dataPtr = getDirectBufferPointer( env, samplers );
    glGenSamplers(count, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteSamplers
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteSamplers__I_3II
  (JNIEnv *env, jobject, jint count, jintArray samplers, jint offset) {
    int* v = (int*)env->GetPrimitiveArrayCritical(samplers, 0);
    glDeleteSamplers(count, (GLuint*)&v[offset]);
    env->ReleasePrimitiveArrayCritical(samplers, v, 0);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteSamplers
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteSamplers__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint count, jobject samplers) {
    void* dataPtr = getDirectBufferPointer( env, samplers );
    glDeleteSamplers(count, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glIsSampler
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_arc_backend_robovm_IOSGLES30_glIsSampler
  (JNIEnv *env, jobject, jint sampler) {
    return glIsSampler(sampler);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBindSampler
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBindSampler
  (JNIEnv *env, jobject, jint unit, jint sampler) {
    glBindSampler(unit, sampler);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glSamplerParameteri
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glSamplerParameteri
  (JNIEnv *env, jobject, jint sampler, jint pname, jint param) {
    glSamplerParameteri(sampler, pname, param);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glSamplerParameteriv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glSamplerParameteriv
  (JNIEnv *env, jobject, jint sampler, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glSamplerParameteriv(sampler, pname, (GLint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glSamplerParameterf
 * Signature: (IIF)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glSamplerParameterf
  (JNIEnv *env, jobject, jint sampler, jint pname, jfloat param) {
    glSamplerParameterf(sampler, pname, param);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glSamplerParameterfv
 * Signature: (IILjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glSamplerParameterfv
  (JNIEnv *env, jobject, jint sampler, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glSamplerParameterfv(sampler, pname, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetSamplerParameteriv
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetSamplerParameteriv
  (JNIEnv *env, jobject, jint sampler, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetSamplerParameteriv(sampler, pname, (GLint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGetSamplerParameterfv
 * Signature: (IILjava/nio/FloatBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGetSamplerParameterfv
  (JNIEnv *env, jobject, jint sampler, jint pname, jobject params) {
    void* dataPtr = getDirectBufferPointer( env, params );
    glGetSamplerParameterfv(sampler, pname, (GLfloat*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glVertexAttribDivisor
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glVertexAttribDivisor
  (JNIEnv *env, jobject, jint index, jint divisor) {
    glVertexAttribDivisor(index, divisor);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glBindTransformFeedback
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glBindTransformFeedback
  (JNIEnv *env, jobject, jint target, jint id) {
    glBindTransformFeedback(target, id);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteTransformFeedbacks
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteTransformFeedbacks__I_3II
  (JNIEnv *env, jobject, jint n, jintArray ids, jint offset) {
    int* v = (int*)env->GetPrimitiveArrayCritical(ids, 0);
    glDeleteTransformFeedbacks(n, (GLuint*)&v[offset]);
    env->ReleasePrimitiveArrayCritical(ids, v, 0);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glDeleteTransformFeedbacks
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glDeleteTransformFeedbacks__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint n, jobject ids) {
    void* dataPtr = getDirectBufferPointer( env, ids );
    glDeleteTransformFeedbacks(n, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenTransformFeedbacks
 * Signature: (I[II)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenTransformFeedbacks__I_3II
  (JNIEnv *env, jobject, jint n, jintArray ids, jint offset) {
    int* v = (int*)env->GetPrimitiveArrayCritical(ids, 0);
    glGenTransformFeedbacks(n, (GLuint*)&v[offset]);
    env->ReleasePrimitiveArrayCritical(ids, v, 0);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glGenTransformFeedbacks
 * Signature: (ILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glGenTransformFeedbacks__ILjava_nio_IntBuffer_2
  (JNIEnv *env, jobject, jint n, jobject ids) {
    void* dataPtr = getDirectBufferPointer( env, ids );
    glGenTransformFeedbacks(n, (GLuint*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glIsTransformFeedback
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_arc_backend_robovm_IOSGLES30_glIsTransformFeedback
  (JNIEnv *env, jobject, jint id) {
    return glIsTransformFeedback(id);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glPauseTransformFeedback
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glPauseTransformFeedback
  (JNIEnv *env, jobject) {
    glPauseTransformFeedback();
}
/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glResumeTransformFeedback
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glResumeTransformFeedback
  (JNIEnv *env, jobject) {
    glResumeTransformFeedback();
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glProgramParameteri
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glProgramParameteri
  (JNIEnv *env, jobject, jint program, jint pname, jint value) {
    glProgramParameteri(program, pname, value);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glInvalidateFramebuffer
 * Signature: (IILjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glInvalidateFramebuffer
  (JNIEnv *env, jobject, jint target, jint numAttachments, jobject attachments) {
    void* dataPtr = getDirectBufferPointer( env, attachments );
    glInvalidateFramebuffer(target, numAttachments, (GLenum*)dataPtr);
}

/*
 * Class:     arc_backend_robovm_IOSGLES30
 * Method:    glInvalidateSubFramebuffer
 * Signature: (IILjava/nio/IntBuffer;IIII)V
 */
JNIEXPORT void JNICALL Java_arc_backend_robovm_IOSGLES30_glInvalidateSubFramebuffer
  (JNIEnv *env, jobject, jint target, jint numAttachments, jobject attachments, jint x, jint y, jint width, jint height) {
    void* dataPtr = getDirectBufferPointer( env, attachments );
    glInvalidateSubFramebuffer(target, numAttachments, (GLenum*)dataPtr, x, y, width, height);
}


#endif
#endif
