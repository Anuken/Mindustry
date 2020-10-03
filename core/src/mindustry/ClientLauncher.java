package mindustry;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.async.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.*;
import mindustry.mod.*;
import mindustry.net.Net;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public abstract class ClientLauncher extends ApplicationCore implements Platform{
    private static final int loadingFPS = 20;

    private long lastTime;
    private long beginTime;
    private boolean finished = false;
    private LoadRenderer loader;

    @Override
    public void setup(){
        loadLogger();

        loader = new LoadRenderer();
        Events.fire(new ClientCreateEvent());

        loadFileLogger();
        platform = this;
        beginTime = Time.millis();

        //debug GL information
        Log.info("[GL] Version: @", graphics.getGLVersion());
        Log.info("[GL] Max texture size: @", Gl.getInt(Gl.maxTextureSize));
        Log.info("[GL] Using @ context.", gl30 != null ? "OpenGL 3" : "OpenGL 2");
        Log.info("[JAVA] Version: @", System.getProperty("java.version"));

        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
        });

        batch = new SortedSpriteBatch();
        assets = new AssetManager();
        assets.setLoader(Texture.class, "." + mapExtension, new MapPreviewLoader());

        tree = new FileTree();
        assets.setLoader(Sound.class, new SoundLoader(tree));
        assets.setLoader(Music.class, new MusicLoader(tree));

        assets.load("sprites/error.png", Texture.class);
        atlas = TextureAtlas.blankAtlas();
        Vars.net = new Net(platform.getNet());
        mods = new Mods();
        schematics = new Schematics();

        Fonts.loadSystemCursors();

        assets.load(new Vars());

        Fonts.loadDefaultFont();

        //load fallback atlas if max texture size is below 4096
        assets.load(new AssetDescriptor<>(Gl.getInt(Gl.maxTextureSize) >= 4096 ? "sprites/sprites.atlas" : "sprites/fallback/sprites.atlas", TextureAtlas.class)).loaded = t -> {
            atlas = (TextureAtlas)t;
            Fonts.mergeFontAtlas(atlas);
        };

        assets.loadRun("maps", Map.class, () -> maps.loadPreviews());

        Musics.load();
        Sounds.load();

        assets.loadRun("contentcreate", Content.class, () -> {
            content.createBaseContent();
            content.loadColors();
        }, () -> {
            mods.loadScripts();
            content.createModContent();
        });

        add(logic = new Logic());
        add(control = new Control());
        add(renderer = new Renderer());
        add(ui = new UI());
        add(netServer = new NetServer());
        add(netClient = new NetClient());

        assets.load(mods);
        assets.load(schematics);

        assets.loadRun("contentinit", ContentLoader.class, () -> content.init(), () -> content.load());
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

        if(!finished){
            Draw.proj().setOrtho(0, 0, width, height);
        }else{
            super.resize(width, height);
        }
    }

    @Override
    public void update(){
        if(!finished){
            if(loader != null){
                loader.draw();
            }
            if(assets.update(1000 / loadingFPS)){
                loader.dispose();
                loader = null;
                Log.info("Total time to load: @", Time.timeSinceMillis(beginTime));
                for(ApplicationListener listener : modules){
                    listener.init();
                }
                mods.eachClass(Mod::init);
                finished = true;
                Events.fire(new ClientLoadEvent());
                super.resize(graphics.getWidth(), graphics.getHeight());
                app.post(() -> app.post(() -> app.post(() -> app.post(() -> super.resize(graphics.getWidth(), graphics.getHeight())))));
            }
        }else{
            asyncCore.begin();

            super.update();

            asyncCore.end();
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
}
