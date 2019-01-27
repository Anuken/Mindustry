package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.Sort;
import io.anuke.arc.entities.EntityDraw;
import io.anuke.arc.entities.EntityGroup;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Texture.TextureFilter;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.graphics.glutils.FrameBuffer;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.arc.Core.camera;
import static io.anuke.mindustry.Vars.*;

public class BlockRenderer{
    private final static int initialRequests = 32 * 32;
    private final static int expandr = 6;
    private final static boolean disableShadows = false;
    private final static Color shadowColor = new Color(0, 0, 0, 0.19f);

    public final FloorRenderer floor = new FloorRenderer();

    private Array<BlockRequest> requests = new Array<>(true, initialRequests, BlockRequest.class);
    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private int requestidx = 0;
    private int iterateidx = 0;
    private FrameBuffer shadows = new FrameBuffer(2, 2);
    private FrameBuffer fog = new FrameBuffer(2, 2);

    public BlockRenderer(){

        for(int i = 0; i < requests.size; i++){
            requests.set(i, new BlockRequest());
        }

        Events.on(WorldLoadEvent.class, event -> {
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated

            fog.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            fog.resize(world.width(), world.height());
            fog.begin();
            Core.graphics.clear(Color.WHITE);
            Draw.proj().setOrtho(0, 0, fog.getWidth(), fog.getHeight());

            //TODO highly inefficient, width*height rectangles isn't great
            //TODO handle shadows with GPU
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile tile = world.rawTile(x, y);
                    if(tile.getRotation() > 0){
                        Draw.color(0f, 0f, 0f, Math.min((tile.getRotation() + 0.5f)/4f, 1f));
                        Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                    }
                }
            }

            Draw.flush();
            Draw.color();
            fog.end();
        });

        Events.on(TileChangeEvent.class, event -> {
            int avgx = (int)(camera.position.x / tilesize);
            int avgy = (int)(camera.position. y/ tilesize);
            int rangex = (int) (camera.width  / tilesize / 2) + 2;
            int rangey = (int) (camera.height  / tilesize / 2) + 2;

            if(Math.abs(avgx - event.tile.x) <= rangex && Math.abs(avgy - event.tile.y) <= rangey){
                lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
            }
        });
    }

    public void drawFog(){
        float ww = world.width() * tilesize, wh = world.height() * tilesize;
        float u = (camera.position.x - camera.width/2f) / ww,
        v = (camera.position.y - camera.height/2f) / wh,
        u2 = (camera.position.x + camera.width/2f) / ww,
        v2 = (camera.position.y + camera.height/2f) / wh;

        Tmp.tr1.set(fog.getTexture());
        Tmp.tr1.set(u, v2, u2, v);

        Draw.shader(Shaders.fog);
        Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
        Draw.shader();
    }

    public void drawShadows(){
        if(disableShadows) return;

        if(!Core.graphics.isHidden() && (shadows.getWidth() != Core.graphics.getWidth() || shadows.getHeight() != Core.graphics.getHeight())){
            shadows.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        }

        Tmp.tr1.set(shadows.getTexture());
        Shaders.outline.color.set(shadowColor);
        Shaders.outline.scl = renderer.cameraScale()/3f;
        Shaders.outline.region = Tmp.tr1;

        Draw.flush();
        shadows.begin();
        Core.graphics.clear(Color.CLEAR);
        Draw.color(shadowColor);
        floor.beginDraw();
        floor.drawLayer(CacheLayer.walls);
        floor.endDraw();
        drawBlocks(Layer.shadow);

        EntityDraw.drawWith(playerGroup, player -> !player.isDead(), Unit::draw);
        for(EntityGroup group : unitGroups){
            EntityDraw.drawWith(group, unit -> !unit.isDead(), Unit::draw);
        }

        Draw.color();
        Draw.flush();
        shadows.end();

        Draw.shader(Shaders.outline);
        Draw.rect(Draw.wrap(shadows.getTexture()),
            camera.position.x,
            camera.position.y,
            camera.width, -camera.height);
        Draw.shader();
    }

    /**Process all blocks to draw.*/
    public void processBlocks(){
        iterateidx = 0;

        int avgx = (int)(camera.position.x / tilesize);
        int avgy = (int)(camera.position.y / tilesize);

        int rangex = (int) (camera.width / tilesize / 2) + 3;
        int rangey = (int) (camera.height / tilesize / 2) + 3;

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
                Block block = tile.block();

                if(block != Blocks.air && block.cacheLayer == CacheLayer.normal){
                    if(!expanded){
                        addRequest(tile, Layer.shadow);
                        addRequest(tile, Layer.block);
                    }

                    if(block.expanded || !expanded){
                        if(block.layer != null && block.isLayer(tile)){
                            addRequest(tile, block.layer);
                        }

                        if(block.layer2 != null && block.isLayer2(tile)){
                            addRequest(tile, block.layer2);
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

        floor.beginDraw();
        floor.drawLayer(CacheLayer.walls);
        floor.endDraw();
    }

    public void drawBlocks(Layer stopAt){

        for(; iterateidx < requestidx; iterateidx++){

            if(iterateidx < requests.size && requests.get(iterateidx).layer.ordinal() > stopAt.ordinal()){
                break;
            }

            BlockRequest req = requests.get(iterateidx);
            Block block = req.tile.block();

            if(req.layer == Layer.shadow){
                block.drawShadow(req.tile);
            }else if(req.layer == Layer.block){
                block.draw(req.tile);
                if(block.synthetic() && req.tile.getTeam() != players[0].getTeam()){
                    Draw.color(req.tile.getTeam().color);
                    Draw.rect("block-border", req.tile.drawx() - block.size * tilesize/2f + 4, req.tile.drawy() - block.size * tilesize/2f + 4);
                    Draw.color();
                }
            }else if(req.layer == block.layer){
                block.drawLayer(req.tile);
            }else if(req.layer == block.layer2){
                block.drawLayer2(req.tile);
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

    private class BlockRequest implements Comparable<BlockRequest>{
        Tile tile;
        Layer layer;

        @Override
        public int compareTo(BlockRequest other){
            return layer.compareTo(other.layer);
        }

        @Override
        public String toString(){
            return tile.block().name + ":" + layer.toString();
        }
    }
}
