package io.anuke.ucore.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;

public class CacheDrawBatch implements Batch{
    private final CacheBatch batch;
    private boolean drawing;

    public CacheDrawBatch(CacheBatch batch){
        this.batch = batch;
    }

    @Override
    public void begin() {
        drawing = true;
        batch.beginDraw();
    }

    @Override
    public void end() {
        batch.endDraw();
        drawing = false;
    }

    @Override
    public void setColor(Color tint) {
        stub();
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        stub();
    }

    @Override
    public void setColor(float color) {
        stub();
    }

    @Override
    public Color getColor() {
        stub();
        return null;
    }

    @Override
    public float getPackedColor() {
        stub();
        return 0;
    }

    @Override
    public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        stub();
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        stub();
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        stub();
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        stub();
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        stub();
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        stub();
    }

    @Override
    public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        stub();
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {
        stub();
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        stub();
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        stub();
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
        stub();
    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {
        stub();
    }

    @Override
    public void flush() {

    }

    @Override
    public void disableBlending() {

    }

    @Override
    public void enableBlending() {

    }

    @Override
    public void setBlendFunction(int srcFunc, int dstFunc) {

    }

    @Override
    public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {

    }

    @Override
    public int getBlendSrcFunc() {
        return 0;
    }

    @Override
    public int getBlendDstFunc() {
        return 0;
    }

    @Override
    public int getBlendSrcFuncAlpha() {
        return 0;
    }

    @Override
    public int getBlendDstFuncAlpha() {
        return 0;
    }

    @Override
    public Matrix4 getProjectionMatrix() {
        stub();
        return null;
    }

    @Override
    public Matrix4 getTransformMatrix() {
        stub();
        return null;
    }

    @Override
    public void setProjectionMatrix(Matrix4 projection) {
        batch.setProjectionMatrix(projection);
    }

    @Override
    public void setTransformMatrix(Matrix4 transform) {
        batch.setTransformMatrix(transform);
    }

    @Override
    public void setShader(ShaderProgram shader) {
        batch.setShader(shader);
    }

    @Override
    public ShaderProgram getShader() {
        return null;
    }

    @Override
    public boolean isBlendingEnabled() {
        return false;
    }

    @Override
    public boolean isDrawing() {
        return drawing;
    }

    @Override
    public void dispose() {
        stub();
    }

    private void stub(){
        throw new IllegalArgumentException("Stub method!");
    }
}
