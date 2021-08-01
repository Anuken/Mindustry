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
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class Renderer implements ApplicationListener{
    /** These are global variables, for headless access. Cached. */
    public static float laserOpacity = 0.5f, bridgeOpacity = 0.75f;

    private static final float cloudScaling = 1700f, cfinScl = -2f, cfinOffset = 0.3f, calphaFinOffset = 0.25f;
    private static final float[] cloudAlphas = {0, 0.5f, 1f, 0.1f, 0, 0f};
    private static final float cloudAlpha = 0.81f;
    private static final float[] thrusterSizes = {0f, 0f, 0f, 0f, 0.3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 0f};
    private static final Interp landInterp = Interp.pow3;

    public final BlockRenderer blocks = new BlockRenderer();
    public final MinimapRenderer minimap = new MinimapRenderer();
    public final OverlayRenderer overlays = new OverlayRenderer();
    public final LightRenderer lights = new LightRenderer();
    public final Pixelator pixelator = new Pixelator();
    public PlanetRenderer planets;

    public @Nullable Bloom bloom;
    public FrameBuffer effectBuffer = new FrameBuffer();
    public boolean animateShields, drawWeather = true, drawStatus;
    public float weatherAlpha;
    /** minZoom = zooming out, maxZoom = zooming in */
    public float minZoom = 1.5f, maxZoom = 6f;
    public Seq<EnvRenderer> envRenderers = new Seq<>();
    public TextureRegion[] bubbles = new TextureRegion[16], splashes = new TextureRegion[12];

    private @Nullable CoreBuild landCore;
    private @Nullable CoreBlock launchCoreType;
    private Color clearColor = new Color(0f, 0f, 0f, 1f);
    private float
    //seed for cloud visuals, 0-1
    cloudSeed = 0f,
    //target camera scale that is lerp-ed to
    targetscale = Scl.scl(4),
    //current actual camera scale
    camerascale = targetscale,
    //minimum camera zoom value for landing/launching; constant TODO make larger?
    minZoomScl = Scl.scl(0.02f),
    //starts at coreLandDuration, ends at 0. if positive, core is landing.
    landTime,
    //timer for core landing particles
    landPTimer,
    //intensity for screen shake
    shakeIntensity,
    //current duration of screen shake
    shakeTime;
    //for landTime > 0: if true, core is currently *launching*, otherwise landing.
    private boolean launching;
    private Vec2 camShakeOffset = new Vec2();

    public Renderer(){
        camera = new Camera();
        Shaders.init();
    }

    public void shake(float intensity, float duration){
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeTime = Math.max(shakeTime, duration);
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
                updateLandParticles();
            }

            if(!state.isPaused()){
                landTime -= Time.delta;
            }
            float fin = landTime / coreLandDuration;
            if(!launching) fin = 1f - fin;
            camerascale = landInterp.apply(minZoomScl, Scl.scl(4f), fin);
            weatherAlpha = 0f;

            //snap camera to cutscene core regardless of player input
            if(landCore != null){
                camera.position.set(landCore);
            }
        }else{
            weatherAlpha = Mathf.lerpDelta(weatherAlpha, 1f, 0.08f);
        }

        camera.width = graphics.getWidth() / camerascale;
        camera.height = graphics.getHeight() / camerascale;

        if(state.isMenu()){
            landTime = 0f;
            graphics.clear(Color.black);
        }else{
            if(shakeTime > 0){
                float intensity = shakeIntensity * (settings.getInt("screenshake", 4) / 4f) * 0.75f;
                camShakeOffset.setToRandomDirection().scl(Mathf.random(intensity));
                camera.position.add(camShakeOffset);
                shakeIntensity -= 0.25f * Time.delta;
                shakeTime -= Time.delta;
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
        //nothing to draw currently
    }

    void updateLandParticles(){
        float time = launching ? coreLandDuration - landTime : landTime;
        float tsize = Mathf.sample(thrusterSizes, (time + 35f) / coreLandDuration);

        landPTimer += tsize * Time.delta;
        if(landCore != null && landPTimer >= 1f){
            landCore.tile.getLinkedTiles(t -> {
                if(Mathf.chance(0.4f)){
                    Fx.coreLandDust.at(t.worldx(), t.worldy(), landCore.angleTo(t) + Mathf.range(30f), Tmp.c1.set(t.floor().mapColor).mul(1.5f + Mathf.range(0.15f)));
                }
            });

            landPTimer = 0f;
        }
    }

    void drawLanding(){
        CoreBuild build = landCore == null ? player.bestCore() : landCore;
        var clouds = assets.get("sprites/clouds.png", Texture.class);
        if(landTime > 0 && build != null){
            float fout = landTime / coreLandDuration;

            if(launching) fout = 1f - fout;

            float fin = 1f - fout;

            //draw core
            var block = launching && launchCoreType != null ? launchCoreType : (CoreBlock)build.block;
            TextureRegion reg = block.fullIcon;
            float scl = Scl.scl(4f) / camerascale;
            float shake = 0f;
            float s = reg.width * Draw.scl * scl * 3.6f * Interp.pow2Out.apply(fout);
            float rotation = Interp.pow2In.apply(fout) * 135f, x = build.x + Mathf.range(shake), y = build.y + Mathf.range(shake);
            float thrustOpen = 0.25f;
            float thrusterFrame = fin >= thrustOpen ? 1f : fin / thrustOpen;
            float thrusterSize = Mathf.sample(thrusterSizes, fin);

            //when launching, thrusters stay out the entire time.
            if(launching){
                Interp i = Interp.pow2Out;
                thrusterFrame = i.apply(Mathf.clamp(fout*13f));
                thrusterSize = i.apply(Mathf.clamp(fout*9f));
            }

            Draw.color(Pal.lightTrail);
            //TODO spikier heat
            Draw.rect("circle-shadow", x, y, s, s);

            Draw.color(Pal.lightTrail);

            float pfin = Interp.pow3Out.apply(fin), pf = Interp.pow2In.apply(fout);

            //draw particles
            Angles.randLenVectors(1, pfin, 100, 800f * scl * pfin, (ax, ay, ffin, ffout) -> {
                Lines.stroke(scl * ffin * pf * 3f);
                Lines.lineAngle(build.x + ax, build.y + ay, Mathf.angle(ax, ay), (ffin * 20 + 1f) * scl);
            });

            Draw.color();
            Draw.mixcol(Color.white, Interp.pow5In.apply(fout));

            //accent tint indicating that the core was just constructed
            if(launching){
                float f = Mathf.clamp(1f - fout * 12f);
                if(f > 0.001f){
                    Draw.mixcol(Pal.accent, f);
                }
            }

            Draw.scl(scl);

            Draw.alpha(1f);

            //draw thruster flame
            float strength = (1f + (block.size - 3)/2.5f) * scl * thrusterSize * (0.95f + Mathf.absin(2f, 0.1f));
            float offset = (block.size - 3) * 3f * scl;

            for(int i = 0; i < 4; i++){
                Tmp.v1.trns(i * 90 + rotation, 1f);

                Tmp.v1.setLength((block.size * tilesize/2f + 1f)*scl + strength*2f + offset);
                Draw.color(build.team.color);
                Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 6f * strength);

                Tmp.v1.setLength((block.size * tilesize/2f + 1f)*scl + strength*0.5f + offset);
                Draw.color(Color.white);
                Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 3.5f * strength);
            }

            drawThrusters(block, x, y, rotation, thrusterFrame);

            Drawf.spinSprite(block.region, x, y, rotation);

            Draw.alpha(Interp.pow4In.apply(thrusterFrame));
            drawThrusters(block, x, y, rotation, thrusterFrame);
            Draw.alpha(1f);

            Drawf.spinSprite(block.teamRegions[build.team.id], x, y, rotation);

            Draw.scl();

            Draw.reset();

            //draw clouds
            if(state.rules.cloudColor.a > 0.0001f){
                float scaling = cloudScaling;
                float sscl = Math.max(1f + Mathf.clamp(fin + cfinOffset)* cfinScl, 0f) * camerascale;

                Tmp.tr1.set(clouds);
                Tmp.tr1.set(
                (camera.position.x - camera.width/2f * sscl) / scaling,
                (camera.position.y - camera.height/2f * sscl) / scaling,
                (camera.position.x + camera.width/2f * sscl) / scaling,
                (camera.position.y + camera.height/2f * sscl) / scaling);

                Tmp.tr1.scroll(10f * cloudSeed, 10f * cloudSeed);

                Draw.alpha(Mathf.sample(cloudAlphas, fin + calphaFinOffset) * cloudAlpha);
                Draw.mixcol(state.rules.cloudColor, state.rules.cloudColor.a);
                Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
                Draw.reset();
            }
        }
    }

    void drawThrusters(CoreBlock block, float x, float y, float rotation, float frame){
        float length = block.thrusterLength * (frame - 1f) - 1f/4f;
        float alpha = Draw.getColor().a;

        //two passes for consistent lighting
        for(int j = 0; j < 2; j++){
            for(int i = 0; i < 4; i++){
                var reg = i >= 2 ? block.thruster2 : block.thruster1;
                float rot = (i * 90) + rotation % 90f;
                Tmp.v1.trns(rot, length * Draw.xscl);

                //second pass applies extra layer of shading
                if(j == 1){
                    Tmp.v1.rotate(-90f);
                    Draw.alpha((rotation % 90f) / 90f * alpha);
                    rot -= 90f;
                    Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                }else{
                    Draw.alpha(alpha);
                    Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                }
            }
        }
        Draw.alpha(1f);
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

    public void showLanding(){
        launching = false;
        camerascale = minZoomScl;
        landTime = coreLandDuration;
        cloudSeed = Mathf.random(1f);
    }

    public void showLaunch(CoreBlock coreType){
        Vars.ui.hudfrag.showLaunch();
        launchCoreType = coreType;
        launching = true;
        landCore = player.team().core();
        cloudSeed = Mathf.random(1f);
        landTime = coreLandDuration;
        if(landCore != null){
            Fx.coreLaunchConstruct.at(landCore.x, landCore.y, coreType.size);
        }
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
