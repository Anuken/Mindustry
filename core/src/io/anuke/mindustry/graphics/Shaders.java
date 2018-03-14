package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Shader;
import io.anuke.ucore.scene.ui.layout.Unit;

public class Shaders{
	public static final Outline outline = new Outline();
	public static final Shield shield = new Shield();
	public static final SurfaceShader water = new SurfaceShader("water");
	public static final SurfaceShader lava = new SurfaceShader("lava");
	public static final SurfaceShader oil = new SurfaceShader("oil");

	private static final Vector2 vec = new Vector2();

	public static class Outline extends Shader{
		public Color color = new Color();
		public float lighten = 0f;

		public Outline(){
			super("outline", "default");
		}
		
		@Override
		public void apply(){
			shader.setUniformf("u_color", color);
			shader.setUniformf("u_lighten", lighten);
			shader.setUniformf("u_texsize", vec.set(region.getTexture().getWidth(), region.getTexture().getHeight()));
		}
	}
	
	public static class Shield extends Shader{
		public static final int MAX_HITS = 3*64;
		public Color color = new Color();
		public FloatArray hits;
		
		public Shield(){
			super("shield", "default");
		}
		
		@Override
		public void apply(){
			float scaling = Core.cameraScale / 4f / Core.camera.zoom;
			if(hits.size > 0){
				shader.setUniform3fv("u_hits[0]", hits.items, 0, Math.min(hits.size, MAX_HITS));
				shader.setUniformi("u_hitamount", Math.min(hits.size, MAX_HITS)/3);
			}
			shader.setUniformf("u_dp", Unit.dp.scl(1f));
			shader.setUniformf("u_color", color);
			shader.setUniformf("u_time", Timers.time() / Unit.dp.scl(1f));
			shader.setUniformf("u_scaling", scaling);
			shader.setUniformf("u_offset",
					Core.camera.position.x - Core.camera.viewportWidth/2 * Core.camera.zoom,
							Core.camera.position.y - Core.camera.viewportHeight/2 * Core.camera.zoom);
			shader.setUniformf("u_texsize", Gdx.graphics.getWidth() / Core.cameraScale * Core.camera.zoom,
					Gdx.graphics.getHeight() / Core.cameraScale * Core.camera.zoom);
		}
	}

	public static class SurfaceShader extends Shader{

		public SurfaceShader(String frag){
			super(frag, "default");
		}

		@Override
		public void apply(){
			shader.setUniformf("camerapos",
					Core.camera.position.x - Core.camera.viewportWidth/2 * Core.camera.zoom,
					Core.camera.position.y - Core.camera.viewportHeight/2 * Core.camera.zoom);
			shader.setUniformf("screensize", Gdx.graphics.getWidth() / Core.cameraScale * Core.camera.zoom,
					Gdx.graphics.getHeight() / Core.cameraScale * Core.camera.zoom);
			shader.setUniformf("time", Timers.time());
		}
	}
}
