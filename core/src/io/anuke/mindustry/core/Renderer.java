package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.effect.GroundEffectEntity;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.entities.traits.BelowLiquidTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.blocks.defense.ForceProjector.ShieldEntity;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.entities.EntityDraw;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.EffectEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.batch;
import static io.anuke.ucore.core.Core.camera;

public class Renderer extends RendererModule{
    public final Surface effectSurface;
    public final BlockRenderer blocks = new BlockRenderer();
    public final MinimapRenderer minimap = new MinimapRenderer();
    public final OverlayRenderer overlays = new OverlayRenderer();
    public final FogRenderer fog = new FogRenderer();

    private int targetscale = baseCameraScale;
    private Rectangle rect = new Rectangle(), rect2 = new Rectangle();
    private Vector2 avgPosition = new Translator();

    public Renderer(){
        Core.batch = new SpriteBatch(4096);

        Lines.setCircleVertices(14);

        Shaders.init();

        Core.cameraScale = baseCameraScale;
        Effects.setEffectProvider((effect, color, x, y, rotation, data) -> {
            if(effect == Fx.none) return;
            if(Settings.getBool("effects")){
                Rectangle view = rect.setSize(camera.viewportWidth, camera.viewportHeight)
                        .setCenter(camera.position.x, camera.position.y);
                Rectangle pos = rect2.setSize(effect.size).setCenter(x, y);

                if(view.overlaps(pos)){

                    if(!(effect instanceof GroundEffect)){
                        EffectEntity entity = Pooling.obtain(EffectEntity.class, EffectEntity::new);
                        entity.effect = effect;
                        entity.color = color;
                        entity.rotation = rotation;
                        entity.data = data;
                        entity.id++;
                        entity.set(x, y);
                        if(data instanceof Entity){
                            entity.setParent((Entity) data);
                        }
                        threads.runGraphics(() -> effectGroup.add(entity));
                    }else{
                        GroundEffectEntity entity = Pooling.obtain(GroundEffectEntity.class, GroundEffectEntity::new);
                        entity.effect = effect;
                        entity.color = color;
                        entity.rotation = rotation;
                        entity.id++;
                        entity.data = data;
                        entity.set(x, y);
                        if(data instanceof Entity){
                            entity.setParent((Entity) data);
                        }
                        threads.runGraphics(() -> groundEffectGroup.add(entity));
                    }
                }
            }
        });

        Cursors.cursorScaling = 3;
        Cursors.outlineColor = Color.valueOf("444444");

        Cursors.arrow = Cursors.loadCursor("cursor");
        Cursors.hand = Cursors.loadCursor("hand");
        Cursors.ibeam = Cursors.loadCursor("ibar");
        Cursors.restoreCursor();
        Cursors.loadCustom("drill");
        Cursors.loadCustom("unload");

        clearColor = new Color(0f, 0f, 0f, 1f);

        effectSurface = Graphics.createSurface(Core.cameraScale);
        pixelSurface = Graphics.createSurface(Core.cameraScale);
    }

    @Override
    public void init(){
    }

    @Override
    public void update(){
        //TODO hack, find source of this bug
        Color.WHITE.set(1f, 1f, 1f, 1f);

        if(Core.cameraScale != targetscale){
            float targetzoom = (float) Core.cameraScale / targetscale;
            camera.zoom = Mathf.lerpDelta(camera.zoom, targetzoom, 0.2f);

            if(Mathf.in(camera.zoom, targetzoom, 0.005f)){
                camera.zoom = 1f;
                Graphics.setCameraScale(targetscale);
                for(Player player : players){
                    control.input(player.playerIndex).resetCursor();
                }
            }
        }else{
            camera.zoom = Mathf.lerpDelta(camera.zoom, 1f, 0.2f);
        }

        if(state.is(State.menu)){
            Graphics.clear(Color.BLACK);
        }else{
            Vector2 position = averagePosition();

            if(players[0].isDead()){
                TileEntity core = players[0].getClosestCore();
                if(core != null && players[0].spawner == -1){
                    smoothCamera(core.x, core.y, 0.08f);
                }else{
                    smoothCamera(position.x + 0.0001f, position.y + 0.0001f, 0.08f);
                }
            }else if(!mobile){
                setCamera(position.x + 0.0001f, position.y + 0.0001f);
            }
            camera.position.x = Mathf.clamp(camera.position.x, -tilesize / 2f, world.width() * tilesize - tilesize / 2f);
            camera.position.y = Mathf.clamp(camera.position.y, -tilesize / 2f, world.height() * tilesize - tilesize / 2f);

            float prex = camera.position.x, prey = camera.position.y;
            updateShake(0.75f);

            float deltax = camera.position.x - prex, deltay = camera.position.y - prey;
            float lastx = camera.position.x, lasty = camera.position.y;

            if(snapCamera){
                camera.position.set((int) camera.position.x, (int) camera.position.y, 0);
            }

            if(Gdx.graphics.getHeight() / Core.cameraScale % 2 == 1){
                camera.position.add(0, -0.5f, 0);
            }

            if(Gdx.graphics.getWidth() / Core.cameraScale % 2 == 1){
                camera.position.add(-0.5f, 0, 0);
            }

            draw();

            camera.position.set(lastx - deltax, lasty - deltay, 0);
        }

        if(!ui.chatfrag.chatOpen()){
            renderer.record(); //this only does something if GdxGifRecorder is on the class path, which it usually isn't
        }
    }

    @Override
    public void draw(){
        camera.update();
        if(Float.isNaN(Core.camera.position.x) || Float.isNaN(Core.camera.position.y)){
            Core.camera.position.x = players[0].x;
            Core.camera.position.y = players[0].y;
        }

        Graphics.clear(clearColor);

        batch.setProjectionMatrix(camera.combined);

        Graphics.surface(pixelSurface, false);

        Graphics.clear(clearColor);

        blocks.drawFloor();

        drawAndInterpolate(groundEffectGroup, e -> e instanceof BelowLiquidTrait);
        drawAndInterpolate(puddleGroup);
        drawAndInterpolate(groundEffectGroup, e -> !(e instanceof BelowLiquidTrait));

        blocks.processBlocks();
        blocks.drawShadows();
        for(Team team : Team.all){
            if(blocks.isTeamShown(team)){
                boolean outline = team != players[0].getTeam() && team != Team.none;

                if(outline){
                    Shaders.outline.color.set(team.color);
                    Shaders.outline.color.a = 0.8f;
                    Graphics.beginShaders(Shaders.outline);
                }

                blocks.drawTeamBlocks(Layer.block, team);

                if(outline){
                    Graphics.endShaders();
                }
            }
        }
        blocks.skipLayer(Layer.block);

        Graphics.shader(Shaders.blockbuild, false);
        blocks.drawBlocks(Layer.placement);
        Graphics.shader();

        blocks.drawBlocks(Layer.overlay);

        drawAllTeams(false);

        blocks.skipLayer(Layer.turret);
        blocks.drawBlocks(Layer.laser);

        drawFlyerShadows();

        drawAllTeams(true);

        drawAndInterpolate(bulletGroup);
        drawAndInterpolate(effectGroup);

        overlays.drawBottom();
        drawAndInterpolate(playerGroup, p -> true, Player::drawBuildRequests);

        Graphics.beginShaders(Shaders.shield);
        EntityDraw.draw(shieldGroup);
        EntityDraw.drawWith(shieldGroup, shield -> true, shield -> ((ShieldEntity)shield).drawOver());
        Draw.color(Palette.accent);
        Graphics.endShaders();
        Draw.color();

        overlays.drawTop();

        if(showFog){
            Graphics.surface();
        }else{
            Graphics.flushSurface();
        }

        batch.end();

        if(showFog){
            fog.draw();
        }

        Graphics.beginCam();
        EntityDraw.setClip(false);
        drawAndInterpolate(playerGroup, p -> !p.isDead() && !p.isLocal, Player::drawName);
        EntityDraw.setClip(true);
        Graphics.end();
        Draw.color();
    }

    private void drawFlyerShadows(){
        Graphics.surface(effectSurface, true, false);

        float trnsX = -12, trnsY = -13;

        for(EntityGroup<? extends BaseUnit> group : unitGroups){
            if(!group.isEmpty()){
                drawAndInterpolate(group, unit -> unit.isFlying() && !unit.isDead(), baseUnit -> baseUnit.drawShadow(trnsX, trnsY));
            }
        }

        if(!playerGroup.isEmpty()){
            drawAndInterpolate(playerGroup, unit -> unit.isFlying() && !unit.isDead(), player -> player.drawShadow(trnsX, trnsY));
        }

        Draw.color(0, 0, 0, 0.15f);
        Graphics.flushSurface();
        Draw.color();
    }

    private void drawAllTeams(boolean flying){
        for(Team team : Team.all){
            EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];

            if(group.count(p -> p.isFlying() == flying) +
                    playerGroup.count(p -> p.isFlying() == flying && p.getTeam() == team) == 0 && flying) continue;

            drawAndInterpolate(unitGroups[team.ordinal()], u -> u.isFlying() == flying && !u.isDead(), Unit::drawUnder);
            drawAndInterpolate(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team, Unit::drawUnder);

            Shaders.outline.color.set(team.color);
            Shaders.mix.color.set(Color.WHITE);

            Graphics.beginShaders(Shaders.outline);
            Graphics.shader(Shaders.mix, true);
            drawAndInterpolate(unitGroups[team.ordinal()], u -> u.isFlying() == flying && !u.isDead(), Unit::drawAll);
            drawAndInterpolate(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team, Unit::drawAll);
            Graphics.shader();
            blocks.drawTeamBlocks(Layer.turret, team);
            Graphics.endShaders();

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
        EntityDraw.drawWith(group, toDraw, drawer);
    }

    @Override
    public void resize(int width, int height){
        float lastX = camera.position.x, lastY = camera.position.y;
        super.resize(width, height);
        for(Player player : players){
            control.input(player.playerIndex).resetCursor();
        }
        camera.update();
        camera.position.set(lastX, lastY, 0f);
    }

    @Override
    public void dispose(){
        fog.dispose();
    }

    public Vector2 averagePosition(){
        avgPosition.setZero();

        drawAndInterpolate(playerGroup, p -> p.isLocal, p -> avgPosition.add(p.x, p.y));

        avgPosition.scl(1f / players.length);
        return avgPosition;
    }

    public void setCameraScale(int amount){
        targetscale = amount;
        clampScale();
        //scale up all surfaces in preparation for the zoom
        for(Surface surface : Graphics.getSurfaces()){
            surface.setScale(targetscale);
        }
    }

    public void scaleCamera(int amount){
        setCameraScale(targetscale + amount);
    }

    public void clampScale(){
        float s = io.anuke.ucore.scene.ui.layout.Unit.dp.scl(1f);
        targetscale = Mathf.clamp(targetscale, Math.round(s * 2), Math.round(s * 5));
    }

    public void takeMapScreenshot(){
        float vpW = Core.camera.viewportWidth, vpH = Core.camera.viewportHeight;
        int w = world.width()*tilesize, h =  world.height()*tilesize;
        int pw = pixelSurface.width(), ph = pixelSurface.height();
        showFog = false;
        disableUI = true;
        pixelSurface.setSize(w, h, true);
        Graphics.getEffectSurface().setSize(w, h, true);
        Core.camera.viewportWidth = w;
        Core.camera.viewportHeight = h;
        Core.camera.position.x = w/2f;
        Core.camera.position.y = h/2f;

        draw();

        showFog = true;
        disableUI = false;
        Core.camera.viewportWidth = vpW;
        Core.camera.viewportHeight = vpH;

        pixelSurface.getBuffer().begin();
        byte[] lines = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
        for(int i = 0; i < lines.length; i+= 4){
            lines[i + 3] = (byte)255;
        }
        pixelSurface.getBuffer().end();

        Pixmap fullPixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        BufferUtils.copy(lines, 0, fullPixmap.getPixels(), lines.length);
        FileHandle file = screenshotDirectory.child("screenshot-" + TimeUtils.millis() + ".png");
        PixmapIO.writePNG(file, fullPixmap);
        fullPixmap.dispose();

        pixelSurface.setSize(pw, ph, false);
        Graphics.getEffectSurface().setSize(pw, ph, false);

        ui.showInfoFade(Bundles.format("text.screenshot", file.toString()));
    }

}
