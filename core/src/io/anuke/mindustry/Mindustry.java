package io.anuke.mindustry;

import io.anuke.mindustry.GameState.State;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;

public class Mindustry extends ModuleCore {
	public static String[] args = {};
	
	@Override
	public void init(){
		add(Vars.control = new Control());
		add(Vars.renderer = new Renderer());
		add(Vars.ui = new UI());
	}
	
	@Override
	public void postInit(){
		Vars.control.reset();
	}
	
	@Override
	public void render(){
		
		try{
			super.render();
		}catch (RuntimeException e){
			//TODO
			//Gdx.app.getClipboard().setContents(e.getMessage());
			throw e;
		}
		
		if(!GameState.is(State.paused)){
			Inputs.update();
			Timers.update();
		}
	}
}
