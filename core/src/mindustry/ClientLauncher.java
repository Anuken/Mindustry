package mindustry;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.audio.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.*;
import mindustry.mod.*;
import mindustry.net.*;
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
        String dataDir = OS.env("MINDUSTRY_DATA_DIR");
        if(dataDir != null){
            Core.settings.setDataDirectory(files.absolute(dataDir));
        }

        checkLaunch();
        loadLogger();

        loader = new LoadRenderer();
        Events.fire(new ClientCreateEvent());

        loadFileLogger();
        platform = this;
        maxTextureSize = Gl.getInt(Gl.maxTextureSize);
        beginTime = Time.millis();

        //debug GL information
        Log.info("[GL] Version: @", graphics.getGLVersion());
        Log.info("[GL] Max texture size: @", maxTextureSize);
        Log.info("[GL] Using @ context.", gl30 != null ? "OpenGL 3" : "OpenGL 2");
        if(maxTextureSize < 4096) Log.warn("[GL] Your maximum texture size is below the recommended minimum of 4096. This will cause severe performance issues.");
        Log.info("[JAVA] Version: @", OS.javaVersion);
        long ram = Runtime.getRuntime().maxMemory();
        boolean gb = ram >= 1024 * 1024 * 1024;
        Log.info("[RAM] Available: @ @", Strings.fixed(gb ? ram / 1024f / 1024 / 1024f : ram / 1024f / 1024f, 1), gb ? "GB" : "MB");

        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, 60f / 10f);
        });

        batch = new SortedSpriteBatch();
        assets = new AssetManager();
        assets.setLoader(Texture.class, "." + mapExtension, new MapPreviewLoader());

        tree = new FileTree();
        assets.setLoader(Sound.class, new SoundLoader(tree){

            @Override
            public void loadAsync(AssetManager manager, String fileName, Fi file, SoundParameter parameter){

            }

            @Override
            public Sound loadSync(AssetManager manager, String fileName, Fi file, SoundParameter parameter){
                if(parameter != null && parameter.sound != null){
                    mainExecutor.submit(() -> parameter.sound.load(file));

                    return parameter.sound;
                }else{
                    Sound sound = new Sound();

                    mainExecutor.submit(() -> {
                        try{
                            sound.load(file);
                        }catch(Throwable t){
                            Log.err("Error loading sound: " + file, t);
                        }
                    });

                    return sound;
                }
            }
        });
        assets.setLoader(Music.class, new MusicLoader(tree){
            @Override
            public void loadAsync(AssetManager manager, String fileName, Fi file, MusicParameter parameter){

            }

            @Override
            public Music loadSync(AssetManager manager, String fileName, Fi file, MusicParameter parameter){

                if(parameter != null && parameter.music != null){
                    mainExecutor.submit(() -> {
                        try{
                            parameter.music.load(file);
                        }catch(Throwable t){
                            Log.err("Error loading music: " + file, t);
                        }
                    });

                    return parameter.music;
                }else{
                    Music music = new Music();

                    mainExecutor.submit(() -> {
                        try{
                            music.load(file);
                        }catch(Throwable t){
                            Log.err("Error loading music: " + file, t);
                        }
                    });

                    return music;
                }
            }
        });

        assets.load("sprites/error.png", Texture.class);
        atlas = TextureAtlas.blankAtlas();
        Vars.net = new Net(platform.getNet());
        MapPreviewLoader.setupLoaders();
        mods = new Mods();
        schematics = new Schematics();

        Fonts.loadSystemCursors();

        assets.load(new Vars());

        Fonts.loadDefaultFont();

        //load fallback atlas if max texture size is below 4096
        assets.load(new AssetDescriptor<>(maxTextureSize >= 4096 ? "sprites/sprites.aatls" : "sprites/fallback/sprites.aatls", TextureAtlas.class)).loaded = t -> atlas = t;
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

        assets.load(mods);
        assets.loadRun("mergeUI", PixmapPacker.class, () -> {}, () -> Fonts.mergeFontAtlas(atlas));

        add(logic = new Logic());
        add(control = new Control());
        add(renderer = new Renderer());
        add(ui = new UI());
        add(netServer = new NetServer());
        add(netClient = new NetClient());

        assets.load(schematics);

        assets.loadRun("contentinit", ContentLoader.class, () -> content.init(), () -> content.load());
        assets.loadRun("baseparts", BaseRegistry.class, () -> {}, () -> bases.load());
    }

    @Override
    public void add(ApplicationListener module){
        super.add(module);

        //autoload modules when necessary
        if(module instanceof Loadable l){
            assets.load(l);
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
                Log.info("Total time to load: @ms", Time.timeSinceMillis(beginTime));
                for(ApplicationListener listener : modules){
                    listener.init();
                }
                mods.eachClass(Mod::init);
                finished = true;
                Events.fire(new ClientLoadEvent());
                clientLoaded = true;
                super.resize(graphics.getWidth(), graphics.getHeight());
                app.post(() -> app.post(() -> app.post(() -> app.post(() -> {
                    super.resize(graphics.getWidth(), graphics.getHeight());

                    //mark initialization as complete
                    finishLaunch();
                }))));
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
    public void exit(){
        //on graceful exit, finish the launch normally.
        Vars.finishLaunch();
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
        //when the user tabs out on mobile, the exit() event doesn't fire reliably - in that case, just assume they're about to kill the app
        //this isn't 100% reliable but it should work for most cases
        if(mobile){
            Vars.finishLaunch();
        }
        if(finished){
            super.pause();
        }
    }
}
