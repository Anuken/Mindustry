package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.Color;

public enum Liquid{
	water(Color.ROYAL);
	
	public final Color color;
	
	private Liquid(Color color){
		this.color = new Color(color);
	}
}
