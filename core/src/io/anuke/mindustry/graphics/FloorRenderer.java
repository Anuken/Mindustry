package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.IntArray;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.collection.IntSet.IntSetIterator;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.Camera;
import io.anuke.arc.graphics.GL20;
import io.anuke.arc.graphics.g2d.CacheBatch;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.SpriteBatch;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class FloorRenderer{
    private final static int chunksize = 64;

    private Chunk[][] cache;
    private CacheBatch cbatch;
    private IntSet drawnLayerSet = new IntSet();
    private IntArray drawnLayers = new IntArray();

    public FloorRenderer(){
        Events.on(WorldLoadEvent.class, event -> clearTiles());
    }

    public void drawFloor(){
        if(cache == null){
            return;
        }

        Camera camera = Core.camera;

        int crangex = (int) (camera.width  / (chunksize * tilesize)) + 1;
        int crangey = (int) (camera.height  / (chunksize * tilesize)) + 1;

        int camx = (int)(camera.position.x / (chunksize * tilesize));
        int camy = (int)(camera.position.y / (chunksize * tilesize));

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

        Draw.flush();
        beginDraw();

        for(int i = 0; i < drawnLayers.size; i++){
            CacheLayer layer = CacheLayer.values()[drawnLayers.get(i)];

            drawLayer(layer);
        }

        endDraw();
    }

    public void beginDraw(){
        if(cache == null){
            return;
        }

        cbatch.setProjection(Core.camera.projection());
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

        Camera camera = Core.camera;

        int crangex = (int) (camera.width  / (chunksize * tilesize)) + 1;
        int crangey = (int) (camera.height  / (chunksize * tilesize)) + 1;

        layer.begin();

        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = (int)(camera.position.x / (chunksize * tilesize)) + x;
                int worldy = (int)(camera.position.y / (chunksize * tilesize)) + y;

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

    private void cacheChunk(int cx, int cy){
        Chunk chunk = cache[cx][cy];

        ObjectSet<CacheLayer> used = new ObjectSet<>();

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
        SpriteBatch current = Core.batch;
        Core.batch = cbatch;

        cbatch.beginCache();

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
        Core.batch = current;
        chunk.caches[layer.ordinal()] = cbatch.endCache();
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
