package io.anuke.mindustry;

import io.anuke.ucore.modules.Core;

public class Mindustry extends Core {
	
	@Override
	public void init(){
		add(Vars.control = new Control());
		add(Vars.ui = new UI());
	}
	
	@Override
	public void postInit(){
		Vars.control.reset();
	}
}
