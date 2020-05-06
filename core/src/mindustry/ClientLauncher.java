package mindustry;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.async.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.*;
import mindustry.maps.Map;
import mindustry.mod.*;
import mindustry.net.Net;
import mindustry.ui.*;

import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public abstract class ClientLauncher extends ApplicationCore implements Platform{
    private static final int loadingFPS = 20;

    private float smoothProgress;
    private long lastTime;
    private long beginTime;
    private boolean finished = false;
    private FloatArray floats = new FloatArray();

    @Override
    public void setup(){
        Events.fire(new ClientCreateEvent());

        Vars.loadLogger();
        Vars.loadFileLogger();
        Vars.platform = this;
        beginTime = Time.millis();

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

        Fonts.loadSystemCursors();

        assets.load(new Vars());

        Fonts.loadDefaultFont();

        assets.load(new AssetDescriptor<>("sprites/sprites.atlas", TextureAtlas.class)).loaded = t -> {
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
            drawLoading();
            if(false && assets.update(1000 / loadingFPS)){
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
            asyncLogic.begin();

            super.update();

            asyncLogic.end();
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

    float testprogress = 0f;
    static String[] properties = new String[4];
    static Color color = new Color(Pal.accent).lerp(Color.black, 0.5f);

    static{
        String red = "[#" + Color.scarlet.cpy().a(0.5f).toString() + "]";
        String orange = "[#" + color.toString() + "]";
        for(int i = 0; i < 4; i++){
            Properties props = System.getProperties();
            StringBuilder builder = new StringBuilder();
            for(Object key : props.keySet()){
                String str = (String)key;
                if(Mathf.chance(0.6)){
                    builder.append(orange);
                    if(Mathf.chance(0.2)) builder.append(red);

                    builder.append(str).append("::").append(props.get(str)).append("[]\n");
                    if(Mathf.chance(0.3)){
                        builder.append("\n");
                    }
                }

            }
            properties[i] = builder.toString();
        }
    }

    void drawLoading(){
        smoothProgress = Mathf.lerpDelta(smoothProgress, assets.getProgress(), 0.1f);

        Core.graphics.clear(Color.black);

        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight(), s = Scl.scl();
        Lines.precise(true);

        Draw.proj().setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

        int lightVerts = 20;
        float lightRad = Math.max(w, h)*0.6f;

        //light
        if(true){
            Fill.light(w/2, h/2, lightVerts, lightRad, Tmp.c1.set(Pal.accent).a(0.15f), Color.clear);
        }

        float space = Scl.scl(60);
        float progress = assets.getProgress();
        int dotw = (int)(w / space)/2 + 1;
        int doth = (int)(h / space)/2 + 1;

        //TODO remove
        if(true){
            testprogress += Time.delta() / (60f * 3);
            progress = testprogress;
            if(input.keyTap(KeyCode.space)){
                testprogress = 0;
            }
        }

        //dot matrix
        if(false){

            Draw.color(Pal.accent);

            Draw.alpha(0.3f);

            for(int cx = -dotw; cx <= dotw; cx++){
                for(int cy = -doth; cy <= doth; cy++){
                    float dx = cx * space + w/2f, dy = cy * space + h/2f;

                    Fill.square(dx, dy, 1.5f*s, 45);
                }
            }

            Draw.reset();
        }

        //square matrix
        if(true){

            Draw.color(Pal.accent);

            Draw.alpha(0.1f);
            Lines.stroke(s*3f);

            for(int cx = -dotw; cx <= dotw; cx++){
                for(int cy = -doth; cy <= doth; cy++){
                    float dx = cx * space + w/2f, dy = cy * space + h/2f;

                    Lines.poly(dx, dy, 4, space/2f);
                }
            }
        }

        //bars
        if(false){
            Draw.color(Pal.accent, Color.black, 0.7f);

            for(int cx = -dotw; cx <= dotw; cx++){
                float height = 400f * s * Mathf.randomSeed(cx);

                float dx = cx * space + w/2f, dy = 0;
                Lines.rect(dx - space/2f, dy, space, height, 1*s, 2*s);
            }

            Draw.reset();
        }

        //background text and indicator
        if(true){
            float rads = 110*s;
            float rad = Math.min(Math.min(w, h) / 3.1f, Math.min(w, h)/2f - rads);
            float rad2 = rad + rads;
            float epad = 60f * s;
            float mpad = 100f*s;

            Draw.color(color);
            Lines.stroke(2f * s);

            Lines.poly(w/2, h/2, 4, rad);
            Lines.poly(w/2, h/2, 4, rad2);

            int propi = 0;

            for(int sx : Mathf.signs){
                for(int sy : Mathf.signs){
                    float y1 = h/2f + sy*rad2, y2 = h/2f + sy*120f;
                    //Lines.beginLine();
                    floats.clear();

                    if(w > h){ //non-portrait
                        floats.add(w/2f + sx*mpad, y1);
                        floats.add(w/2f + (w/2f-epad)*sx, y1);
                        floats.add(w/2f + (w/2f-epad)*sx, y2);
                        floats.add(w/2f + sx*mpad + sx*Math.abs(y2-y1), y2);
                    }else{ //portrait
                        floats.add(w/2f + sx*mpad, y1);
                        floats.add(w/2f + sx*mpad, h/2f + (h/2f-epad)*sy);
                        floats.add(w/2f + sx*mpad + sx*Math.abs(y2-y1), h/2f + (h/2f-epad)*sy);
                        floats.add(w/2f + sx*mpad + sx*Math.abs(y2-y1), y2);
                    }

                    float minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, maxx = 0, maxy = 0;
                    for(int i = 0; i < floats.size; i+= 2){
                        float x = floats.items[i], y = floats.items[i + 1];
                        minx = Math.min(x, minx);
                        miny = Math.min(y, miny);

                        maxx = Math.max(x, maxx);
                        maxy = Math.max(y, maxy);
                    }

                    Lines.polyline(floats, true);

                    Draw.flush();
                    Gl.clear(Gl.stencilBufferBit);
                    Gl.stencilMask(0xFF);
                    Gl.colorMask(false, false, false, false);
                    Gl.enable(Gl.stencilTest);
                    Gl.stencilFunc(Gl.always, 1, 0xFF);
                    Gl.stencilMask(0xFF);
                    Gl.stencilOp(Gl.replace, Gl.replace, Gl.replace);

                    Fill.poly(floats);

                    Draw.flush();

                    Gl.stencilOp(Gl.keep, Gl.keep, Gl.keep);
                    Gl.colorMask(true, true, true, true);
                    Gl.stencilFunc(Gl.equal, 1, 0xFF);

                    if(assets.isLoaded("tech")){
                        BitmapFont font = assets.get("tech");
                        font.getData().markupEnabled = true;

                        font.draw(properties[propi++], minx, maxy);
                    }else{
                        Core.assets.finishLoadingAsset("tech");
                    }



                    Draw.flush();
                    Gl.disable(Gl.stencilTest);

                }
            }
        }

        //middle display
        if(true){
            float bspace = s * 100f;
            float bsize = s * 80f;
            int bars = (int)(w / bspace / 2) + 1;
            float pscale = 1f / bars;
            float barScale = 1.5f;

            Draw.color(Color.black);
            Fill.rect(w/2, h/2, w, bsize * barScale);
            Lines.stroke(1f*s);
            Draw.color(color);
            Lines.rect(0, h/2 - bsize * barScale/2f, w, bsize * barScale, 10, 0);

            for(int i = 1; i < bars; i++){
                float cx = i * bspace;
                float fract = 1f - (i - 1) / (float)(bars - 1);
                float alpha = progress >= fract ? 1f : Mathf.clamp((pscale - (fract - progress)) / pscale);
                Draw.color(Color.black, color, alpha);

                for(int dir : Mathf.signs){
                    float width = bsize/1.7f;
                    float skew = bsize/2f;

                    Fill.rects(w/2 + cx*dir - width/2f + dir*skew, h/2f - bsize/2f + bsize/2f, width, bsize/2f, -dir*skew);
                    Fill.rects(w/2 + cx*dir - width/2f, h/2f - bsize/2f, width, bsize/2f, dir*skew);
                    //Lines.poly(w/2 + cx*dir, h/2f, 3, bsize, 90 + dir*90);
                }

            }
        }


        if(assets.isLoaded("tech")){
            BitmapFont font = assets.get("tech");
            font.setColor(Pal.accent);
            Draw.color(Color.black);
            font.draw(System.getProperty("java.version") + "\n\n[scarlet][[ready]", w/2f, h/2f + 120, Align.center);
        }else{

        }

        /*

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
                String key = name.contains("script") ? "scripts" : name.contains("content") ? "content" : name.contains("mod") ? "mods" : name.contains("msav") ||
                    name.contains("maps") ? "map" : name.contains("ogg") || name.contains("mp3") ? "sound" : name.contains("png") ? "image" : "system";
                font.draw(bundle.get("load." + key, ""), graphics.getWidth() / 2f, graphics.getHeight() / 2f - height / 2f - Scl.scl(10f), Align.center);
            }
        }
         */
        Lines.precise(false);
        Draw.flush();
    }
}
