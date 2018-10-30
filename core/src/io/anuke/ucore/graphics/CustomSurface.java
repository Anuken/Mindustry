package io.anuke.ucore.graphics;

/**A surface that, by default, does not do anything. Used for things like postprocessors.*/
public abstract class CustomSurface extends Surface{
	
	public CustomSurface() {
		super(1, 0);
	}
	
	@Override
	public void begin(boolean clear){
		
	}
	
	@Override
	public void end(boolean render){
		
	}
	
	@Override
	public void resize(){
		
	}
	
	@Override
	public void dispose(){
		
	}
}
