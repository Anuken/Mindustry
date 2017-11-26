package io.anuke.mindustry.world;

public enum Map{
	delta("Starting map."), 
	pit("Eck."), 
	maze("it's okay."),
	volcano("desc"),
	tutorial(false), 
	test1(false),
	test2(false);
	
	public final boolean visible;
	public final boolean sandbox;
	public final String description;
	public int width, height;
	
	private Map(boolean visible){
		this.visible = visible;
		this.sandbox = false;
		this.description = "Test map!";
	}
	
	private Map(String description){
		this(description, false);
	}
	
	private Map(String description, boolean sandbox){
		this.visible = true;
		this.sandbox = sandbox;
		this.description = description;
	}
}
