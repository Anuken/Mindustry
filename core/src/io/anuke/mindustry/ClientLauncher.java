package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.assets.loaders.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.async.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.ctype.Content;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.mod.*;
import io.anuke.mindustry.net.Net;

import static io.anuke.arc.Core.*;
import static io.anuke.mindustry.Vars.*;

public abstract class ClientLauncher extends ApplicationCore implements Platform{
    private static final int loadingFPS = 20;

    private float smoothProgress;
    private long lastTime;
    private long beginTime;
    private boolean finished = false;

    @Override
    public void setup(){
        Vars.platform = this;
        Log.setUseColors(false);
        beginTime = Time.millis();

        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
        });

        batch = new SpriteBatch();
        assets = new AssetManager();
        assets.setLoader(Texture.class, "." + mapExtension, new MapPreviewLoader());

        tree = new FileTree();
        assets.setLoader(Sound.class, new SoundLoader(tree));
        assets.setLoader(Music.class, new MusicLoader(tree));

        assets.load("sprites/error.png", Texture.class);
        atlas = TextureAtlas.blankAtlas();
        Vars.net = new Net(platform.getNet());
        mods = new Mods();

        UI.loadSystemCursors();

        assets.load(new Vars());

        UI.loadDefaultFont();

        assets.load(new AssetDescriptor<>("sprites/sprites.atlas", TextureAtlas.class)).loaded = t -> {
            atlas = (TextureAtlas)t;
        };

        assets.loadRun("maps", Map.class, () -> maps.loadPreviews());

        Musics.load();
        Sounds.load();

        assets.loadRun("contentcreate", Content.class, () -> {
            content.createContent();
            content.loadColors();
        });

        add(logic = new Logic());
        add(control = new Control());
        add(renderer = new Renderer());
        add(ui = new UI());
        add(netServer = new NetServer());
        add(netClient = new NetClient());

        assets.load(mods);
        assets.load(schematics);

        assets.loadRun("contentinit", ContentLoader.class, () -> {
            content.init();
            content.load();
        });
    }

    @Override
    public void add(ApplicationListener module){
        super.add(module);

        //autoload modules when necessary
        if(module instanceof Loadable){
            assets.load((Loadable)module);
        }
    }

    @Override
    public void resize(int width, int height){
        if(assets == null) return;

        if(!assets.isFinished()){
            Draw.proj().setOrtho(0, 0, width, height);
        }else{
            super.resize(width, height);
        }
    }

    @Override
    public void update(){
        if(!finished){
            drawLoading();
            if(assets.update(1000 / loadingFPS)){
                Log.info("Total time to load: {0}", Time.timeSinceMillis(beginTime));
                for(ApplicationListener listener : modules){
                    listener.init();
                }
                mods.each(Mod::init);
                finished = true;
                Events.fire(new ClientLoadEvent());
                super.resize(graphics.getWidth(), graphics.getHeight());
                app.post(() -> app.post(() -> app.post(() -> app.post(() -> super.resize(graphics.getWidth(), graphics.getHeight())))));
            }
        }else{
            super.update();
        }

        int targetfps = Core.settings.getInt("fpscap", 120);

        if(targetfps > 0 && targetfps <= 240){
            long target = (1000 * 1000000) / targetfps; //target in nanos
            long elapsed = Time.timeSinceNanos(lastTime);
            if(elapsed < target){
                Threads.sleep((target - elapsed) / 1000000, (int)((target - elapsed) % 1000000));
            }
        }

        lastTime = Time.nanos();
    }

    @Override
    public void init(){
        setup();
    }

    @Override
    public void resume(){
        if(finished){
            super.resume();
        }
    }

    @Override
    public void pause(){
        if(finished){
            super.pause();
        }
    }

    void drawLoading(){
        smoothProgress = Mathf.lerpDelta(smoothProgress, assets.getProgress(), 0.1f);

        Core.graphics.clear(Pal.darkerGray);
        Draw.proj().setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

        float height = Scl.scl(50f);

        Draw.color(Color.black);
        Fill.poly(graphics.getWidth()/2f, graphics.getHeight()/2f, 6, Mathf.dst(graphics.getWidth()/2f, graphics.getHeight()/2f) * smoothProgress);
        Draw.reset();

        float w = graphics.getWidth()*0.6f;

        Draw.color(Color.black);
        Fill.rect(graphics.getWidth()/2f, graphics.getHeight()/2f, w, height);

        Draw.color(Pal.accent);
        Fill.crect(graphics.getWidth()/2f-w/2f, graphics.getHeight()/2f - height/2f, w * smoothProgress, height);

        for(int i : Mathf.signs){
            Fill.tri(graphics.getWidth()/2f + w/2f*i, graphics.getHeight()/2f + height/2f, graphics.getWidth()/2f + w/2f*i, graphics.getHeight()/2f - height/2f, graphics.getWidth()/2f + w/2f*i + height/2f*i, graphics.getHeight()/2f);
        }

        if(assets.isLoaded("outline")){
            BitmapFont font = assets.get("outline");
            font.draw((int)(assets.getProgress() * 100) + "%", graphics.getWidth() / 2f, graphics.getHeight() / 2f + Scl.scl(10f), Align.center);
            font.draw(bundle.get("loading", "").replace("[accent]", ""), graphics.getWidth() / 2f, graphics.getHeight() / 2f + height / 2f + Scl.scl(20), Align.center);

            if(assets.getCurrentLoading() != null){
                String name = assets.getCurrentLoading().fileName.toLowerCase();
                String key = name.contains("content") ? "content" : name.contains("mod") ? "mods" : name.contains("msav") || name.contains("maps") ? "map" : name.contains("ogg") || name.contains("mp3") ? "sound" : name.contains("png") ? "image" : "system";
                font.draw(bundle.get("load." + key, ""), graphics.getWidth() / 2f, graphics.getHeight() / 2f - height / 2f - Scl.scl(10f), Align.center);
            }
        }
        Draw.flush();
    }
}
