package io.anuke.ucore.modules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.ucore.core.Core.batch;
import static io.anuke.ucore.core.Core.camera;

public abstract class RendererModule extends Module{
	public Color clearColor = Color.BLACK;
	public float shakeIntensity, shaketime;
	
	public boolean pixelate;
	public Surface pixelSurface;
	
	private Object recorder;
	private Class<?> recorderClass;
	
	public RendererModule(){
		Settings.defaults("screenshake", 4);
		
		Effects.setScreenShakeProvider((intensity, duration) -> {
			shakeIntensity = Math.max(intensity, shakeIntensity);
			shaketime = Math.max(shaketime, duration);
		});
		
		//set up recorder if possible; ignore if it doesn't work
		//(to disable the recorder, just don't call record(), or remove GifRecorder from the classpath)
		try{
			recorderClass = ClassReflection.forName("io.anuke.gif.GifRecorder");
			recorder = ClassReflection.getConstructor(recorderClass, Batch.class).newInstance(new SpriteBatch());
			if(UCore.isAssets()){
				Method method = ClassReflection.getMethod(recorderClass, "setExportDirectory", FileHandle.class);
				method.invoke(recorder, Gdx.files.local("../../desktop/gifexport"));
			}
		}catch (Exception e){}
	}
	
	public void setCamera(float x, float y){
		camera.position.set(x, y, 0);
	}
	
	public void smoothCamera(float x, float y, float alpha){
		camera.position.interpolate(Tmp.v31.set(x, y, 0), Math.min(alpha*delta(), 1f), Interpolation.linear);
	}
	
	public void limitCamera(float lim, float targetX, float targetY){
		if(Math.abs(targetX - camera.position.x) > lim){
			camera.position.x = targetX - Mathf.clamp(targetX - camera.position.x, -lim, lim);
		}
		
		if(Math.abs(targetY - camera.position.y) > lim){
			camera.position.y = targetY - Mathf.clamp(targetY - camera.position.y, -lim, lim);
		}
	}
	
	public void roundCamera(){
		camera.position.x = (int)camera.position.x;
		camera.position.y = (int)camera.position.y;
	}
	
	public void clampCamera(float minx, float miny, float maxx, float maxy){
		float vw = camera.viewportWidth/2f * camera.zoom;
		float vh = camera.viewportHeight/2f * camera.zoom;
		Vector3 pos = camera.position;
		
		if(pos.x - vw < minx)
			pos.x = vw + minx;
		
		if(pos.y - vh < miny)
			pos.y = vh + miny;
		
		if(pos.x + vw > maxx)
			pos.x = maxx-vw;
		
		if(pos.y + vh > maxy)
			pos.y = maxy-vh;

		if(vw*2f > maxx - minx){
			pos.x = (maxx + minx)/2f;
		}

		if(vh*2f > maxy - miny){
			pos.y = (maxy + miny)/2f;
		}
	}
	
	public void updateShake(){
		updateShake(1f);
	}
	
	public void updateShake(float scale){
		if(shaketime > 0){
			float intensity = shakeIntensity*(Settings.getInt("screenshake")/4f)*scale;
			camera.position.add(Mathf.range(intensity), Mathf.range(intensity), 0);
			shakeIntensity -= 0.25f*delta();
			shaketime -= delta();
			shakeIntensity = Mathf.clamp(shakeIntensity, 0f, 100f);
		}else{
			shakeIntensity = 0f;
		}
	}
	
	/**Updates the camera, clears the screen, begins the batch and calls draw().*/
	public void drawDefault(){
		camera.update();
		
		batch.setProjectionMatrix(camera.combined);
		
		if(pixelate) 
			Graphics.surface(pixelSurface);
		else
			batch.begin();

		clearScreen(clearColor);
		
		draw();
		
		if(pixelate) 
			Graphics.flushSurface();

		batch.end();
	}
	
	/**override this*/
	public void draw(){}
	
	/**Updates the gif recorder. Does nothing on GWT or projects without it on the classpath.*/
	public void record(){
		if(recorder == null) return;
		
		try{
			Method method = ClassReflection.getMethod(recorderClass, "update");
			method.invoke(recorder);
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public void pixelate(){
		pixelate(-1);
	}
	
	public void pixelate(int scl){
		pixelSurface = Graphics.createSurface(scl == -1 ? Core.cameraScale : scl);
		pixelate = true;
	}
	
	@Override
	public void resize(int width, int height){
		camera.setToOrtho(false, width/Core.cameraScale, height/Core.cameraScale);

		resize();
	}
}
