package io.anuke.mindustry.core;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.entities.Effects;
import io.anuke.arc.entities.EntityDraw;
import io.anuke.arc.entities.EntityGroup;
import io.anuke.arc.entities.impl.EffectEntity;
import io.anuke.arc.entities.trait.DrawTrait;
import io.anuke.arc.entities.trait.Entity;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.pooling.Pools;
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

import static io.anuke.arc.Core.camera;
import static io.anuke.arc.Core.graphics;
import static io.anuke.mindustry.Vars.*;

public class Renderer implements ApplicationListener{
    public final BlockRenderer blocks = new BlockRenderer();
    public final MinimapRenderer minimap = new MinimapRenderer();
    public final OverlayRenderer overlays = new OverlayRenderer();
    public final FogRenderer fog = new FogRenderer();

    private Color clearColor;
    private int targetscale = baseCameraScale;
    private Rectangle rect = new Rectangle(), rect2 = new Rectangle();
    private Vector2 avgPosition = new Vector2();

    public Renderer(){
        Lines.setCircleVertices(14);

        Shaders.init();

        Effects.setEffectProvider((effect, color, x, y, rotation, data) -> {
            if(effect == Fx.none) return;
            if(Core.settings.getBool("effects")){
                Rectangle view = rect.setSize(camera.width, camera.height)
                        .setCenter(camera.position.x, camera.position.y);
                Rectangle pos = rect2.setSize(effect.size).setCenter(x, y);

                if(view.overlaps(pos)){

                    if(!(effect instanceof GroundEffect)){
                        EffectEntity entity = Pools.obtain(EffectEntity.class, EffectEntity::new);
                        entity.effect = effect;
                        entity.color = color;
                        entity.rotation = rotation;
                        entity.data = data;
                        entity.id++;
                        entity.set(x, y);
                        if(data instanceof Entity){
                            entity.setParent((Entity) data);
                        }
                        effectGroup.add(entity);
                    }else{
                        GroundEffectEntity entity = Pools.obtain(GroundEffectEntity.class, GroundEffectEntity::new);
                        entity.effect = effect;
                        entity.color = color;
                        entity.rotation = rotation;
                        entity.id++;
                        entity.data = data;
                        entity.set(x, y);
                        if(data instanceof Entity){
                            entity.setParent((Entity) data);
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

        if(state.is(State.menu)){
            graphics.clear(Color.BLACK);
        }else{
            Vector2 position = averagePosition();

            if(players[0].isDead()){
                TileEntity core = players[0].getClosestCore();
                if(core != null && players[0].spawner == Unit.noSpawner){
                    camera.position.lerpDelta(core.x, core.y, 0.08f);
                }else{
                    camera.position.lerpDelta(position, 0.08f);
                }
            }else if(!mobile){
                camera.position.set(position);
            }

            camera.position.x = Mathf.clamp(camera.position.x, -tilesize / 2f, world.width() * tilesize - tilesize / 2f);
            camera.position.y = Mathf.clamp(camera.position.y, -tilesize / 2f, world.height() * tilesize - tilesize / 2f);

            float prex = camera.position.x, prey = camera.position.y;
            //TODO update screenshake
            //updateShake(0.75f);

            float deltax = camera.position.x - prex, deltay = camera.position.y - prey;
            float lastx = camera.position.x, lasty = camera.position.y;

            if(snapCamera){
                camera.position.set((int) camera.position.x, (int) camera.position.y);
            }

            draw();

            camera.position.set(lastx - deltax, lasty - deltay);
        }

        if(!ui.chatfrag.chatOpen()){
            //TODO does not work
            //ScreenRecorder.record(); //this only does something if CoreGifRecorder is on the class path, which it usually isn't
        }
    }

    public void draw(){
        camera.update();
        if(Float.isNaN(camera.position.x) || Float.isNaN(camera.position.y)){
            camera.position.x = players[0].x;
            camera.position.y = players[0].y;
        }

        graphics.clear(clearColor);

        graphics.batch().setProjection(camera.projection());

        blocks.drawFloor();

        drawAndInterpolate(groundEffectGroup, e -> e instanceof BelowLiquidTrait);
        drawAndInterpolate(puddleGroup);
        drawAndInterpolate(groundEffectGroup, e -> !(e instanceof BelowLiquidTrait));

        blocks.processBlocks();
        blocks.drawShadows();
        for(Team team : Team.all){
            if(blocks.isTeamShown(team)){
                blocks.drawTeamBlocks(Layer.block, team);
            }
        }
        blocks.skipLayer(Layer.block);

        Draw.shader(Shaders.blockbuild, false);
        blocks.drawBlocks(Layer.placement);
        Draw.shader();

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

        //TODO shield
        /*
        Graphics.beginShaders(Shaders.shield);
        EntityDraw.draw(shieldGroup);
        EntityDraw.drawWith(shieldGroup, shield -> true, shield -> ((ShieldEntity)shield).drawOver());
        Draw.color(Palette.accent);
        Graphics.endShaders();
        Draw.color();
        */

        overlays.drawTop();

        //TODO fog
        /*
        if(showFog){
            Graphics.surface();
        }else{
            Graphics.flushSurface();
        }*/

        //batch.end();

        if(showFog){
       //     fog.draw();
        }

        //TODO this isn't necessary anymore
        //Graphics.beginCam();

        EntityDraw.setClip(false);
        drawAndInterpolate(playerGroup, p -> !p.isDead() && !p.isLocal, Player::drawName);
        EntityDraw.setClip(true);
        //Graphics.end();

        Draw.color();
        Draw.flush();
    }

    private void drawFlyerShadows(){
        //TODO fix flyer shadows
        //Graphics.surface(effectSurface, true, false);

        float trnsX = -12, trnsY = -13;

        for(EntityGroup<? extends BaseUnit> group : unitGroups){
            if(!group.isEmpty()){
                drawAndInterpolate(group, unit -> unit.isFlying() && !unit.isDead(), baseUnit -> baseUnit.drawShadow(trnsX, trnsY));
            }
        }

        if(!playerGroup.isEmpty()){
            drawAndInterpolate(playerGroup, unit -> unit.isFlying() && !unit.isDead(), player -> player.drawShadow(trnsX, trnsY));
        }

        //Draw.color(0, 0, 0, 0.15f);
        //Graphics.flushSurface();
       // Draw.color();
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

            //Graphics.beginShaders(Shaders.outline);
            //Draw.shader(Shaders.mix, true);
            drawAndInterpolate(unitGroups[team.ordinal()], u -> u.isFlying() == flying && !u.isDead(), Unit::drawAll);
            drawAndInterpolate(playerGroup, p -> p.isFlying() == flying && p.getTeam() == team, Unit::drawAll);
            //Draw.shader();
            blocks.drawTeamBlocks(Layer.turret, team);
            //Graphics.endShaders();

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
        camera.resize(width, height);
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
    }

    public void scaleCamera(int amount){
        setCameraScale(targetscale + amount);
    }

    public void clampScale(){
        float s = io.anuke.arc.scene.ui.layout.Unit.dp.scl(1f);
        targetscale = Mathf.clamp(targetscale, Math.round(s * 2), Math.round(s * 5));
    }

    public void takeMapScreenshot(){
        //TODO fix/implement
        /*
        float vpW = camera.width, vpH = camera.height;
        int w = world.width()*tilesize, h =  world.height()*tilesize;
        int pw = pixelSurface.width(), ph = pixelSurface.height();
        showFog = false;
        disableUI = true;
        pixelSurface.setSize(w, h, true);
        Graphics.getEffectSurface().setSize(w, h, true);
        camera.width = w;
        camera.height = h;
        camera.position.x = w/2f + tilesize/2f;
        camera.position.y = h/2f + tilesize/2f;

        draw();

        showFog = true;
        disableUI = false;
        camera.width = vpW;
        camera.height = vpH;

        pixelSurface.getBuffer().begin();
        byte[] lines = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
        for(int i = 0; i < lines.length; i+= 4){
            lines[i + 3] = (byte)255;
        }
        pixelSurface.getBuffer().end();

        Pixmap fullPixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        BufferUtils.copy(lines, 0, fullPixmap.getPixels(), lines.length);
        FileHandle file = screenshotDirectory.child("screenshot-" + Time.millis() + ".png");
        PixmapIO.writePNG(file, fullPixmap);
        fullPixmap.dispose();

        pixelSurface.setSize(pw, ph, false);
        Graphics.getEffectSurface().setSize(pw, ph, false);

        ui.showInfoFade(Core.bundle.format("text.screenshot", file.toString()));*/
    }

}
