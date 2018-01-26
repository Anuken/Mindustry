package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import io.anuke.ucore.util.Bundles;

public class Map{
	public int id = -1;
	public String name;
	public boolean visible = true;
	public boolean flipBase = false;
	public boolean custom = false;
	public boolean oreGen = true;
	public Color backgroundColor = Color.valueOf("646464");
	
	public transient Pixmap pixmap;
	public transient Texture texture;

	public Map(){}

	public String localized(){
		return Bundles.get("map."+name+".name", name);
	}
	
	public int getWidth(){
		return pixmap.getWidth();
	}
	
	public int getHeight(){
		return pixmap.getHeight();
	}
}
