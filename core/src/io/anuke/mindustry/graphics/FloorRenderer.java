package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.camera;

public class FloorRenderer {
    private final static int chunksize = 32;

    private int[][][] cache;
    private CacheBatch cbatch;

    public FloorRenderer(){

    }

    public void drawFloor(){
        int chunksx = world.width() / chunksize, chunksy = world.height() / chunksize;

        //render the entire map
        if(cache == null || cache.length != chunksx || cache[0].length != chunksy){
            cache = new int[chunksx][chunksy][DrawLayer.values().length];

            Timers.markNs();

            for(DrawLayer layer : DrawLayer.values()){
                for(int x = 0; x < chunksx; x++){
                    for(int y = 0; y < chunksy; y++){
                        cacheChunk(x, y, layer);
                    }
                }
            }

            Log.info("CACHING ELAPSED: {0}", Timers.elapsedNs());
        }

        OrthographicCamera camera = Core.camera;

        if(Graphics.drawing()) Graphics.end();

        int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
        int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;

        DrawLayer[] layers = DrawLayer.values();

        for(int i = 0; i < layers.length - 1; i ++) {
            drawCache(layers[i], crangex, crangey);
        }

        Graphics.begin();

        Draw.reset();

        if(debug && debugChunks){
            Draw.color(Color.YELLOW);
            Lines.stroke(1f);
            for(int x = -crangex; x <= crangex; x++){
                for(int y = -crangey; y <= crangey; y++){
                    int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                    int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                    if(!Mathf.inBounds(worldx, worldy, cache))
                        continue;
                    Lines.rect(worldx * chunksize * tilesize, worldy * chunksize * tilesize, chunksize * tilesize, chunksize * tilesize);
                }
            }
            Draw.reset();
        }
    }

    void drawCache(DrawLayer layer, int crangex, int crangey){

        Gdx.gl.glEnable(GL20.GL_BLEND);

        layer.begin(cbatch);

        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Mathf.inBounds(worldx, worldy, cache))
                    continue;

                cbatch.drawCache(cache[worldx][worldy][layer.ordinal()]);
            }
        }

        layer.end(cbatch);
    }

    void cacheChunk(int cx, int cy, DrawLayer layer){
        if(cbatch == null){
            createBatch();
        }

        cbatch.begin();
        Graphics.useBatch(cbatch);

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex, tiley);
                if(tile == null) continue;

                if(tile.floor().drawLayer == layer && tile.block().drawLayer != DrawLayer.walls){
                    tile.floor().draw(tile);
                }else if(tile.floor().drawLayer.ordinal() < layer.ordinal() && tile.block().drawLayer != DrawLayer.walls){
                    tile.floor().drawNonLayer(tile);
                }

                if(tile.block().drawLayer == layer && layer == DrawLayer.walls){
                    tile.block().draw(tile);
                }
            }
        }
        Graphics.popBatch();
        cbatch.end();
        cache[cx][cy][layer.ordinal()] = cbatch.getLastCache();
    }

    public void clearTiles(){
        cache = null;
        createBatch();
    }

    private void createBatch(){
        if(cbatch != null)
            cbatch.dispose();
        cbatch = new CacheBatch(world.width() * world.height() * 4);
    }
}
