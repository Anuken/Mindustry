package io.anuke.mindustry;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.graphics.Shader;
import io.anuke.ucore.util.Tmp;

public class Shaders{
	public static final Outline outline = new Outline();
	
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
}
