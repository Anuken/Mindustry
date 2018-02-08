package io.anuke.mindustry;

import io.anuke.mindustry.core.*;
import io.anuke.mindustry.io.BlockLoader;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.mindustry.io.Platform;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ModuleCore {

	@Override
	public void init(){
		debug = Platform.instance.isDebug();

		Log.setUseColors(false);
		BundleLoader.load();
		BlockLoader.load();

		module(logic = new Logic());
		module(world = new World());
		module(control = new Control());
		module(renderer = new Renderer());
		module(ui = new UI());
		module(netServer = new NetServer());
		module(netClient = new NetClient());
		module(netCommon = new NetCommon());
	}

	@Override
	public void render(){
		super.render();
		threads.handleRender();
	}

}
