package io.anuke.mindustry.graphics;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Texture.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.glutils.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Teams.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;

import static io.anuke.arc.Core.camera;
import static io.anuke.mindustry.Vars.*;

public class BlockRenderer implements Disposable{
    private final static int initialRequests = 32 * 32;
    private final static int expandr = 9;
    private final static Color shadowColor = new Color(0, 0, 0, 0.71f);

    public final FloorRenderer floor = new FloorRenderer();

    private Array<BlockRequest> requests = new Array<>(true, initialRequests, BlockRequest.class);
    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private int requestidx = 0;
    private int iterateidx = 0;
    private float brokenFade = 0f;
    private FrameBuffer shadows = new FrameBuffer(2, 2);
    private FrameBuffer fog = new FrameBuffer(2, 2);
    private Array<Tile> outArray = new Array<>();
    private Array<Tile> shadowEvents = new Array<>();

    public BlockRenderer(){

        for(int i = 0; i < requests.size; i++){
            requests.set(i, new BlockRequest());
        }

        Events.on(WorldLoadEvent.class, event -> {
            shadowEvents.clear();
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated

            shadows.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            shadows.resize(world.width(), world.height());
            shadows.begin();
            Core.graphics.clear(Color.white);
            Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

            Draw.color(shadowColor);

            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile tile = world.rawTile(x, y);
                    if(tile.block().hasShadow){
                        Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                    }
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

            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile tile = world.rawTile(x, y);
                    int edgeBlend = 2;
                    float rot = tile.rotation();
                    boolean fillable = (tile.block().solid && tile.block().fillsTile && !tile.block().synthetic());
                    int edgeDst = Math.min(x, Math.min(y, Math.min(Math.abs(x - (world.width() - 1)), Math.abs(y - (world.height() - 1)))));
                    if(edgeDst <= edgeBlend){
                        rot = Math.max((edgeBlend - edgeDst) * (4f / edgeBlend), fillable ? rot : 0);
                    }
                    if(rot > 0 && (fillable || edgeDst <= edgeBlend)){
                        Draw.color(0f, 0f, 0f, Math.min((rot + 0.5f) / 4f, 1f));
                        Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                    }
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

    public void drawFog(){
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

    public void drawBroken(){
        if(control.input.isPlacing() || control.input.isBreaking()){
            brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        }else{
            brokenFade = Mathf.lerpDelta(brokenFade, 0f, 0.1f);
        }

        if(brokenFade > 0.001f){
            for(BrokenBlock block : state.teams.get(player.getTeam()).brokenBlocks){
                Block b = content.block(block.block);
                if(!camera.bounds(Tmp.r1).grow(tilesize * 2f).overlaps(Tmp.r2.setSize(b.size * tilesize).setCenter(block.x * tilesize + b.offset(), block.y * tilesize + b.offset()))) continue;

                Draw.alpha(0.53f * brokenFade);
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

            Draw.proj(camera.projection());
            renderer.pixelator.rebind();
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
        iterateidx = 0;

        int avgx = (int)(camera.position.x / tilesize);
        int avgy = (int)(camera.position.y / tilesize);

        int rangex = (int)(camera.width / tilesize / 2) + 3;
        int rangey = (int)(camera.height / tilesize / 2) + 3;

        if(avgx == lastCamX && avgy == lastCamY && lastRangeX == rangex && lastRangeY == rangey){
            return;
        }

        requestidx = 0;

        int minx = Math.max(avgx - rangex - expandr, 0);
        int miny = Math.max(avgy - rangey - expandr, 0);
        int maxx = Math.min(world.width() - 1, avgx + rangex + expandr);
        int maxy = Math.min(world.height() - 1, avgy + rangey + expandr);

        for(int x = minx; x <= maxx; x++){
            for(int y = miny; y <= maxy; y++){
                boolean expanded = (Math.abs(x - avgx) > rangex || Math.abs(y - avgy) > rangey);
                Tile tile = world.rawTile(x, y);
                if(tile == null) continue; //how is this possible?
                Block block = tile.block();

                if(block != Blocks.air && block.cacheLayer == CacheLayer.normal){
                    if(!expanded){
                        addRequest(tile, Layer.block);
                    }

                    if(block.expanded || !expanded){

                        if(block.layer != null){
                            addRequest(tile, block.layer);
                        }

                        if(block.layer2 != null){
                            addRequest(tile, block.layer2);
                        }

                        if(tile.entity != null && tile.entity.power != null && tile.entity.power.links.size > 0){
                            for(Tile other : block.getPowerConnections(tile, outArray)){
                                if(other.block().layer == Layer.power){
                                    addRequest(other, Layer.power);
                                }
                            }
                        }
                    }
                }
            }
        }

        Sort.instance().sort(requests.items, 0, requestidx);

        lastCamX = avgx;
        lastCamY = avgy;
        lastRangeX = rangex;
        lastRangeY = rangey;
    }

    public void drawBlocks(Layer stopAt){
        int startIdx = iterateidx;
        for(; iterateidx < requestidx; iterateidx++){
            BlockRequest request = requests.get(iterateidx);

            if(request.layer.ordinal() > stopAt.ordinal()){
                break;
            }

            if(request.layer == Layer.power){
                if(iterateidx - startIdx > 0 && request.tile.pos() == requests.get(iterateidx - 1).tile.pos()){
                    continue;
                }
            }

            Block block = request.tile.block();

            if(request.layer == Layer.block){
                block.draw(request.tile);
                if(request.tile.entity != null && request.tile.entity.damaged()){
                    block.drawCracks(request.tile);
                }
                if(block.synthetic() && request.tile.getTeam() != player.getTeam()){
                    block.drawTeam(request.tile);
                }
            }else if(request.layer == block.layer){
                block.drawLayer(request.tile);
            }else if(request.layer == block.layer2){
                block.drawLayer2(request.tile);
            }
        }
    }

    public void drawTeamBlocks(Layer layer, Team team){
        int index = this.iterateidx;

        for(; index < requestidx; index++){

            if(index < requests.size && requests.get(index).layer.ordinal() > layer.ordinal()){
                break;
            }

            BlockRequest req = requests.get(index);
            if(req.tile.getTeam() != team) continue;

            Block block = req.tile.block();

            if(req.layer == Layer.block){
                block.draw(req.tile);
            }else if(req.layer == block.layer){
                block.drawLayer(req.tile);
            }else if(req.layer == block.layer2){
                block.drawLayer2(req.tile);
            }

        }
    }

    public void skipLayer(Layer stopAt){
        for(; iterateidx < requestidx; iterateidx++){
            if(iterateidx < requests.size && requests.get(iterateidx).layer.ordinal() > stopAt.ordinal()){
                break;
            }
        }
    }

    private void addRequest(Tile tile, Layer layer){
        if(requestidx >= requests.size){
            requests.add(new BlockRequest());
        }
        BlockRequest r = requests.get(requestidx);
        if(r == null){
            requests.set(requestidx, r = new BlockRequest());
        }
        r.tile = tile;
        r.layer = layer;
        requestidx++;
    }

    @Override
    public void dispose(){
        shadows.dispose();
        fog.dispose();
        shadows = fog = null;
        floor.dispose();
    }

    private class BlockRequest implements Comparable<BlockRequest>{
        Tile tile;
        Layer layer;

        @Override
        public int compareTo(BlockRequest other){
            int compare = layer.compareTo(other.layer);

            return (compare != 0) ? compare : Integer.compare(tile.pos(), other.tile.pos());
        }

        @Override
        public String toString(){
            return tile.block().name + ":" + layer.toString();
        }
    }
}
