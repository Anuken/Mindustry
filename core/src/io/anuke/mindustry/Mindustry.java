package io.anuke.mindustry;

import io.anuke.ucore.modules.ModuleCore;

public class Mindustry extends ModuleCore {
	public static String[] args = {};
	
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
