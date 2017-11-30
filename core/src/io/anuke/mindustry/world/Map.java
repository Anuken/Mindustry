package io.anuke.mindustry.world;

public enum Map{
	delta("Starting map."), 
	pit("Eck."), 
	canyon("the canyon"),
	maze("it's okay."),
	volcano("desc"),
	fortress("desc", true),
	tutorial(false), 
	test1(false),
	test2(false);
	
	public final boolean visible;
	public final String description;
	public final boolean flipBase;
	public int width, height;
	
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
