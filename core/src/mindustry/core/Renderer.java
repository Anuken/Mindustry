package mindustry.core;

import arc.*;
import arc.files.*;
import arc.fx.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class Renderer implements ApplicationListener{
    public final BlockRenderer blocks = new BlockRenderer();
    public final MinimapRenderer minimap = new MinimapRenderer();
    public final OverlayRenderer overlays = new OverlayRenderer();
    public final LightRenderer lights = new LightRenderer();
    public final Pixelator pixelator = new Pixelator();
    public PlanetRenderer planets;

    public FrameBuffer effectBuffer = new FrameBuffer();
    private Bloom bloom;
    private FxProcessor fx = new FxProcessor();
    private Color clearColor = new Color(0f, 0f, 0f, 1f);
    private float targetscale = Scl.scl(4);
    private float camerascale = targetscale;
    private float landscale = 0f, landTime, weatherAlpha;
    private float minZoomScl = Scl.scl(0.01f);
    private float shakeIntensity, shaketime;

    public Renderer(){
        camera = new Camera();
        Shaders.init();
    }

    public void shake(float intensity, float duration){
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shaketime = Math.max(shaketime, duration);
    }

    @Override
    public void init(){
        planets = new PlanetRenderer();

        if(settings.getBool("bloom")){
            setupBloom();
        }
    }

    @Override
    public void update(){
        Color.white.set(1f, 1f, 1f, 1f);

        camerascale = Mathf.lerpDelta(camerascale, targetscale, 0.1f);

        if(landTime > 0){
            landTime -= Time.delta;
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
            updateShake(0.75f);
            if(pixelator.enabled()){
                pixelator.drawPixelate();
            }else{
                draw();
            }
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
        minimap.dispose();
        effectBuffer.dispose();
        blocks.dispose();
        planets.dispose();
        if(bloom != null){
            bloom.dispose();
            bloom = null;
        }
        Events.fire(new DisposeEvent());
    }

    @Override
    public void resize(int width, int height){
        if(settings.getBool("bloom")){
            setupBloom();
        }

        fx.resize(width, height);
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
            e.printStackTrace();
            settings.put("bloom", false);
            ui.showErrorMessage("@error.bloom");
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

    void beginFx(){
        if(!fx.hasEnabledEffects()) return;

        Draw.flush();
        fx.clear();
        fx.begin();
    }

    void endFx(){
        if(!fx.hasEnabledEffects()) return;

        Draw.flush();
        fx.end();
        fx.applyEffects();
        fx.render(0, 0, fx.getWidth(), fx.getHeight());
    }

    void updateShake(float scale){
        if(shaketime > 0){
            float intensity = shakeIntensity * (settings.getInt("screenshake", 4) / 4f) * scale;
            camera.position.add(Mathf.range(intensity), Mathf.range(intensity));
            shakeIntensity -= 0.25f * Time.delta;
            shaketime -= Time.delta;
            shakeIntensity = Mathf.clamp(shakeIntensity, 0f, 100f);
        }else{
            shakeIntensity = 0f;
        }
    }

    public void draw(){
        camera.update();

        if(Float.isNaN(camera.position.x) || Float.isNaN(camera.position.y)){
            camera.position.set(player);
        }

        graphics.clear(clearColor);
        Draw.reset();

        if(Core.settings.getBool("animatedwater") || Core.settings.getBool("animatedshields")){
            effectBuffer.resize(graphics.getWidth(), graphics.getHeight());
        }

        Draw.proj(camera);

        blocks.floor.checkChanges();
        blocks.processBlocks();

        Draw.sort(true);

        if(pixelator.enabled()){
            pixelator.register();
        }

        //TODO fx

        Draw.draw(Layer.background, this::drawBackground);
        Draw.draw(Layer.floor, blocks.floor::drawFloor);
        Draw.draw(Layer.block - 1, blocks::drawShadows);
        Draw.draw(Layer.block, () -> {
            blocks.floor.beginDraw();
            blocks.floor.drawLayer(CacheLayer.walls);
            blocks.floor.endDraw();
        });

        Draw.drawRange(Layer.blockBuilding, () -> Draw.shader(Shaders.blockbuild, true), Draw::shader);

        if(state.rules.lighting){
            Draw.draw(Layer.light, lights::draw);
        }

        if(enableDarkness){
            Draw.draw(Layer.darkness, blocks::drawDarkness);
        }

        if(bloom != null){
            Draw.draw(Layer.bullet - 0.01f, bloom::capture);
            Draw.draw(Layer.effect + 0.01f, bloom::render);
        }

        Draw.draw(Layer.plans, overlays::drawBottom);

        if(settings.getBool("animatedshields") && Shaders.shield != null){
            Draw.drawRange(Layer.shields, 1f, () -> effectBuffer.begin(Color.clear), () -> {
                effectBuffer.end();
                effectBuffer.blit(Shaders.shield);
            });
        }

        Draw.draw(Layer.overlayUI, overlays::drawTop);
        Draw.draw(Layer.space, this::drawLanding);

        blocks.drawBlocks();

        Groups.draw.draw(Drawc::draw);

        Draw.reset();
        Draw.flush();
        Draw.sort(false);
    }

    private void drawBackground(){

    }

    private void drawLanding(){
        if(landTime > 0 && player.closestCore() != null){
            float fract = landTime / Fx.coreLand.lifetime;
            Building entity = player.closestCore();

            TextureRegion reg = entity.block().icon(Cicon.full);
            float scl = Scl.scl(4f) / camerascale;
            float s = reg.getWidth() * Draw.scl * scl * 4f * fract;

            Draw.color(Pal.lightTrail);
            Draw.rect("circle-shadow", entity.getX(), entity.getY(), s, s);

            Angles.randLenVectors(1, (1f- fract), 100, 1000f * scl * (1f-fract), (x, y, fin, fout) -> {
                Lines.stroke(scl * fin);
                Lines.lineAngle(entity.getX() + x, entity.getY() + y, Mathf.angle(x, y), (fin * 20 + 1f) * scl);
            });

            Draw.color();
            Draw.mixcol(Color.white, fract);
            Draw.rect(reg, entity.getX(), entity.getY(), reg.getWidth() * Draw.scl * scl, reg.getHeight() * Draw.scl * scl, fract * 135f);

            Draw.reset();
        }
    }

    public void scaleCamera(float amount){
        targetscale *= (amount / 4) + 1;
        clampScale();
    }

    public void clampScale(){
        float s = Scl.scl(1f);
        targetscale = Mathf.clamp(targetscale, minScale(), Math.round(s * 6));
    }

    public float minScale(){
        return Scl.scl(1.5f);
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

        if(memory >= 65){
            ui.showInfo("@screenshot.invalid");
            return;
        }

        FrameBuffer buffer = new FrameBuffer(w, h);

        float vpW = camera.width, vpH = camera.height, px = camera.position.x, py = camera.position.y;
        disableUI = true;
        camera.width = w;
        camera.height = h;
        camera.position.x = w / 2f + tilesize / 2f;
        camera.position.y = h / 2f + tilesize / 2f;
        buffer.begin();
        draw();
        buffer.end();
        disableUI = false;
        camera.width = vpW;
        camera.height = vpH;
        camera.position.set(px, py);
        buffer.begin();
        byte[] lines = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
        for(int i = 0; i < lines.length; i += 4){
            lines[i + 3] = (byte)255;
        }
        buffer.end();
        Pixmap fullPixmap = new Pixmap(w, h, Pixmap.Format.rgba8888);
        Buffers.copy(lines, 0, fullPixmap.getPixels(), lines.length);
        Fi file = screenshotDirectory.child("screenshot-" + Time.millis() + ".png");
        PixmapIO.writePNG(file, fullPixmap);
        fullPixmap.dispose();
        ui.showInfoFade(Core.bundle.format("screenshot", file.toString()));

        buffer.dispose();
    }

}
