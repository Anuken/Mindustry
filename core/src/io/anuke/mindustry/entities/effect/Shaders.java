package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Shader;
import io.anuke.ucore.util.Tmp;

public class Shaders{
	public static final Outline outline = new Outline();
	public static final Shield shield = new Shield();
	
	public static class Outline extends Shader{
		public Color color = new Color();
		
		public Outline(){
			super("outline", "default");
		}
		
		@Override
		public void apply(){
			shader.setUniformf("u_color", color);
			shader.setUniformf("u_texsize", Tmp.v1.set(region.getTexture().getWidth(), region.getTexture().getHeight()));
		}
		
	}
	
	public static class Shield extends Shader{
		public Color color = new Color();
		
		public Shield(){
			super("shield", "default");
		}
		
		@Override
		public void apply(){
			float scale = Settings.getBool("pixelate") ? 1 : Core.cameraScale / Core.camera.zoom;
			float scaling = Core.cameraScale / 4f / Core.camera.zoom;
			shader.setUniformf("u_color", color);
			shader.setUniformf("u_time", Timers.time());
			shader.setUniformf("u_scaling", scaling);
			shader.setUniformf("u_offset", Tmp.v1.set(Core.camera.position.x, Core.camera.position.y));
			shader.setUniformf("u_texsize", Tmp.v1.set(region.getTexture().getWidth() / scale, 
					region.getTexture().getHeight() / scale));
		}
		
	}
}
