package io.anuke.mindustry;

import java.util.Date;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.io.Formatter;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;

public class Mindustry extends ModuleCore {
	public static String[] args = {};
	public static Formatter formatter = new Formatter(){

		@Override
		public String format(Date date){
			return "invalid date";
		}

		@Override
		public String format(int number){
			return number + "";
		}
		
	};
	
	@Override
	public void init(){
		module(Vars.control = new Control());
		module(Vars.renderer = new Renderer());
		module(Vars.ui = new UI());
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
			Timers.update();
		}
		
		Inputs.update();
	}
}
