package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.io.*;

import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ApplicationCore{
    private long lastTime;

    @Override
    public void setup(){
        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
        });

        Time.mark();
        UI.loadSystemCursors();

        Vars.init();
        Log.setUseColors(false);
        BundleLoader.load();
        Musics.load();
        Sounds.load();

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
    public void update(){
        super.update();

        int targetfps = Core.settings.getInt("fpscap", 120);

        if(targetfps > 0 && targetfps <= 240){
            long target = (1000 * 1000000) / targetfps; //target in nanos
            long elapsed = Time.timeSinceNanos(lastTime);
            if(elapsed < target){
                try{
                    Thread.sleep((target - elapsed) / 1000000, (int)((target - elapsed) % 1000000));
                }catch(InterruptedException ignored){
                    //ignore
                }
            }
        }

        lastTime = Time.nanos();
    }

    @Override
    public void init(){
        super.init();

        Log.info("Time to load [total]: {0}", Time.elapsed());
        Events.fire(new ClientLoadEvent());
    }
}
