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
    private static final int chunksize = mobile ? 16 : 32, chunkunits = chunksize * tilesize;
    private static final float pad = tilesize/2f;

    private int[][][] cache;
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
        //currently a no-op
        //recacheSet.add(Point2.pack(tile.x / chunksize, tile.y / chunksize));
    }

    public void drawFloor(){
        if(cache == null){
            return;
        }

        Camera camera = Core.camera;

        int
            minx = (int)((camera.position.x - camera.width/2f - pad) / chunkunits),
            miny = (int)((camera.position.y - camera.height/2f - pad) / chunkunits),
            maxx = Mathf.ceil((camera.position.x + camera.width/2f + pad) / chunkunits),
            maxy = Mathf.ceil((camera.position.y + camera.height/2f + pad) / chunkunits);

        int layers = CacheLayer.all.length;

        drawnLayers.clear();
        drawnLayerSet.clear();

        //preliminary layer check
        for(int x = minx; x <= maxx; x++){
            for(int y = miny; y <= maxy; y++){

                if(!Structs.inBounds(x, y, cache)) continue;

                int[] chunk = cache[x][y];

                //loop through all layers, and add layer index if it exists
                for(int i = 0; i < layers; i++){
                    if(chunk[i] != -1 && i != CacheLayer.walls.ordinal()){
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

        Draw.flush();
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

        int
            minx = (int)((camera.position.x - camera.width/2f - pad) / chunkunits),
            miny = (int)((camera.position.y - camera.height/2f - pad) / chunkunits),
            maxx = Mathf.ceil((camera.position.x + camera.width/2f + pad) / chunkunits),
            maxy = Mathf.ceil((camera.position.y + camera.height/2f + pad) / chunkunits);

        layer.begin();

        for(int x = minx; x <= maxx; x++){
            for(int y = miny; y <= maxy; y++){

                if(!Structs.inBounds(x, y, cache)){
                    continue;
                }

                int[] chunk = cache[x][y];
                if(chunk[layer.ordinal()] == -1) continue;
                cbatch.drawCache(chunk[layer.ordinal()]);
            }
        }

        layer.end();
    }

    private void cacheChunk(int cx, int cy){
        used.clear();
        int[] chunk = cache[cx][cy];

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

    private void cacheChunkLayer(int cx, int cy, int[] chunk, CacheLayer layer){
        Batch current = Core.batch;
        Core.batch = cbatch;

        //begin a new cache
        if(chunk[layer.ordinal()] == -1){
            cbatch.beginCache();
        }else{
            cbatch.beginCache(chunk[layer.ordinal()]);
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
                }else if(floor.cacheLayer != layer && layer != CacheLayer.walls){
                    floor.drawNonLayer(tile, layer);
                }
            }
        }

        Core.batch = current;
        chunk[layer.ordinal()] = cbatch.endCache();
    }

    public void clearTiles(){
        if(cbatch != null) cbatch.dispose();

        recacheSet.clear();
        int chunksx = Mathf.ceil((float)(world.width()) / chunksize),
        chunksy = Mathf.ceil((float)(world.height()) / chunksize);
        cache = new int[chunksx][chunksy][CacheLayer.all.length];
        cbatch = new MultiCacheBatch(chunksize * chunksize * 9);

        Time.mark();

        for(int x = 0; x < chunksx; x++){
            for(int y = 0; y < chunksy; y++){
                Arrays.fill(cache[x][y], -1);

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
}
