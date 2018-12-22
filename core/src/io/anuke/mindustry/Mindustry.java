package io.anuke.mindustry;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.EventType.GameLoadEvent;
import io.anuke.mindustry.io.BundleLoader;

import static io.anuke.mindustry.Vars.*;

public class Mindustry implements ApplicationListener{
    private long lastFrameTime;

    public Mindustry(){
        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return Float.isNaN(result) || Float.isInfinite(result) ? 1f : Math.min(result, 60f / 10f);
        });

        Time.mark();

        Vars.init();

        Log.setUseColors(false);
        BundleLoader.load();
        content.load();

        Core.app.addListener(logic = new Logic());
        Core.app.addListener(world = new World());
        Core.app.addListener(control = new Control());
        Core.app.addListener(renderer = new Renderer());
        Core.app.addListener(ui = new UI());
        Core.app.addListener(netServer = new NetServer());
        Core.app.addListener(netClient = new NetClient());
    }

    @Override
    public void init(){
        Log.info("Time to load [total]: {0}", Time.elapsed());
        Events.fire(new GameLoadEvent());
    }

    @Override
    public void update(){
        lastFrameTime = Time.millis();

        //TODO ??render it all??

        int fpsCap = Core.settings.getInt("fpscap", 125);

        if(fpsCap <= 120){
            long target = 1000/fpsCap;
            long elapsed = Time.timeSinceMillis(lastFrameTime);
            if(elapsed < target){
                try{
                    Thread.sleep(target - elapsed);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

}
