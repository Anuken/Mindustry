package io.anuke.ucore.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import io.anuke.ucore.util.Mathf;

/**A 'batch' that calls Caches.draw() for most operations. Many operations are unsupported.*/
public class CacheBatch implements Batch{
	protected SpriteCache cache;
	protected CacheDrawBatch drawbatch;
	protected boolean caching;
	protected int lastCache;
	
	public CacheBatch(int size){
		this.cache = new SpriteCache(size, false);
		drawbatch = new CacheDrawBatch(this);
	}
	
	public void beginDraw(){
		cache.begin();
	}

	public CacheDrawBatch drawBatch() {
		return drawbatch;
	}

	public void endDraw(){
		cache.end();
	}
	
	public void drawCache(int id){
		cache.draw(id);
	}
	
	public int getLastCache(){
		return lastCache;
	}
	
	public void clear(){
		cache.clear();
	}

	@Override
	public void dispose(){
		cache.dispose();
	}

	@Override
	public void begin(){
		cache.beginCache();
		caching = true;
	}
	
	public void begin(int cacheid){
		cache.beginCache(cacheid);
		caching = true;
	}

	@Override
	public void end(){
		lastCache = cache.endCache();
		caching = false;
	}

	@Override
	public void setColor(Color tint){
		cache.setColor(tint);
	}

	@Override
	public void setColor(float r, float g, float b, float a){
		cache.setColor(r, g, b, a);
	}

	@Override
	public void setColor(float color){
		cache.setColor(color);
	}

	@Override
	public Color getColor(){
		return cache.getColor();
	}

	@Override
	public float getPackedColor(){
		return cache.getColor().toFloatBits();
	}

	@Override
	public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY){
		cache.add(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
	}

	@Override
	public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY){
		cache.add(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY);
	}

	@Override
	public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight){
		cache.add(texture, x, y, srcX, srcY, srcWidth, srcHeight);
	}

	@Override
	public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2){
		stub();
	}

	@Override
	public void draw(Texture texture, float x, float y){
		cache.add(texture, x, y);
	}

	@Override
	public void draw(Texture texture, float x, float y, float width, float height){
		stub();
	}

	@Override
	public void draw(Texture texture, float[] spriteVertices, int offset, int count){
		stub();
	}

	@Override
	public void draw(TextureRegion region, float x, float y){
		cache.add(region, x, y);
	}

	@Override
	public void draw(TextureRegion region, float x, float y, float width, float height){
		cache.add(region, x, y, width, height);
	}

	@Override
	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
		cache.add(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
	}

	@Override
	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise){
		cache.add(region, x, y, originX, originY, width, height, scaleX, scaleY, -1 * rotation * Mathf.sign(clockwise));
	}

	@Override
	public void draw(TextureRegion region, float width, float height, Affine2 transform){
		stub();
	}

	@Override
	public void flush(){
		stub();
	}

	@Override
	public void disableBlending(){
		stub();
	}

	@Override
	public void enableBlending(){
		stub();
	}

	@Override
	public void setBlendFunction(int srcFunc, int dstFunc){
		stub();
	}

	@Override
	public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
		stub();
	}

	@Override
	public int getBlendSrcFunc(){
		stub();
		return 0;
	}

	@Override
	public int getBlendDstFunc(){
		stub();
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
	public Matrix4 getProjectionMatrix(){
		return cache.getProjectionMatrix();
	}

	@Override
	public Matrix4 getTransformMatrix(){
		return cache.getTransformMatrix();
	}

	@Override
	public void setProjectionMatrix(Matrix4 projection){
		cache.setProjectionMatrix(projection);
	}

	@Override
	public void setTransformMatrix(Matrix4 transform){
		cache.setTransformMatrix(transform);
	}

	@Override
	public void setShader(ShaderProgram shader){
		cache.setShader(shader);
	}

	@Override
	public ShaderProgram getShader(){
		stub();
		return null;
	}

	@Override
	public boolean isBlendingEnabled(){
		stub();
		return false;
	}

	@Override
	public boolean isDrawing(){
		return caching;
	}
	
	private void stub(){
		throw new IllegalArgumentException("Stub method!");
	}

}
