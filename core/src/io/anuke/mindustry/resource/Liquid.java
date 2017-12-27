package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.util.Bundles;

public enum Liquid{
	water(Color.ROYAL),
	plasma(Color.CORAL),
	lava(Color.valueOf("ed5334")),
	oil(Color.valueOf("292929"));
	
	public final Color color;
	
	private Liquid(Color color){
		this.color = new Color(color);
	}

	public String localized(){
		return Bundles.get("liquid."+name() + ".name");
	}

	@Override
	public String toString(){
		return localized();
	}
}
