package io.anuke.mindustry;

import java.util.Date;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.scene.ui.TextField;

public class Mindustry extends ModuleCore {
	public static Callable donationsCallable;
	public static boolean hasDiscord = true;
	public static Array<String> args = new Array<>();
	public static PlatformFunction platforms = new PlatformFunction(){
		@Override public String format(Date date){ return "invalid date"; }
		@Override public String format(int number){ return number + ""; }
		@Override public void openLink(String link){ }
		@Override public void addDialog(TextField field){}
		@Override public void onSceneChange(String state, String details, String icon) {}
		@Override public void onGameExit() { }
	};

	public static boolean externalBundle = false;
	
	@Override
	public void init(){
		loadBundle();

		module(Vars.world = new World());
		module(Vars.control = new Control());
		module(Vars.renderer = new Renderer());
		module(Vars.ui = new UI());
		module(Vars.netServer = new NetServer());
		module(Vars.netClient = new NetClient());
	}

	@Override
	public void dispose() {
		platforms.onGameExit();
		super.dispose();
	}

	public void loadBundle(){
		I18NBundle.setExceptionOnMissingKey(false);

		if(externalBundle){
			FileHandle handle = Gdx.files.local("bundle");

			Locale locale = Locale.ENGLISH;
			Core.bundle = I18NBundle.createBundle(handle, locale);
		}else{
			FileHandle handle = Gdx.files.internal("bundles/bundle");

			Locale locale = Locale.getDefault();
			Core.bundle = I18NBundle.createBundle(handle, locale);
		}


		//always initialize blocks in this order, otherwise there are ID errors
		Block[] blockClasses = {
				Blocks.air,
				DefenseBlocks.compositewall,
				DistributionBlocks.conduit,
				ProductionBlocks.coaldrill,
				WeaponBlocks.chainturret,
				SpecialBlocks.enemySpawn
		};

		UCore.log("Block classes: " + blockClasses.length);
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
