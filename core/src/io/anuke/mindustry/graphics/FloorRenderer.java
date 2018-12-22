package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.GL20;
import io.anuke.arc.graphics.OrthographicCamera;
import io.anuke.arc.util.IntArray;
import io.anuke.arc.util.IntSet;
import io.anuke.arc.util.IntSet.IntSetIterator;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.arc.Events;
import io.anuke.arc.Graphics;
import io.anuke.arc.Timers;
import io.anuke.arc.graphics.CacheBatch;
import io.anuke.arc.graphics.Draw;
import io.anuke.arc.graphics.Fill;
import io.anuke.arc.util.Log;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Structs;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class FloorRenderer{
    private final static int chunksize = 64;

    private Chunk[][] cache;
    private CacheBatch cbatch;
    private IntSet drawnLayerSet = new IntSet();
    private IntArray drawnLayers = new IntArray();

    public FloorRenderer(){
        Events.on(WorldLoadGraphicsEvent.class, event -> clearTiles());
    }

    public void drawFloor(){
        if(cache == null){
            return;
        }

        OrthographicCamera camera = Core.camera;

        int crangex = (int) (camera.viewportWidth * camera.zoom / (chunksize * tilesize)) + 1;
        int crangey = (int) (camera.viewportHeight * camera.zoom / (chunksize * tilesize)) + 1;

        int camx = Mathf.scl(camera.position.x, chunksize * tilesize);
        int camy = Mathf.scl(camera.position.y, chunksize * tilesize);

        int layers = CacheLayer.values().length;

        drawnLayers.clear();
        drawnLayerSet.clear();

        //preliminary layer check
        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = camx + x;
                int worldy = camy + y;

                if(!Structs.inBounds(worldx, worldy, cache))
                    continue;

                Chunk chunk = cache[worldx][worldy];

                //loop through all layers, and add layer index if it exists
                for(int i = 0; i < layers; i++){
                    if(chunk.caches[i] != -1){
                        drawnLayerSet.add(i);
                    }
                }
            }
        }

        IntSetIterator it = drawnLayerSet.iterator();
        while(it.hasNext){
            drawnLayers.add(it.next());
        }

        drawnLayers.sort();

        Graphics.end();
        beginDraw();

        for(int i = 0; i < drawnLayers.size; i++){
            CacheLayer layer = CacheLayer.values()[drawnLayers.get(i)];

            drawLayer(layer);
        }

        endDraw();
        Graphics.begin();
    }

    public void beginDraw(){
        if(cache == null){
            return;
        }

        cbatch.setProjectionMatrix(Core.camera.combined);
        cbatch.beginDraw();

        Core.gl.glEnable(GL20.GL_BLEND);
    }

    public void endDraw(){
        if(cache == null){
            return;
        }

        cbatch.endDraw();
    }

    public void drawLayer(CacheLayer layer){
        if(cache == null){
            return;
        }

        OrthographicCamera camera = Core.camera;

        int crangex = (int) (camera.viewportWidth * camera.zoom / (chunksize * tilesize)) + 1;
        int crangey = (int) (camera.viewportHeight * camera.zoom / (chunksize * tilesize)) + 1;

        layer.begin();

        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Structs.inBounds(worldx, worldy, cache)){
                    continue;
                }

                Chunk chunk = cache[worldx][worldy];
                if(chunk.caches[layer.ordinal()] == -1) continue;
                cbatch.drawCache(chunk.caches[layer.ordinal()]);
            }
        }

        layer.end();
    }

    private void fillChunk(float x, float y){
        Draw.color(Color.BLACK);
        Fill.crect(x, y, chunksize * tilesize, chunksize * tilesize);
        Draw.color();
    }

    private void cacheChunk(int cx, int cy){
        Chunk chunk = cache[cx][cy];

        ObjectSet<CacheLayer> used = new ObjectSet<>();

        Sector sector = world.getSector();

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex, tiley);

                if(tile != null){
                    used.add(tile.floor().cacheLayer);
                }
            }
        }

        for(CacheLayer layer : used){
            cacheChunkLayer(cx, cy, chunk, layer);
        }
    }

    private void cacheChunkLayer(int cx, int cy, Chunk chunk, CacheLayer layer){

        Graphics.useBatch(cbatch);
        cbatch.begin();

        Sector sector = world.getSector();

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex , tiley);
                Floor floor;

                if(tile == null){
                    continue;
                }else{
                    floor = tile.floor();
                }

                if(floor.cacheLayer == layer){
                    floor.draw(tile);
                }else if(floor.cacheLayer.ordinal() < layer.ordinal()){
                    floor.drawNonLayer(tile);
                }
            }
        }

        cbatch.end();
        Graphics.popBatch();
        chunk.caches[layer.ordinal()] = cbatch.getLastCache();
    }

    public void clearTiles(){
        if(cbatch != null) cbatch.dispose();

        int chunksx = Mathf.ceil((float) (world.width()) / chunksize),
            chunksy = Mathf.ceil((float) (world.height()) / chunksize) ;
        cache = new Chunk[chunksx][chunksy];
        cbatch = new CacheBatch(world.width() * world.height() * 4 * 4);

        Time.mark();

        for(int x = 0; x < chunksx; x++){
            for(int y = 0; y < chunksy; y++){
                cache[x][y] = new Chunk();
                Arrays.fill(cache[x][y].caches, -1);

                cacheChunk(x, y);
            }
        }

        Log.info("Time to cache: {0}", Time.elapsed());
    }

    private class Chunk{
        int[] caches = new int[CacheLayer.values().length];
    }
}
