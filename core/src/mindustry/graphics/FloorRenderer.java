package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.struct.IntSet.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import java.util.*;

import static mindustry.Vars.*;

public class FloorRenderer implements Disposable{
    //TODO find out number with best performance
    private static final int chunksize = mobile ? 16 : 32;

    private Chunk[][] cache;
    private MultiCacheBatch cbatch;
    private IntSet drawnLayerSet = new IntSet();
    private IntSet recacheSet = new IntSet();
    private IntSeq drawnLayers = new IntSeq();
    private ObjectSet<CacheLayer> used = new ObjectSet<>();

    public FloorRenderer(){
        Events.on(WorldLoadEvent.class, event -> clearTiles());
    }

    /**Queues up a cache change for a tile. Only runs in render loop. */
    public void recacheTile(Tile tile){
        recacheSet.add(Point2.pack(tile.x / chunksize, tile.y / chunksize));
    }

    public void drawFloor(){
        if(cache == null){
            return;
        }

        Camera camera = Core.camera;

        int crangex = (int)(camera.width / (chunksize * tilesize)) + 1;
        int crangey = (int)(camera.height / (chunksize * tilesize)) + 1;

        int camx = (int)(camera.position.x / (chunksize * tilesize));
        int camy = (int)(camera.position.y / (chunksize * tilesize));

        int layers = CacheLayer.all.length;

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
                    if(chunk.caches[i] != -1 && i != CacheLayer.walls.ordinal()){
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
            CacheLayer layer = CacheLayer.all[drawnLayers.get(i)];

            drawLayer(layer);
        }

        endDraw();
    }

    public void beginc(){
        cbatch.beginDraw();
    }

    public void endc(){
        cbatch.endDraw();
    }

    public void checkChanges(){
        if(recacheSet.size > 0){
            //recache one chunk at a time
            IntSetIterator iterator = recacheSet.iterator();
            while(iterator.hasNext){
                int chunk = iterator.next();
                cacheChunk(Point2.x(chunk), Point2.y(chunk));
            }

            recacheSet.clear();
        }
    }

    public void beginDraw(){
        if(cache == null){
            return;
        }

        cbatch.setProjection(Core.camera.mat);
        cbatch.beginDraw();

        Gl.enable(Gl.blend);
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

        int crangex = (int)(camera.width / (chunksize * tilesize)) + 1;
        int crangey = (int)(camera.height / (chunksize * tilesize)) + 1;

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
        used.clear();
        Chunk chunk = cache[cx][cy];

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize && tilex < world.width(); tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize && tiley < world.height(); tiley++){
                Tile tile = world.rawTile(tilex, tiley);

                if(tile.block().cacheLayer != CacheLayer.normal){
                    used.add(tile.block().cacheLayer);
                }else{
                    used.add(tile.floor().cacheLayer);
                }
            }
        }

        for(CacheLayer layer : used){
            cacheChunkLayer(cx, cy, chunk, layer);
        }
    }

    private void cacheChunkLayer(int cx, int cy, Chunk chunk, CacheLayer layer){
        Batch current = Core.batch;
        Core.batch = cbatch;

        //begin a new cache
        if(chunk.caches[layer.ordinal()] == -1){
            cbatch.beginCache();
        }else{
            cbatch.beginCache(chunk.caches[layer.ordinal()]);
        }

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex, tiley);
                Floor floor;

                if(tile == null){
                    continue;
                }else{
                    floor = tile.floor();
                }

                if(tile.block().cacheLayer == layer && layer == CacheLayer.walls && !(tile.isDarkened() && tile.data >= 5)){
                    tile.block().drawBase(tile);
                }else if(floor.cacheLayer == layer && (world.isAccessible(tile.x, tile.y) || tile.block().cacheLayer != CacheLayer.walls || !tile.block().fillsTile)){
                    floor.drawBase(tile);
                }else if(floor.cacheLayer.ordinal() < layer.ordinal() && layer != CacheLayer.walls){
                    floor.drawNonLayer(tile);
                }
            }
        }

        Core.batch = current;
        cbatch.reserve(layer.capacity * chunksize * chunksize);
        chunk.caches[layer.ordinal()] = cbatch.endCache();
    }

    public void clearTiles(){
        if(cbatch != null) cbatch.dispose();

        recacheSet.clear();
        int chunksx = Mathf.ceil((float)(world.width()) / chunksize),
        chunksy = Mathf.ceil((float)(world.height()) / chunksize);
        cache = new Chunk[chunksx][chunksy];
        cbatch = new MultiCacheBatch(chunksize * chunksize * 5);

        Time.mark();

        for(int x = 0; x < chunksx; x++){
            for(int y = 0; y < chunksy; y++){
                cache[x][y] = new Chunk();
                Arrays.fill(cache[x][y].caches, -1);

                cacheChunk(x, y);
            }
        }

        Log.debug("Time to cache: @", Time.elapsed());
    }

    @Override
    public void dispose(){
        if(cbatch != null){
            cbatch.dispose();
            cbatch = null;
        }
    }

    private static class Chunk{
        /** Maps cache layer ID to cache ID in the batch.
         * -1 means that this cache is unoccupied. */
        int[] caches = new int[CacheLayer.all.length];
    }
}
