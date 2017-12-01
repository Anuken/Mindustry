package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;

public enum Map{
	delta("Starting map."), 
	pit("Eck."), 
	canyon("the canyon"),
	maze("it's okay."),
	volcano("desc"),
	fortress("desc", true),
	sinkhole("desc"),
	volcanic("desc"),
	rooms("desc"),
	desert("desc"),
	grassland("desc"){{
		backgroundColor = Color.valueOf("5ab464");
	}},
	tundra("desc"),
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
}
