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
    private final static int initialRequests = 32 * 32;
    private final static int expandr = 9;
    private final static Color shadowColor = new Color(0, 0, 0, 0.71f);

    public final FloorRenderer floor = new FloorRenderer();

    private Array<Tile> requests = new Array<>(false, initialRequests, Tile.class);

    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private float brokenFade = 0f;
    private FrameBuffer shadows = new FrameBuffer();
    private FrameBuffer fog = new FrameBuffer();
    private Array<Tilec> outArray2 = new Array<>();
    private Array<Tile> shadowEvents = new Array<>();
    private boolean displayStatus = false;

    public BlockRenderer(){

        Events.on(WorldLoadEvent.class, event -> {
            shadowEvents.clear();
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated

            shadows.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
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

            fog.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            fog.resize(world.width(), world.height());
            fog.begin();
            Core.graphics.clear(Color.white);
            Draw.proj().setOrtho(0, 0, fog.getWidth(), fog.getHeight());

            for(Tile tile : world.tiles){
                float darkness = world.getDarkness(tile.x, tile.y);

                if(darkness > 0){
                    Draw.color(0f, 0f, 0f, Math.min((darkness + 0.5f) / 4f, 1f));
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                }
            }

            Draw.flush();
            Draw.color();
            fog.end();
        });

        Events.on(TileChangeEvent.class, event -> {
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
        float ww = world.width() * tilesize, wh = world.height() * tilesize;
        float x = camera.position.x + tilesize / 2f, y = camera.position.y + tilesize / 2f;
        float u = (x - camera.width / 2f) / ww,
        v = (y - camera.height / 2f) / wh,
        u2 = (x + camera.width / 2f) / ww,
        v2 = (y + camera.height / 2f) / wh;

        Tmp.tr1.set(fog.getTexture());
        Tmp.tr1.set(u, v2, u2, v);

        Draw.shader(Shaders.fog);
        Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
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
            for(BrokenBlock block : state.teams.get(player.team()).brokenBlocks){
                Block b = content.block(block.block);
                if(!camera.bounds(Tmp.r1).grow(tilesize * 2f).overlaps(Tmp.r2.setSize(b.size * tilesize).setCenter(block.x * tilesize + b.offset(), block.y * tilesize + b.offset()))) continue;

                Draw.alpha(0.33f * brokenFade);
                Draw.mixcol(Color.white, 0.2f + Mathf.absin(Time.globalTime(), 6f, 0.2f));
                Draw.rect(b.icon(Cicon.full), block.x * tilesize + b.offset(), block.y * tilesize + b.offset(), b.rotate ? block.rotation * 90 : 0f);
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

        Draw.shader(Shaders.fog);
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

        requests.clear();

        int minx = Math.max(avgx - rangex - expandr, 0);
        int miny = Math.max(avgy - rangey - expandr, 0);
        int maxx = Math.min(world.width() - 1, avgx + rangex + expandr);
        int maxy = Math.min(world.height() - 1, avgy + rangey + expandr);

        for(int x = minx; x <= maxx; x++){
            for(int y = miny; y <= maxy; y++){
                boolean expanded = (Math.abs(x - avgx) > rangex || Math.abs(y - avgy) > rangey);
                Tile tile = world.rawTile(x, y);
                Block block = tile.block();

                if(block != Blocks.air && tile.isCenter() && block.cacheLayer == CacheLayer.normal){
                    if(block.expanded || !expanded){
                        requests.add(tile);
                    }

                    if(tile.entity != null && tile.entity.power() != null && tile.entity.power().links.size > 0){
                        for(Tilec other : tile.entity.getPowerConnections(outArray2)){
                            if(other.block() instanceof PowerNode){ //TODO need a generic way to render connections!
                                requests.add(other.tile());
                            }
                        }
                    }
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

        for(int i = 0; i < requests.size; i++){
            Tile tile = requests.items[i];
            Block block = tile.block();
            Tilec entity = tile.entity;

            Draw.z(Layer.block);

            if(block != Blocks.air){
                block.drawBase(tile);

                if(entity != null){
                    if(entity.damaged()){
                        entity.drawCracks();
                    }

                    if(entity.team() != player.team()){
                        entity.drawTeam();
                    }

                    entity.drawLight();

                    if(displayStatus && block.consumes.any()){
                        entity.drawStatus();
                    }
                }
            }
        }
    }

    @Override
    public void dispose(){
        shadows.dispose();
        fog.dispose();
        shadows = fog = null;
        floor.dispose();
    }
}
