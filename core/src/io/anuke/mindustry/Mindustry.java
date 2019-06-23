package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.SpriteBatch;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.EventType.GameLoadEvent;
import io.anuke.mindustry.io.BundleLoader;

import static io.anuke.arc.Core.batch;
import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ApplicationCore{

    @Override
    public void setup(){
        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
        });

        Time.mark();

        batch = new SpriteBatch();

        Core.app.post(() -> Core.app.post(() -> {
            drawLoading();
            Core.app.post(() -> Core.app.post(() -> {
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

                for(ApplicationListener listener : modules){
                    listener.init();
                }

                Log.info("Time to load [total]: {0}", Time.elapsed());
                Events.fire(new GameLoadEvent());
            }));
        }));
    }

    @Override
    public void init(){
        setup();
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

    void drawLoading(){
        Core.graphics.clear(Color.BLACK);
        Draw.proj().setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

        Texture icon = new Texture("sprites/logotext.png");
        Draw.rect(Draw.wrap(icon), Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
        Draw.flush();

        icon.dispose();
    }

}
