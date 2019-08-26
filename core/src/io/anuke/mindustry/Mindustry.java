package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.io.*;

import static io.anuke.arc.Core.*;
import static io.anuke.mindustry.Vars.*;

public class Mindustry extends ApplicationCore{
    private long lastTime;
    private boolean finished = false;

    @Override
    public void setup(){
        Log.setUseColors(false);

        Bench.begin("load setup");
        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
        });

        batch = new SpriteBatch();
        assets = new AssetManager();
        atlas = TextureAtlas.blankAtlas();

        assets.load(new Vars());

        Bench.begin("cursors");
        UI.loadSystemCursors();

        Bench.begin("vars");
        Bench.begin("bundle");
        BundleLoader.load();

        Bench.begin("music");
        Musics.loadBegin();
        Bench.begin("sound");
        Sounds.loadBegin();
    }

    private void post(){
        Bench.begin("content");
        content.load();
        content.loadColors();

        Bench.begin("logic");
        add(logic = new Logic());
        Bench.begin("world");
        add(world = new World());
        Bench.begin("control");
        add(control = new Control());
        Bench.begin("renderer");
        add(renderer = new Renderer());
        Bench.begin("ui");
        add(ui = new UI());
        Bench.begin("net");
        add(netServer = new NetServer());
        add(netClient = new NetClient());
        Bench.begin("init");
    }

    @Override
    public void update(){
        //
        if(!assets.update()){
            drawLoading();
        }else{
            if(!finished){
                post();
                finished = true;
                Events.fire(new ClientLoadEvent());
            }

            super.update();
        }

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
        Bench.end();
    }

    void drawLoading(){
        Core.graphics.clear(Color.BLACK);
        Draw.proj().setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        float height = UnitScl.dp.scl(100f);

        Draw.color(Pal.darkerGray);
        Fill.rect(graphics.getWidth()/2f, graphics.getHeight()/2f, graphics.getWidth(), height);
        Draw.color(Pal.accent);
        Fill.crect(0, graphics.getHeight()/2f - height/2f, graphics.getWidth() * assets.getProgress(), height);
        Draw.flush();
    }
}
