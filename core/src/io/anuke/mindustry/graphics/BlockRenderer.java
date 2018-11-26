package io.anuke.mindustry.graphics;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Sort;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Surface;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.camera;

public class BlockRenderer{
    private final static int initialRequests = 32 * 32;
    private final static int expandr = 6;

    private FloorRenderer floorRenderer;

    private Array<BlockRequest> requests = new Array<>(true, initialRequests, BlockRequest.class);
    private IntSet teamChecks = new IntSet();
    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private Layer lastLayer;
    private int requestidx = 0;
    private int iterateidx = 0;
    private Surface shadows = Graphics.createSurface().setSize(2, 2);

    public BlockRenderer(){
        floorRenderer = new FloorRenderer();

        for(int i = 0; i < requests.size; i++){
            requests.set(i, new BlockRequest());
        }

        Events.on(WorldLoadGraphicsEvent.class, event -> {
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
        });

        Events.on(TileChangeEvent.class, event -> {
            threads.runGraphics(() -> {
                int avgx = Mathf.scl(camera.position.x, tilesize);
                int avgy = Mathf.scl(camera.position.y, tilesize);
                int rangex = (int) (camera.viewportWidth * camera.zoom / tilesize / 2) + 2;
                int rangey = (int) (camera.viewportHeight * camera.zoom / tilesize / 2) + 2;

                if(Math.abs(avgx - event.tile.x) <= rangex && Math.abs(avgy - event.tile.y) <= rangey){
                    lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
                }
            });
        });
    }

    public void drawShadows(){
        Draw.color(0, 0, 0, 0.15f);
        Draw.rect(shadows.texture(),
            Core.camera.position.x - Core.camera.position.x % tilesize,
            Core.camera.position.y - Core.camera.position.y % tilesize,
            shadows.width(), -shadows.height());
        Draw.color();
    }

    public boolean isTeamShown(Team team){
        return teamChecks.contains(team.ordinal());
    }

    /**Process all blocks to draw, simultaneously updating the block shadow framebuffer.*/
    public void processBlocks(){
        iterateidx = 0;
        lastLayer = null;

        int avgx = Mathf.scl(camera.position.x, tilesize);
        int avgy = Mathf.scl(camera.position.y, tilesize);

        int rangex = (int) (camera.viewportWidth * camera.zoom / tilesize / 2) + 2;
        int rangey = (int) (camera.viewportHeight * camera.zoom / tilesize / 2) + 2;

        if(avgx == lastCamX && avgy == lastCamY && lastRangeX == rangex && lastRangeY == rangey){
            return;
        }

        int shadowW = rangex * tilesize*2, shadowH = rangey * tilesize*2;

        teamChecks.clear();
        requestidx = 0;

        Graphics.end();
        if(shadows.width() != shadowW || shadows.height() != shadowH){
            shadows.setSize(shadowW, shadowH);
        }
        Core.batch.getProjectionMatrix().setToOrtho2D(Mathf.round(Core.camera.position.x, tilesize)-shadowW/2f, Mathf.round(Core.camera.position.y, tilesize)-shadowH/2f, shadowW, shadowH);
        Graphics.surface(shadows);

        int minx = Math.max(avgx - rangex - expandr, 0);
        int miny = Math.max(avgy - rangey - expandr, 0);
        int maxx = Math.min(world.width() - 1, avgx + rangex + expandr);
        int maxy = Math.min(world.height() - 1, avgy + rangey + expandr);

        for(int x = minx; x <= maxx; x++){
            for(int y = miny; y <= maxy; y++){
                boolean expanded = (Math.abs(x - avgx) > rangex || Math.abs(y - avgy) > rangey);
                Tile tile = world.rawTile(x, y);

                if(tile != null){
                    Block block = tile.block();
                    Team team = tile.getTeam();

                    if(!expanded && block != Blocks.air && world.isAccessible(x, y)){
                        tile.block().drawShadow(tile);
                    }

                    if(block != Blocks.air){
                        if(!expanded){
                            addRequest(tile, Layer.block);
                            teamChecks.add(team.ordinal());
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
        }

        Graphics.surface();
        Graphics.end();
        Core.batch.setProjectionMatrix(camera.combined);
        Graphics.begin();

        Sort.instance().sort(requests.items, 0, requestidx);

        lastCamX = avgx;
        lastCamY = avgy;
        lastRangeX = rangex;
        lastRangeY = rangey;
    }

    public int getRequests(){
        return requestidx;
    }

    public void drawBlocks(Layer stopAt){

        for(; iterateidx < requestidx; iterateidx++){

            if(iterateidx < requests.size && requests.get(iterateidx).layer.ordinal() > stopAt.ordinal()){
                break;
            }

            BlockRequest req = requests.get(iterateidx);

            if(req.layer != lastLayer){
                if(lastLayer != null) layerEnds(lastLayer);
                layerBegins(req.layer);
            }

            Block block = req.tile.block();

            if(req.layer == Layer.block){
                block.draw(req.tile);
            }else if(req.layer == block.layer){
                block.drawLayer(req.tile);
            }else if(req.layer == block.layer2){
                block.drawLayer2(req.tile);
            }

            lastLayer = req.layer;
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

    public void beginFloor(){
        floorRenderer.beginDraw();
    }

    public void endFloor(){
        floorRenderer.endDraw();
    }

    public void drawFloor(){
        floorRenderer.drawFloor();
    }

    private void layerBegins(Layer layer){
    }

    private void layerEnds(Layer layer){
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
