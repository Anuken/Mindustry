package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;

import static arc.Core.camera;
import static mindustry.Vars.*;

public class BlockRenderer implements Disposable{
    private static final int initialRequests = 32 * 32;
    private static final int expandr = 9;
    private static final Color shadowColor = new Color(0, 0, 0, 0.71f);

    public final FloorRenderer floor = new FloorRenderer();

    private Seq<Tile> tileview = new Seq<>(false, initialRequests, Tile.class);
    private Seq<Tile> lightview = new Seq<>(false, initialRequests, Tile.class);

    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private float brokenFade = 0f;
    private FrameBuffer shadows = new FrameBuffer();
    private FrameBuffer dark = new FrameBuffer();
    private Seq<Building> outArray2 = new Seq<>();
    private Seq<Tile> shadowEvents = new Seq<>();
    private IntSet processedEntities = new IntSet();
    private boolean displayStatus = false;

    public BlockRenderer(){

        Events.on(WorldLoadEvent.class, event -> {
            shadowEvents.clear();
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated

            shadows.getTexture().setFilter(TextureFilter.linear, TextureFilter.linear);
            shadows.resize(world.width(), world.height());
            shadows.begin();
            Core.graphics.clear(Color.white);
            Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

            Draw.color(shadowColor);

            for(Tile tile : world.tiles){
                if(tile.block().hasShadow){
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                }
            }

            Draw.flush();
            Draw.color();
            shadows.end();

            dark.getTexture().setFilter(TextureFilter.linear, TextureFilter.linear);
            dark.resize(world.width(), world.height());
            dark.begin();
            Core.graphics.clear(Color.white);
            Draw.proj().setOrtho(0, 0, dark.getWidth(), dark.getHeight());

            for(Tile tile : world.tiles){
                float darkness = world.getDarkness(tile.x, tile.y);

                if(darkness > 0){
                    Draw.color(0f, 0f, 0f, Math.min((darkness + 0.5f) / 4f, 1f));
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                }
            }

            Draw.flush();
            Draw.color();
            dark.end();
        });

        Events.on(BuildinghangeEvent.class, event -> {
            shadowEvents.add(event.tile);

            int avgx = (int)(camera.position.x / tilesize);
            int avgy = (int)(camera.position.y / tilesize);
            int rangex = (int)(camera.width / tilesize / 2) + 2;
            int rangey = (int)(camera.height / tilesize / 2) + 2;

            if(Math.abs(avgx - event.tile.x) <= rangex && Math.abs(avgy - event.tile.y) <= rangey){
                lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
            }
        });
    }

    public void drawDarkness(){
        Draw.shader(Shaders.darkness);
        Draw.fbo(dark, world.width(), world.height(), tilesize);
        Draw.shader();
    }

    public void drawDestroyed(){
        if(!Core.settings.getBool("destroyedblocks")) return;

        if(control.input.isPlacing() || control.input.isBreaking()){
            brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        }else{
            brokenFade = Mathf.lerpDelta(brokenFade, 0f, 0.1f);
        }

        if(brokenFade > 0.001f){
            for(BlockPlan block : state.teams.get(player.team()).blocks){
                Block b = content.block(block.block);
                if(!camera.bounds(Tmp.r1).grow(tilesize * 2f).overlaps(Tmp.r2.setSize(b.size * tilesize).setCenter(block.x * tilesize + b.offset, block.y * tilesize + b.offset))) continue;

                Draw.alpha(0.33f * brokenFade);
                Draw.mixcol(Color.white, 0.2f + Mathf.absin(Time.globalTime(), 6f, 0.2f));
                Draw.rect(b.icon(Cicon.full), block.x * tilesize + b.offset, block.y * tilesize + b.offset, b.rotate ? block.rotation * 90 : 0f);
            }
            Draw.reset();
        }
    }

    public void drawShadows(){
        if(!shadowEvents.isEmpty()){
            Draw.flush();

            shadows.begin();
            Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

            for(Tile tile : shadowEvents){
                //clear it first
                Draw.color(Color.white);
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                //then draw the shadow
                Draw.color(!tile.block().hasShadow ? Color.white : shadowColor);
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }

            Draw.flush();
            Draw.color();
            shadows.end();
            shadowEvents.clear();

            Draw.proj(camera);
        }

        float ww = world.width() * tilesize, wh = world.height() * tilesize;
        float x = camera.position.x + tilesize / 2f, y = camera.position.y + tilesize / 2f;
        float u = (x - camera.width / 2f) / ww,
        v = (y - camera.height / 2f) / wh,
        u2 = (x + camera.width / 2f) / ww,
        v2 = (y + camera.height / 2f) / wh;

        Tmp.tr1.set(shadows.getTexture());
        Tmp.tr1.set(u, v2, u2, v);

        Draw.shader(Shaders.darkness);
        Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
        Draw.shader();
    }

    /** Process all blocks to draw. */
    public void processBlocks(){
        displayStatus = Core.settings.getBool("blockstatus");

        int avgx = (int)(camera.position.x / tilesize);
        int avgy = (int)(camera.position.y / tilesize);

        int rangex = (int)(camera.width / tilesize / 2) + 3;
        int rangey = (int)(camera.height / tilesize / 2) + 3;

        if(avgx == lastCamX && avgy == lastCamY && lastRangeX == rangex && lastRangeY == rangey){
            return;
        }

        tileview.clear();
        lightview.clear();
        processedEntities.clear();

        int minx = Math.max(avgx - rangex - expandr, 0);
        int miny = Math.max(avgy - rangey - expandr, 0);
        int maxx = Math.min(world.width() - 1, avgx + rangex + expandr);
        int maxy = Math.min(world.height() - 1, avgy + rangey + expandr);

        for(int x = minx; x <= maxx; x++){
            for(int y = miny; y <= maxy; y++){
                boolean expanded = (Math.abs(x - avgx) > rangex || Math.abs(y - avgy) > rangey);
                Tile tile = world.rawTile(x, y);
                Block block = tile.block();
                //link to center
                if(tile.build != null) tile = tile.build.tile();

                if(block != Blocks.air && block.cacheLayer == CacheLayer.normal && (tile.build == null || !processedEntities.contains(tile.build.id()))){
                    if(block.expanded || !expanded){
                        tileview.add(tile);
                        if(tile.build != null) processedEntities.add(tile.build.id());
                    }

                    //lights are drawn even in the expanded range
                    if(tile.build != null || tile.block().emitLight){
                        lightview.add(tile);
                    }

                    if(tile.build != null && tile.build.power != null && tile.build.power.links.size > 0){
                        for(Building other : tile.build.getPowerConnections(outArray2)){
                            if(other.block() instanceof PowerNode){ //TODO need a generic way to render connections!
                                tileview.add(other.tile());
                            }
                        }
                    }
                }

                //special case for floors
                if(block == Blocks.air && tile.floor().emitLight){
                    lightview.add(tile);
                }
            }
        }

        lastCamX = avgx;
        lastCamY = avgy;
        lastRangeX = rangex;
        lastRangeY = rangey;
    }

    public void drawBlocks(){
        drawDestroyed();

        //draw most tile stuff
        for(int i = 0; i < tileview.size; i++){
            Tile tile = tileview.items[i];
            Block block = tile.block();
            Building entity = tile.build;

            Draw.z(Layer.block);

            if(block != Blocks.air){
                block.drawBase(tile);
                Draw.reset();
                Draw.z(Layer.block);

                if(entity != null){
                    if(entity.damaged()){
                        entity.drawCracks();
                        Draw.z(Layer.block);
                    }

                    if(entity.team() != player.team()){
                        entity.drawTeam();
                        Draw.z(Layer.block);
                    }

                    if(displayStatus && block.consumes.any()){
                        entity.drawStatus();
                    }
                }
                Draw.reset();
            }
        }

        if(renderer.lights.enabled()){
            //draw lights
            for(int i = 0; i < lightview.size; i++){
                Tile tile = lightview.items[i];
                Building entity = tile.build;

                if(entity != null){
                    entity.drawLight();
                }else if(tile.block().emitLight){
                    tile.block().drawEnvironmentLight(tile);
                }else if(tile.floor().emitLight){
                    tile.floor().drawEnvironmentLight(tile);
                }
            }
        }


    }

    @Override
    public void dispose(){
        shadows.dispose();
        dark.dispose();
        shadows = dark = null;
        floor.dispose();
    }
}
