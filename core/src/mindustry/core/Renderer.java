package mindustry.core;

import arc.*;
import arc.assets.loaders.TextureLoader.*;
import arc.audio.*;
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
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class Renderer implements ApplicationListener{
    /** These are global variables, for headless access. Cached. */
    public static float laserOpacity = 0.5f, bridgeOpacity = 0.75f;

    public final BlockRenderer blocks = new BlockRenderer();
    public final FogRenderer fog = new FogRenderer();
    public final MinimapRenderer minimap = new MinimapRenderer();
    public final OverlayRenderer overlays = new OverlayRenderer();
    public final LightRenderer lights = new LightRenderer();
    public final Pixelator pixelator = new Pixelator();
    public PlanetRenderer planets;

    public @Nullable Bloom bloom;
    public @Nullable FrameBuffer backgroundBuffer;
    public FrameBuffer effectBuffer = new FrameBuffer();
    public boolean animateShields, drawWeather = true, drawStatus, enableEffects, drawDisplays = true, drawLight = true, pixelate = false;
    public float weatherAlpha;
    /** minZoom = zooming out, maxZoom = zooming in */
    public float minZoom = 1.5f, maxZoom = 6f;
    public Seq<EnvRenderer> envRenderers = new Seq<>();
    public ObjectMap<String, Runnable> customBackgrounds = new ObjectMap<>();
    public TextureRegion[] bubbles = new TextureRegion[16], splashes = new TextureRegion[12];
    public TextureRegion[][] fluidFrames;

    //currently landing core, null if there are no cores or it has finished landing.
    private @Nullable CoreBuild landCore;
    private @Nullable CoreBlock launchCoreType;
    private Color clearColor = new Color(0f, 0f, 0f, 1f);
    private float
    //target camera scale that is lerp-ed to
    targetscale = Scl.scl(4),
    //current actual camera scale
    camerascale = targetscale,
    //starts at coreLandDuration, ends at 0. if positive, core is landing.
    landTime,
    //timer for core landing particles
    landPTimer,
    //intensity for screen shake
    shakeIntensity,
    //reduction rate of screen shake
    shakeReduction,
    //current duration of screen shake
    shakeTime;
    //for landTime > 0: if true, core is currently *launching*, otherwise landing.
    private boolean launching;
    private Vec2 camShakeOffset = new Vec2();

    public Renderer(){
        camera = new Camera();
        Shaders.init();

        Events.on(ResetEvent.class, e -> {
            shakeTime = shakeIntensity = shakeReduction = 0f;
            camShakeOffset.setZero();
        });
    }

    public void shake(float intensity, float duration){
        shakeIntensity = Math.max(shakeIntensity, Mathf.clamp(intensity, 0, 100));
        shakeTime = Math.max(shakeTime, duration);
        shakeReduction = shakeIntensity / shakeTime;
    }

    public void addEnvRenderer(int mask, Runnable render){
        envRenderers.add(new EnvRenderer(mask, render));
    }

    public void addCustomBackground(String name, Runnable render){
        customBackgrounds.put(name, render);
    }

    @Override
    public void init(){
        planets = new PlanetRenderer();

        if(settings.getBool("bloom", !ios)){
            setupBloom();
        }

        EnvRenderers.init();
        for(int i = 0; i < bubbles.length; i++) bubbles[i] = atlas.find("bubble-" + i);
        for(int i = 0; i < splashes.length; i++) splashes[i] = atlas.find("splash-" + i);

        loadFluidFrames();

        Events.on(ClientLoadEvent.class, e -> {
            loadFluidFrames();
        });

        assets.load("sprites/clouds.png", Texture.class).loaded = t -> {
            t.setWrap(TextureWrap.repeat);
            t.setFilter(TextureFilter.linear);
        };

        Events.on(WorldLoadEvent.class, e -> {
            //reset background buffer on every world load, so it can be re-cached first render
            if(backgroundBuffer != null){
                backgroundBuffer.dispose();
                backgroundBuffer = null;
            }
        });
    }

    public void loadFluidFrames(){
        fluidFrames = new TextureRegion[2][Liquid.animationFrames];

        String[] fluidTypes = {"liquid", "gas"};

        for(int i = 0; i < fluidTypes.length; i++){

            for(int j = 0; j < Liquid.animationFrames; j++){
                fluidFrames[i][j] = atlas.find("fluid-" + fluidTypes[i] + "-" + j);
            }
        }
    }

    public TextureRegion[][] getFluidFrames(){
        if(fluidFrames == null || fluidFrames[0][0].texture.isDisposed()){
            loadFluidFrames();
        }
        return fluidFrames;
    }

    @Override
    public void update(){
        Color.white.set(1f, 1f, 1f, 1f);

        float baseTarget = targetscale;

        if(control.input.logicCutscene){
            baseTarget = Mathf.lerp(minZoom, maxZoom, control.input.logicCutsceneZoom);
        }

        float dest = Mathf.clamp(Mathf.round(baseTarget, 0.5f), minScale(), maxScale());
        camerascale = Mathf.lerpDelta(camerascale, dest, 0.1f);
        if(Mathf.equal(camerascale, dest, 0.001f)) camerascale = dest;
        laserOpacity = settings.getInt("lasersopacity") / 100f;
        bridgeOpacity = settings.getInt("bridgeopacity") / 100f;
        animateShields = settings.getBool("animatedshields");
        drawStatus = settings.getBool("blockstatus");
        enableEffects = settings.getBool("effects");
        drawDisplays = !settings.getBool("hidedisplays");
        drawLight = settings.getBool("drawlight", true);
        pixelate = settings.getBool("pixelate");

        //don't bother drawing landing animation if core is null
        if(landCore == null) landTime = 0f;
        if(landTime > 0){
            if(!state.isPaused()) landCore.updateLaunching();

            weatherAlpha = 0f;
            camerascale = landCore.zoomLaunching();

            if(!state.isPaused()) landTime -= Time.delta;
        }else{
            weatherAlpha = Mathf.lerpDelta(weatherAlpha, 1f, 0.08f);
        }

        if(landCore != null && landTime <= 0f){
            landCore.endLaunch();
            landCore = null;
        }

        camera.width = graphics.getWidth() / camerascale;
        camera.height = graphics.getHeight() / camerascale;

        if(state.isMenu()){
            landTime = 0f;
            graphics.clear(Color.black);
        }else{
            minimap.update();

            if(shakeTime > 0){
                float intensity = shakeIntensity * (settings.getInt("screenshake", 4) / 4f) * 0.75f;
                camShakeOffset.setToRandomDirection().scl(Mathf.random(intensity));
                camera.position.add(camShakeOffset);
                shakeIntensity -= shakeReduction * Time.delta;
                shakeTime -= Time.delta;
                shakeIntensity = Mathf.clamp(shakeIntensity, 0f, 100f);
            }else{
                camShakeOffset.setZero();
                shakeIntensity = 0f;
            }

            if(renderer.pixelate){
                pixelator.drawPixelate();
            }else{
                draw();
            }

            camera.position.sub(camShakeOffset);
        }
    }

    public void updateAllDarkness(){
        blocks.updateDarkness();
        minimap.updateAll();
    }

    /** @return whether a launch/land cutscene is playing. */
    public boolean isCutscene(){
        return landTime > 0;
    }

    public float landScale(){
        return landTime > 0 ? camerascale : 1f;
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
        MapPreviewLoader.checkPreviews();

        camera.update();

        if(Float.isNaN(camera.position.x) || Float.isNaN(camera.position.y)){
            camera.position.set(player);
        }

        graphics.clear(clearColor);
        Draw.reset();

        if(settings.getBool("animatedwater") || animateShields){
            effectBuffer.resize(graphics.getWidth(), graphics.getHeight());
        }

        Draw.proj(camera);

        blocks.checkChanges();
        blocks.floor.checkChanges();
        blocks.processBlocks();

        Draw.sort(true);

        Events.fire(Trigger.draw);
        MapPreviewLoader.checkPreviews();

        if(renderer.pixelate){
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
            if((renderer.env & state.rules.env) == renderer.env){
                renderer.renderer.run();
            }
        }

        if(state.rules.lighting && drawLight){
            Draw.draw(Layer.light, lights::draw);
        }

        if(enableDarkness){
            Draw.draw(Layer.darkness, blocks::drawDarkness);
        }

        if(bloom != null){
            bloom.resize(graphics.getWidth(), graphics.getHeight());
            bloom.setBloomIntensity(settings.getInt("bloomintensity", 6) / 4f + 1f);
            bloom.blurPasses = settings.getInt("bloomblur", 1);
            Draw.draw(Layer.bullet - 0.02f, bloom::capture);
            Draw.draw(Layer.effect + 0.02f, bloom::render);
        }

        Draw.draw(Layer.plans, overlays::drawBottom);

        if(animateShields && Shaders.shield != null){
            //TODO would be nice if there were a way to detect if any shields or build beams actually *exist* before beginning/ending buffers, otherwise you're just blitting and swapping shaders for nothing
            Draw.drawRange(Layer.shields, 1f, () -> effectBuffer.begin(Color.clear), () -> {
                effectBuffer.end();
                effectBuffer.blit(Shaders.shield);
            });

            Draw.drawRange(Layer.buildBeam, 1f, () -> effectBuffer.begin(Color.clear), () -> {
                effectBuffer.end();
                effectBuffer.blit(Shaders.buildBeam);
            });
        }

        float scaleFactor = 4f / renderer.getDisplayScale();

        //draw objective markers
        state.rules.objectives.eachRunning(obj -> {
            for(var marker : obj.markers){
                if(marker.world){
                    marker.draw(marker.autoscale ? scaleFactor : 1);
                }
            }
        });

        for(var marker : state.markers){
            if(marker.world){
                marker.draw(marker.autoscale ? scaleFactor : 1);
            }
        }

        Draw.reset();

        Draw.draw(Layer.overlayUI, overlays::drawTop);
        if(state.rules.fog) Draw.draw(Layer.fogOfWar, fog::drawFog);
        Draw.draw(Layer.space, () -> {
            if(landCore == null || landTime <= 0f) return;
            landCore.drawLanding(launching && launchCoreType != null ? launchCoreType : (CoreBlock)landCore.block);
        });

        Events.fire(Trigger.drawOver);
        blocks.drawBlocks();

        Groups.draw.draw(Drawc::draw);

        Draw.reset();
        Draw.flush();
        Draw.sort(false);

        Events.fire(Trigger.postDraw);
    }

    protected void drawBackground(){
        //draw background only if there is no planet background with a skybox
        if(state.rules.backgroundTexture != null && (state.rules.planetBackground == null || !state.rules.planetBackground.drawSkybox)){
            if(!assets.isLoaded(state.rules.backgroundTexture, Texture.class)){
                var file = assets.getFileHandleResolver().resolve(state.rules.backgroundTexture);

                //don't draw invalid/non-existent backgrounds.
                if(!file.exists() || !file.extEquals("png")){
                    return;
                }

                var desc = assets.load(state.rules.backgroundTexture, Texture.class, new TextureParameter(){{
                    wrapU = wrapV = TextureWrap.mirroredRepeat;
                    magFilter = minFilter = TextureFilter.linear;
                }});

                assets.finishLoadingAsset(desc);
            }

            Texture tex = assets.get(state.rules.backgroundTexture, Texture.class);
            Tmp.tr1.set(tex);
            Tmp.tr1.u = 0f;
            Tmp.tr1.v = 0f;

            float ratio = camera.width / camera.height;
            float size = state.rules.backgroundScl;

            Tmp.tr1.u2 = size;
            Tmp.tr1.v2 = size / ratio;

            float sx = 0f, sy = 0f;

            if(!Mathf.zero(state.rules.backgroundSpeed)){
                sx = (camera.position.x) / state.rules.backgroundSpeed;
                sy = (camera.position.y) / state.rules.backgroundSpeed;
            }

            Tmp.tr1.scroll(sx + state.rules.backgroundOffsetX, -sy + state.rules.backgroundOffsetY);

            Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
        }

        if(state.rules.planetBackground != null){
            int size = Math.max(graphics.getWidth(), graphics.getHeight());

            boolean resized = false;
            if(backgroundBuffer == null){
                resized = true;
                backgroundBuffer = new FrameBuffer(size, size);
            }

            if(resized || backgroundBuffer.resizeCheck(size, size)){
                backgroundBuffer.begin(Color.clear);

                var params = state.rules.planetBackground;

                //override some values
                params.viewW = size;
                params.viewH = size;
                params.alwaysDrawAtmosphere = true;
                params.drawUi = false;

                planets.render(params);

                backgroundBuffer.end();
            }

            float drawSize = Math.max(camera.width, camera.height);
            Draw.rect(Draw.wrap(backgroundBuffer.getTexture()), camera.position.x, camera.position.y, drawSize, -drawSize);
        }

        if(state.rules.customBackgroundCallback != null && customBackgrounds.containsKey(state.rules.customBackgroundCallback)){
            customBackgrounds.get(state.rules.customBackgroundCallback).run();
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

    public boolean isLaunching(){
        return launching;
    }

    public CoreBlock getLaunchCoreType(){
        return launchCoreType;
    }

    public float getLandTime(){
        return landTime;
    }

    public float getLandTimeIn(){
        if(landCore == null) return 0f;
        float fin = landTime / landCore.landDuration();
        if(!launching) fin = 1f - fin;
        return fin;
    }

    public float getLandPTimer(){
        return landPTimer;
    }

    public void setLandPTimer(float landPTimer){
        this.landPTimer = landPTimer;
    }

    @Deprecated
    public void showLanding(){
        var core = player.bestCore();
        if(core != null) showLanding(core);
    }

    public void showLanding(CoreBuild landCore){
        this.landCore = landCore;
        launching = false;
        landTime = landCore.landDuration();

        landCore.beginLaunch(null);
        camerascale = landCore.zoomLaunching();
    }

    @Deprecated
    public void showLaunch(CoreBlock coreType){
        var core = player.team().core();
        if(core != null) showLaunch(core, coreType);
    }

    public void showLaunch(CoreBuild landCore, CoreBlock coreType){
        control.input.config.hideConfig();
        control.input.inv.hide();

        this.landCore = landCore;
        launching = true;
        landTime = landCore.landDuration();
        launchCoreType = coreType;

        Music music = landCore.launchMusic();
        music.stop();
        music.play();
        music.setVolume(settings.getInt("musicvol") / 100f);

        landCore.beginLaunch(coreType);
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
            app.post(() -> ui.showInfoFade(bundle.format("screenshot", file.toString())));
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
