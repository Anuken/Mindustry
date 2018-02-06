package io.anuke.mindustry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.io.BlockLoader;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.mindustry.io.Platform;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.modules.ModuleCore;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ModuleCore {
	boolean multithread = true;
	Thread thread;
	float delta = 1f;

	@Override
	public void init(){
		debug = Platform.instance.isDebug();

		Log.setUseColors(false);
		BundleLoader.load();
		BlockLoader.load();


		logic = new Logic();

		if(!multithread) module(logic);

		module(world = new World());
		module(control = new Control());
		module(renderer = new Renderer());
		module(ui = new UI());
		module(netServer = new NetServer());
		module(netClient = new NetClient());
		module(netCommon = new NetCommon());

		Timers.setDeltaProvider(() ->
				Math.min(Thread.currentThread() == thread ? delta : Gdx.graphics.getDeltaTime()*60f, 20f)
		);

		if(multithread) {

			logic.init();

			thread = new Thread(() -> {
				try {
					while (true) {
						long time = TimeUtils.millis();
						logic.update();
						long elapsed = TimeUtils.timeSinceMillis(time);
						long target = (long) (1000 / 60f);

						delta = Math.max(elapsed, target) / 1000f * 60f;

						if (elapsed < target) {
							Thread.sleep(target - elapsed);
						}
					}
				} catch (Exception ex) {
					Gdx.app.postRunnable(() -> {
						throw new RuntimeException(ex);
					});
				}
			});
			thread.setDaemon(true);
			thread.setName("Update Thread");
			thread.start();
		}
	}

	public void render(){
		super.render();

		try {
			//Thread.sleep(40);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

}
