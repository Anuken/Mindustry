package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.EventType.GameLoadEvent;
import io.anuke.mindustry.io.BundleLoader;

import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ApplicationCore{

    @Override
    public void setup(){
        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
        });

        Time.mark();

        Vars.init();

        Log.setUseColors(false);
        BundleLoader.load();
        content.load();
        content.loadColors();

        add(logic = new Logic());
        add(world = new World());
        add(control = new Control());
        add(renderer = new Renderer());
        add(ui = new UI());
        add(netServer = new NetServer());
        add(netClient = new NetClient());
    }

    @Override
    public void init(){
        super.init();

        Log.info("Time to load [total]: {0}", Time.elapsed());
        Events.fire(new GameLoadEvent());
    }

    @Override
    public void update(){
        long lastFrameTime = Time.nanos();

        super.update();

        int fpsCap = Core.settings.getInt("fpscap", 125);

        if(fpsCap <= 120){
            long target = (1000 * 1000000) / fpsCap; //target in nanos
            long elapsed = Time.timeSinceNanos(lastFrameTime);
            if(elapsed < target){
                try{
                    Thread.sleep((target - elapsed) / 1000000, (int)((target - elapsed) % 1000000));
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

}
