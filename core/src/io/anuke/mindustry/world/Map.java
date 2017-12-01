package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;

public enum Map{
	maze("desc"),
	fortress("desc"),
	sinkhole("desc"),
	caves("desc"),
	volcano("desc", true),
	caldera("desc"),
	scorch("desc", Color.valueOf("e5d8bb")),
	desert("desc"),
	islands("desc", Color.valueOf("e5d8bb")),
	grassland("desc", Color.valueOf("5ab464")),
	tundra("desc"),
	spiral("desc", Color.valueOf("f7feff")),
	tutorial(false), 
	test1(false),
	test2(false),
	test3(false);
	
	public final boolean visible;
	public final String description;
	public final boolean flipBase;
	public int width, height;
	public Color backgroundColor = Color.valueOf("646464");
	
	private Map(boolean visible){
		this.visible = visible;
		this.flipBase = false;
		this.description = "Test map!";
	}
	
	private Map(String description){
		this(description, false);
	}
	
	private Map(String description, boolean flipBase){
		this.visible = true;
		this.flipBase = flipBase;
		this.description = description;
	}
	
	private Map(String description, Color background){
		this(description);
		backgroundColor = background;
	}
}
