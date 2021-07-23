package mindustry.core;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.async.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class Renderer implements ApplicationListener{
    /** These are global variables, for headless access. Cached. */
    public static float laserOpacity = 0.5f, bridgeOpacity = 0.75f;

    public final BlockRenderer blocks = new BlockRenderer();
    public final MinimapRenderer minimap = new MinimapRenderer();
    public final OverlayRenderer overlays = new OverlayRenderer();
    public final LightRenderer lights = new LightRenderer();
    public final Pixelator pixelator = new Pixelator();
    public PlanetRenderer planets;

    public @Nullable Bloom bloom;
    public FrameBuffer effectBuffer = new FrameBuffer();
    public boolean animateShields, drawWeather = true, drawStatus;
    /** minZoom = zooming out, maxZoom = zooming in */
    public float minZoom = 1.5f, maxZoom = 6f;
    public Seq<EnvRenderer> envRenderers = new Seq<>();
    public TextureRegion[] bubbles = new TextureRegion[16], splashes = new TextureRegion[12];

    private @Nullable CoreBuild landCore;
    private Color clearColor = new Color(0f, 0f, 0f, 1f);
    private float targetscale = Scl.scl(4), camerascale = targetscale, landscale, landTime, weatherAlpha, minZoomScl = Scl.scl(0.01f);
    private float shakeIntensity, shaketime;
    private Vec2 camShakeOffset = new Vec2();

    public Renderer(){
        camera = new Camera();
        Shaders.init();
    }

    public void shake(float intensity, float duration){
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shaketime = Math.max(shaketime, duration);
    }

    public void addEnvRenderer(int mask, Runnable render){
        envRenderers.add(new EnvRenderer(mask, render));
    }

    @Override
    public void init(){
        planets = new PlanetRenderer();

        if(settings.getBool("bloom", !ios)){
            setupBloom();
        }

        Events.run(Trigger.newGame, () -> {
            landCore = player.bestCore();
        });

        EnvRenderers.init();
        for(int i = 0; i < bubbles.length; i++) bubbles[i] = atlas.find("bubble-" + i);
        for(int i = 0; i < splashes.length; i++) splashes[i] = atlas.find("splash-" + i);

        assets.load("sprites/clouds.png", Texture.class).loaded = t -> {
            ((Texture)t).setWrap(TextureWrap.repeat);
            ((Texture)t).setFilter(TextureFilter.linear);
        };
    }

    @Override
    public void update(){
        Color.white.set(1f, 1f, 1f, 1f);

        float dest = Mathf.round(targetscale, 0.5f);
        camerascale = Mathf.lerpDelta(camerascale, dest, 0.1f);
        if(Mathf.equal(camerascale, dest, 0.001f)) camerascale = dest;
        laserOpacity = settings.getInt("lasersopacity") / 100f;
        bridgeOpacity = settings.getInt("bridgeopacity") / 100f;
        animateShields = settings.getBool("animatedshields");
        drawStatus = Core.settings.getBool("blockstatus");

        if(landTime > 0){
            if(!state.isPaused()){
                landTime -= Time.delta;
            }
            landscale = Interp.pow5In.apply(minZoomScl, Scl.scl(4f), 1f - landTime / Fx.coreLand.lifetime);
            camerascale = landscale;
            weatherAlpha = 0f;
        }else{
            weatherAlpha = Mathf.lerpDelta(weatherAlpha, 1f, 0.08f);
        }

        camera.width = graphics.getWidth() / camerascale;
        camera.height = graphics.getHeight() / camerascale;

        if(state.isMenu()){
            landTime = 0f;
            graphics.clear(Color.black);
        }else{
            if(shaketime > 0){
                float intensity = shakeIntensity * (settings.getInt("screenshake", 4) / 4f) * 0.75f;
                camShakeOffset.setToRandomDirection().scl(Mathf.random(intensity));
                camera.position.add(camShakeOffset);
                shakeIntensity -= 0.25f * Time.delta;
                shaketime -= Time.delta;
                shakeIntensity = Mathf.clamp(shakeIntensity, 0f, 100f);
            }else{
                camShakeOffset.setZero();
                shakeIntensity = 0f;
            }

            if(pixelator.enabled()){
                pixelator.drawPixelate();
            }else{
                draw();
            }

            camera.position.sub(camShakeOffset);
        }
    }

    public boolean isLanding(){
        return landTime > 0;
    }

    public float weatherAlpha(){
        return weatherAlpha;
    }

    public float landScale(){
        return landTime > 0 ? landscale : 1f;
    }

    @Override
    public void dispose(){
        Events.fire(new DisposeEvent());
    }

    @Override
    public void resume(){
        if(settings.getBool("bloom") && bloom != null){
            bloom.resume();
        }
    }

    void setupBloom(){
        try{
            if(bloom != null){
                bloom.dispose();
                bloom = null;
            }
            bloom = new Bloom(true);
        }catch(Throwable e){
            settings.put("bloom", false);
            ui.showErrorMessage("@error.bloom");
            Log.err(e);
        }
    }

    public void toggleBloom(boolean enabled){
        if(enabled){
            if(bloom == null){
                setupBloom();
            }
        }else{
            if(bloom != null){
                bloom.dispose();
                bloom = null;
            }
        }
    }

    public void draw(){
        Events.fire(Trigger.preDraw);

        camera.update();

        if(Float.isNaN(camera.position.x) || Float.isNaN(camera.position.y)){
            camera.position.set(player);
        }

        graphics.clear(clearColor);
        Draw.reset();

        if(Core.settings.getBool("animatedwater") || animateShields){
            effectBuffer.resize(graphics.getWidth(), graphics.getHeight());
        }

        Draw.proj(camera);

        blocks.checkChanges();
        blocks.floor.checkChanges();
        blocks.processBlocks();

        Draw.sort(true);

        Events.fire(Trigger.draw);

        if(pixelator.enabled()){
            pixelator.register();
        }

        Draw.draw(Layer.background, this::drawBackground);
        Draw.draw(Layer.floor, blocks.floor::drawFloor);
        Draw.draw(Layer.block - 1, blocks::drawShadows);
        Draw.draw(Layer.block - 0.09f, () -> {
            blocks.floor.beginDraw();
            blocks.floor.drawLayer(CacheLayer.walls);
            blocks.floor.endDraw();
        });

        Draw.drawRange(Layer.blockBuilding, () -> Draw.shader(Shaders.blockbuild, true), Draw::shader);

        //render all matching environments
        for(var renderer : envRenderers){
            if((renderer.env & state.rules.environment) == renderer.env){
                renderer.renderer.run();
            }
        }

        if(state.rules.lighting){
            Draw.draw(Layer.light, lights::draw);
        }

        if(enableDarkness){
            Draw.draw(Layer.darkness, blocks::drawDarkness);
        }

        if(bloom != null){
            bloom.resize(graphics.getWidth() / 4, graphics.getHeight() / 4);
            Draw.draw(Layer.bullet - 0.02f, bloom::capture);
            Draw.draw(Layer.effect + 0.02f, bloom::render);
        }

        Draw.draw(Layer.plans, overlays::drawBottom);

        if(animateShields && Shaders.shield != null){
            Draw.drawRange(Layer.shields, 1f, () -> effectBuffer.begin(Color.clear), () -> {
                effectBuffer.end();
                effectBuffer.blit(Shaders.shield);
            });

            Draw.drawRange(Layer.buildBeam, 1f, () -> effectBuffer.begin(Color.clear), () -> {
                effectBuffer.end();
                effectBuffer.blit(Shaders.buildBeam);
            });
        }

        Draw.draw(Layer.overlayUI, overlays::drawTop);
        Draw.draw(Layer.space, this::drawLanding);

        blocks.drawBlocks();

        Groups.draw.draw(Drawc::draw);

        Draw.reset();
        Draw.flush();
        Draw.sort(false);

        Events.fire(Trigger.postDraw);
    }

    private void drawBackground(){

    }

    private void drawLanding(){
        CoreBuild entity = landCore == null ? player.bestCore() : landCore;
        //var clouds = assets.get("sprites/clouds.png", Texture.class);
        if(landTime > 0 && entity != null){
            float fout = landTime / Fx.coreLand.lifetime;

            //TODO clouds
            /*
            float scaling = 10000f;
            float sscl = 1f + fout*1.5f;
            float offset = -0.38f;

            Tmp.tr1.set(clouds);
            Tmp.tr1.set((camera.position.x - camera.width/2f * sscl) / scaling, (camera.position.y - camera.height/2f * sscl) / scaling, (camera.position.x + camera.width/2f * sscl) / scaling, (camera.position.y + camera.height/2f * sscl) / scaling);
            Draw.alpha(Mathf.slope(Mathf.clamp(((1f - fout) + offset)/(1f + offset))));
            Draw.mixcol(Pal.spore, 0.5f);
            Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
            Draw.reset();*/

            TextureRegion reg = entity.block.fullIcon;
            float scl = Scl.scl(4f) / camerascale;
            float s = reg.width * Draw.scl * scl * 4f * fout;

            Draw.color(Pal.lightTrail);
            Draw.rect("circle-shadow", entity.x, entity.y, s, s);

            Angles.randLenVectors(1, (1f- fout), 100, 1000f * scl * (1f-fout), (x, y, ffin, ffout) -> {
                Lines.stroke(scl * ffin);
                Lines.lineAngle(entity.x + x, entity.y + y, Mathf.angle(x, y), (ffin * 20 + 1f) * scl);
            });

            Draw.color();
            Draw.mixcol(Color.white, fout);
            Draw.rect(reg, entity.x, entity.y, reg.width * Draw.scl * scl, reg.height * Draw.scl * scl, fout * 135f);

            Draw.reset();
        }
    }

    public void scaleCamera(float amount){
        targetscale *= (amount / 4) + 1;
        clampScale();
    }

    public void clampScale(){
        targetscale = Mathf.clamp(targetscale, minScale(), maxScale());
    }

    public float getDisplayScale(){
        return camerascale;
    }

    public float minScale(){
        return Scl.scl(minZoom);
    }

    public float maxScale(){
        return Mathf.round(Scl.scl(maxZoom));
    }

    public float getScale(){
        return targetscale;
    }

    public void setScale(float scl){
        targetscale = scl;
        clampScale();
    }

    public void zoomIn(float duration){
        landscale = minZoomScl;
        landTime = duration;
    }

    public void takeMapScreenshot(){
        int w = world.width() * tilesize, h = world.height() * tilesize;
        int memory = w * h * 4 / 1024 / 1024;

        if(Vars.checkScreenshotMemory && memory >= (mobile ? 65 : 120)){
            ui.showInfo("@screenshot.invalid");
            return;
        }

        FrameBuffer buffer = new FrameBuffer(w, h);

        drawWeather = false;
        float vpW = camera.width, vpH = camera.height, px = camera.position.x, py = camera.position.y;
        disableUI = true;
        camera.width = w;
        camera.height = h;
        camera.position.x = w / 2f + tilesize / 2f;
        camera.position.y = h / 2f + tilesize / 2f;
        buffer.begin();
        draw();
        Draw.flush();
        byte[] lines = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
        buffer.end();
        disableUI = false;
        camera.width = vpW;
        camera.height = vpH;
        camera.position.set(px, py);
        drawWeather = true;
        buffer.dispose();

        Threads.thread(() -> {
            for(int i = 0; i < lines.length; i += 4){
                lines[i + 3] = (byte)255;
            }
            Pixmap fullPixmap = new Pixmap(w, h);
            Buffers.copy(lines, 0, fullPixmap.pixels, lines.length);
            Fi file = screenshotDirectory.child("screenshot-" + Time.millis() + ".png");
            PixmapIO.writePng(file, fullPixmap);
            fullPixmap.dispose();
            app.post(() -> ui.showInfoFade(Core.bundle.format("screenshot", file.toString())));
        });
    }

    public static class EnvRenderer{
        /** Environment bitmask; must match env exactly when and-ed. */
        public final int env;
        /** Rendering callback. */
        public final Runnable renderer;

        public EnvRenderer(int env, Runnable renderer){
            this.env = env;
            this.renderer = renderer;
        }
    }

}
