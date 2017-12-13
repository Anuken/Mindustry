package io.anuke.mindustry;

import java.util.Date;

import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.core.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.Formatter;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.util.Profiler;

public class Mindustry extends ModuleCore {
	public static Callable donationsCallable;
	public static Array<String> args = new Array<>();
	public static Formatter formatter = new Formatter(){
		@Override public String format(Date date){ return "invalid date"; }
		@Override public String format(int number){ return number + ""; }
	};
	
	//always initialize blocks in this order, otherwise there are ID errors
	public Block[] blockClasses = {
		Blocks.air,
		DefenseBlocks.compositewall,
		DistributionBlocks.conduit,
		ProductionBlocks.coaldrill,
		WeaponBlocks.chainturret
	};
	
	@Override
	public void init(){
		module(Vars.world = new World());
		module(Vars.control = new Control());
		module(Vars.renderer = new Renderer());
		module(Vars.ui = new UI());
	}
	
	@Override
	public void postInit(){
		if(Vars.debug && Vars.debugGL){
			GLProfiler.enable();
		}
		Vars.control.reset();
	}
	
	@Override
	public void render(){
		Profiler.begin("total");
		
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
		
		Profiler.end("total");
	}
}
