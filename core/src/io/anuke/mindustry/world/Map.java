package io.anuke.mindustry.world;

public enum Map{
	delta, canyon, pit, maze, tutorial(false), test(false);
	
	public final boolean visible;
	
	private Map(boolean visible){
		this.visible = visible;
	}
	
	private Map(){
		this(true);
	}
}
