package io.anuke.mindustry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.PlatformFunction;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.BlockLoader;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;

import java.util.Locale;

public class Mindustry extends ModuleCore {
	public static boolean hasDiscord = true;
	public static Array<String> args = new Array<>();
	public static PlatformFunction platforms = new PlatformFunction(){};
	public static boolean externalBundle = false;
	
	@Override
	public void init(){
		Settings.defaults("locale", "default");
		Settings.load("io.anuke.moment");
		loadBundle();
		BlockLoader.load();

		module(Vars.world = new World());
		module(Vars.control = new Control());
		module(Vars.logic = new Logic());
		module(Vars.renderer = new Renderer());
		module(Vars.ui = new UI());
		module(Vars.netServer = new NetServer());
		module(Vars.netClient = new NetClient());
	}

	@Override
	public void dispose() {
		GameState.set(State.menu);
		platforms.onGameExit();
		Net.dispose();
		super.dispose();
	}

	public Locale getLocale(){
		String loc = Settings.getString("locale");
		if(loc.equals("default")){
			return Locale.getDefault();
		}else{
			Locale lastLocale;
			if (loc.contains("_")) {
				String[] split = loc.split("_");
				lastLocale = new Locale(split[0], split[1]);
			} else {
				lastLocale = new Locale(loc);
			}

			return lastLocale;
		}
	}

	public void loadBundle(){
		I18NBundle.setExceptionOnMissingKey(false);

		if(externalBundle){
			try {
				FileHandle handle = Gdx.files.local("bundle");

				Locale locale = Locale.ENGLISH;
				Core.bundle = I18NBundle.createBundle(handle, locale);
			}catch (Exception e){
				UCore.error(e);
				platforms.showError("Failed to find bundle!\nMake sure you have bundle.properties in the same directory\nas the jar file.\n\nIf the problem persists, try running it through the command prompt:\n" +
						"Hold left-shift, then right click and select 'open command prompt here'.\nThen, type in 'java -jar mindustry.jar' without quotes.");
				Gdx.app.exit();
			}
		}else{
			FileHandle handle = Gdx.files.internal("bundles/bundle");

			Locale locale = getLocale();
			UCore.log("Got locale: " + locale);
			Core.bundle = I18NBundle.createBundle(handle, locale);
		}
	}
	
	@Override
	public void postInit(){
		Vars.control.reset();
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
