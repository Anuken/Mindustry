package io.anuke.mindustry.core;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.glutils.FrameBuffer;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.effect.GroundEffectEntity;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.entities.impl.EffectEntity;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.blocks.defense.ForceProjector.ShieldEntity;

import static io.anuke.arc.Core.*;
import static io.anuke.mindustry.Vars.*;

public class Renderer implements ApplicationListener{
    public final BlockRenderer blocks = new BlockRenderer();
    public final MinimapRenderer minimap = new MinimapRenderer();
    public final OverlayRenderer overlays = new OverlayRenderer();
    public final Pixelator pixelator = new Pixelator();

    public FrameBuffer shieldBuffer = new FrameBuffer(2, 2);
    private Color clearColor;
    private float targetscale = io.anuke.arc.scene.ui.layout.Unit.dp.scl(4);
    private float camerascale = targetscale;
    private Rectangle rect = new Rectangle(), rect2 = new Rectangle();
    private float shakeIntensity, shaketime;

    public Renderer(){
        batch = new SpriteBatch(4096);
        camera = new Camera();
        Lines.setCircleVertices(20);
        Shaders.init();

        Effects.setScreenShakeProvider((intensity, duration) -> {
            shakeIntensity = Math.max(intensity, shakeIntensity);
            shaketime = Math.max(shaketime, duration);
        });

        Effects.setEffectProvider((effect, color, x, y, rotation, data) -> {
            if(effect == Fx.none) return;
            if(Core.settings.getBool("effects")){
                Rectangle view = camera.bounds(rect);
                Rectangle pos = rect2.setSize(effect.size).setCenter(x, y);

                if(view.overlaps(pos)){

                    if(!(effect instanceof GroundEffect)){
                        EffectEntity entity = Pools.obtain(EffectEntity.class, EffectEntity::new);
                        entity.effect = effect;
                        entity.color.set(color);
                        entity.rotation = rotation;
                        entity.data = data;
                        entity.id++;
                        entity.set(x, y);
                        if(data instanceof Entity){
                            entity.setParent((Entity)data);
                        }
                        effectGroup.add(entity);
                    }else{
                        GroundEffectEntity entity = Pools.obtain(GroundEffectEntity.class, GroundEffectEntity::new);
                        entity.effect = effect;
                        entity.color.set(color);
                        entity.rotation = rotation;
                        entity.id++;
                        entity.data = data;
                        entity.set(x, y);
                        if(data instanceof Entity){
                            entity.setParent((Entity)data);
                        }
                        groundEffectGroup.add(entity);
                    }
                }
            }
        });

        clearColor = new Color(0f, 0f, 0f, 1f);
    }

    @Override
    public void update(){
        //TODO hack, find source of this bug
        Color.WHITE.set(1f, 1f, 1f, 1f);

        camerascale = Mathf.lerpDelta(camerascale, targetscale, 0.1f);
        camera.width = graphics.getWidth() / camerascale;
        camera.height = graphics.getHeight() / camerascale;

        if(state.is(State.menu)){
            graphics.clear(Color.BLACK);
        }else{
            Vector2 position = Tmp.v3.set(player);

            if(player.isDead()){
                TileEntity core = player.getClosestCore();
                if(core != null && player.spawner == null){
                    camera.position.lerpDelta(core.x, core.y, 0.08f);
                }else{
                    camera.position.lerpDelta(position, 0.08f);
                }
            }else if(!mobile){
                camera.position.lerpDelta(position, 0.08f);
            }

            updateShake(0.75f);
            if(pixelator.enabled()){
                pixelator.drawPixelate();
            }else{
                draw();
            }
        }
    }

    @Override
    public void dispose(){
        minimap.dispose();
        shieldBuffer.dispose();
        blocks.dispose();
    }

    void updateShake(float scale){
        if(shaketime > 0){
            float intensity = shakeIntensity * (settings.getInt("screenshake", 4) / 4f) * scale;
            camera.position.add(Mathf.range(intensity), Mathf.range(intensity));
            shakeIntensity -= 0.25f * Time.delta();
            shaketime -= Time.delta();
            shakeIntensity = Mathf.clamp(shakeIntensity, 0f, 100f);
        }else{
            shakeIntensity = 0f;
        }
    }

    public void draw(){
        camera.update();

        if(Float.isNaN(camera.position.x) || Float.isNaN(camera.position.y)){
            camera.position.x = player.x;
            camera.position.y = player.y;
        }

        graphics.clear(clearColor);

        if(!graphics.isHidden() && (Core.settings.getBool("animatedwater") || Core.settings.getBool("animatedshields")) && (shieldBuffer.getWidth() != graphics.getWidth() || shieldBuffer.getHeight() != graphics.getHeight())){
            shieldBuffer.resize(graphics.getWidth(), graphics.getHeight());
        }

        Draw.proj(camera.projection());

        blocks.floor.drawFloor();

        drawAndInterpolate(groundEffectGroup, e -> e instanceof BelowLiquidTrait);
        drawAndInterpolate(puddleGroup);
        drawAndInterpolate(groundEffectGroup, e -> !(e instanceof BelowLiquidTrait));

        blocks.processBlocks();

        blocks.drawShadows();
        Draw.color();

        blocks.floor.beginDraw();
        blocks.floor.drawLayer(CacheLayer.walls);
        blocks.floor.endDraw();

        blocks.drawBlocks(Layer.block);
        blocks.drawFog();

        Draw.shader(Shaders.blockbuild, true);
        blocks.drawBlocks(Layer.placement);
        Draw.shader();

        blocks.drawBlocks(Layer.overlay);

        drawGroundShadows();

        drawAllTeams(false);

        blocks.skipLayer(Layer.turret);
        blocks.drawBlocks(Layer.laser);

        drawFlyerShadows();

        drawAllTeams(true);

        drawAndInterpolate(bulletGroup);
        drawAndInterpolate(effectGroup);

        overlays.drawBottom();
        drawAndInterpolate(playerGroup, p -> true, Player::drawBuildRequests);

        if(Entities.countInBounds(shieldGroup) > 0){
            if(settings.getBool("animatedshields")){
                Draw.flush();
                shieldBuffer.begin();
                graphics.clear(Color.CLEAR);
                Entities.draw(shieldGroup);
                Entities.draw(shieldGroup, shield -> true, shield -> ((ShieldEntity)shield).drawOver());
                Draw.flush();
                shieldBuffer.end();
                Draw.shader(Shaders.shield);
                Draw.color(Pal.accent);
                Draw.rect(Draw.wrap(shieldBuffer.getTexture()), camera.position.x, camera.position.y, camera.width, -camera.height);
                Draw.color();
                Draw.shader();
            }else{
                Entities.draw(shieldGroup, shield -> true, shield -> ((ShieldEntity)shield).drawSimple());
            }
        }

        overlays.drawTop();

        drawAndInterpolate(playerGroup, p -> !p.isDead() && !p.isLocal, Player::drawName);

        Draw.color();
        Draw.flush();
    }

    private void drawGroundShadows(){
        Draw.color(0, 0, 0, 0.4f);
        float rad = 1.6f;

        Consumer<Unit> draw = u -> {
            float size = Math.max(u.getIconRegion().getWidth(), u.getIconRegion().getHeight()) * Draw.scl;
            Draw.rect("circle-shadow", u.x, u.y, size * rad, size * rad);
        };

        for(EntityGroup<? extends BaseUnit> group : unitGroups){
            if(!group.isEmpty()){
                drawAndInterpolate(group, unit -> !unit.isDead(), draw::accept);
            }
        }

        if(!playerGroup.isEmpty()){
            drawAndInterpolate(playerGroup, unit -> !unit.isDead(), draw::accept);
        }

        Draw.color();
    }

    private void drawFlyerShadows(){
        float trnsX = -12, trnsY = -13;
        Draw.color(0, 0, 0, 0.22f);

        for(EntityGroup<? extends BaseUnit> group : unitGroups){
            if(!group.isEmpty()){
                drawAndInterpolate(group, unit -> unit.isFlying() && !unit.isDead(), baseUnit -> baseUnit.drawShadow(trnsX, trnsY));
            }
        }

        if(!playerGroup.isEmpty()){
            drawAndInterpolate(playerGroup, unit -> unit.isFlying() && !unit.isDead(), player -> player.drawShadow(trnsX, trnsY));
        }

        Draw.color();
    }

    private void drawAllTeams(boolean flying){
        for(Team team : Team.all){
            EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];

            if(group.count(p -> p.isFlying() == flying) +
            playerGroup.count(p -> p.isFlying() == flying && p.getTeam() == team) == 0 && flying) continue;

            drawAndInterpolate(unitGroups[team.ordinal()], u -> u.isFlying() == flying && !u.isDead(), Unit::drawUnder);
            drawAndInterpolate(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team && !p.isDead(), Unit::drawUnder);

            drawAndInterpolate(unitGroups[team.ordinal()], u -> u.isFlying() == flying && !u.isDead(), Unit::drawAll);
            drawAndInterpolate(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team, Unit::drawAll);
            blocks.drawTeamBlocks(Layer.turret, team);

            drawAndInterpolate(unitGroups[team.ordinal()], u -> u.isFlying() == flying && !u.isDead(), Unit::drawOver);
            drawAndInterpolate(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team, Unit::drawOver);
        }
    }

    public <T extends DrawTrait> void drawAndInterpolate(EntityGroup<T> group){
        drawAndInterpolate(group, t -> true, DrawTrait::draw);
    }

    public <T extends DrawTrait> void drawAndInterpolate(EntityGroup<T> group, Predicate<T> toDraw){
        drawAndInterpolate(group, toDraw, DrawTrait::draw);
    }

    public <T extends DrawTrait> void drawAndInterpolate(EntityGroup<T> group, Predicate<T> toDraw, Consumer<T> drawer){
        Entities.draw(group, toDraw, drawer);
    }

    public void scaleCamera(float amount){
        targetscale += amount;
        clampScale();
    }

    public void clampScale(){
        float s = io.anuke.arc.scene.ui.layout.Unit.dp.scl(1f);
        targetscale = Mathf.clamp(targetscale, s * 1.5f, Math.round(s * 6));
    }

    public float getScale(){
        return targetscale;
    }

    public void setScale(float scl){
        targetscale = scl;
        clampScale();
    }

    public void takeMapScreenshot(){
        drawGroundShadows();

        int w = world.width() * tilesize, h = world.height() * tilesize;
        int memory = w * h * 4 / 1024 / 1024;

        if(memory >= 65){
            ui.showInfo("$screenshot.invalid");
            return;
        }

        boolean hadShields = Core.settings.getBool("animatedshields");
        boolean hadWater = Core.settings.getBool("animatedwater");
        Core.settings.put("animatedwater", false);
        Core.settings.put("animatedshields", false);

        FrameBuffer buffer = new FrameBuffer(w, h);

        float vpW = camera.width, vpH = camera.height, px = camera.position.x, py = camera.position.y;
        disableUI = true;
        camera.width = w;
        camera.height = h;
        camera.position.x = w / 2f + tilesize / 2f;
        camera.position.y = h / 2f + tilesize / 2f;
        Draw.flush();
        buffer.begin();
        draw();
        Draw.flush();
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
        Pixmap fullPixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        BufferUtils.copy(lines, 0, fullPixmap.getPixels(), lines.length);
        FileHandle file = screenshotDirectory.child("screenshot-" + Time.millis() + ".png");
        PixmapIO.writePNG(file, fullPixmap);
        fullPixmap.dispose();
        ui.showInfoFade(Core.bundle.format("screenshot", file.toString()));

        buffer.dispose();

        Core.settings.put("animatedwater", hadWater);
        Core.settings.put("animatedshields", hadShields);
    }

}
