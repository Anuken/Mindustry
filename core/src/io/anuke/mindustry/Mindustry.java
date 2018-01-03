package io.anuke.mindustry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.BlockLoader;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.scene.ui.TextField;

import java.util.Date;
import java.util.Locale;

public class Mindustry extends ModuleCore {
	public static boolean hasDiscord = true;
	public static Array<String> args = new Array<>();
	public static PlatformFunction platforms = new PlatformFunction(){
		@Override public String format(Date date){ return "invalid date"; }
		@Override public String format(int number){ return number + ""; }
		@Override public void openLink(String link){ }
		@Override public void addDialog(TextField field){}
		@Override public void onSceneChange(String state, String details, String icon) {}
		@Override public void onGameExit() {}
		@Override public void openDonations() {}
		@Override public void requestWritePerms() {}
	};
	public static OrderedMap<String, Integer> idMap = new OrderedMap<>();

	public static boolean externalBundle = false;
	
	@Override
	public void init(){
		loadBundle();
		BlockLoader.load();

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
	}
	
	@Override
	public void postInit(){
		Vars.control.reset();
		Vars.control.getSaves().convertSaves();
	}
	
	@Override
	public void render(){
        super.render();
		
		if(!GameState.is(State.paused) || Net.active()){
			Timers.update();
		}
		
		Inputs.update();
	}
}
