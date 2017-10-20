package io.anuke.mindustry.entities.effect;

import io.anuke.ucore.entities.Entity;

public class Shield extends Entity{
	public boolean active;
	//TODO
	
	@Override
	public void added(){
		active = true;
	}
	
	@Override
	public void removed(){
		active = false;
	}
}
