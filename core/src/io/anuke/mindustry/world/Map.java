package io.anuke.mindustry.world;

public enum Map{
	delta("Starting map."), 
	canyon("Badly drawn map."), 
	pit("Eck."), 
	maze("it's okay."), 
	maze2("test"),
	maze3("test"),
	maze4("test"),
	maze5("test"),
	tutorial(false), 
	test(false);
	
	public final boolean visible;
	public final String description;
	
	private Map(boolean visible){
		this.visible = visible;
		this.description = "Test map!";
	}
	
	private Map(String description){
		this.visible = true;
		this.description = description;
	}
}
