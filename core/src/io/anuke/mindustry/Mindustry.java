package io.anuke.mindustry;

import java.util.Date;

import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.GameState.State;
import io.anuke.mindustry.io.Formatter;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;

public class Mindustry extends ModuleCore {
	public static Array<String> args = new Array<>();
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
		//if(Vars.debug){
		GLProfiler.enable();
		//}
		//always initialize blocks in this order, otherwise there are ID errors
		Blocks.dirt.getClass();
		ProductionBlocks.coaldrill.getClass();
		WeaponBlocks.turret.getClass();
		
		
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
			//TODO display error log
			//Gdx.app.getClipboard().setContents(e.getMessage());
			throw e;
		}
		
		if(!GameState.is(State.paused)){
			Timers.update();
		}
		
		Inputs.update();
	}
}
