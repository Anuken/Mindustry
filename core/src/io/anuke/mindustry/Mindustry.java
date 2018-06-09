package io.anuke.mindustry;

import io.anuke.mindustry.core.*;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ModuleCore {

	@Override
	public void init(){
		Vars.init();

		debug = Platform.instance.isDebug();

		Timers.mark();

		Log.setUseColors(false);
		BundleLoader.load();
		ContentLoader.load();

		Log.info("Time to load content: {0}", Timers.elapsed());

		module(logic = new Logic());
		module(world = new World());
		module(control = new Control());
		module(renderer = new Renderer());
		module(ui = new UI());
		module(netServer = new NetServer());
		module(netClient = new NetClient());
	}

	@Override
	public void render(){
		super.render();
		threads.handleRender();
	}

}
