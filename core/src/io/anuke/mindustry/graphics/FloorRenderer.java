package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class FloorRenderer {
    private final static int vsize = 5;
    private final static int chunksize = 32;

    private AsyncExecutor executor = new AsyncExecutor(4);
    private Chunk[][] cache;

    public void drawFloor(){
        int chunksx = world.width() / chunksize, chunksy = world.height() / chunksize;

        if(cache == null || cache.length != chunksx || cache[0].length != chunksy){
            cache = new Chunk[chunksx][chunksy];
        }

        OrthographicCamera camera = Core.camera;

        int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
        int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;

        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Mathf.inBounds(worldx, worldy, cache))
                    continue;

                fillChunk(worldx * chunksize * tilesize, worldy * chunksize * tilesize);

                if(cache[worldx][worldy] == null){
                    cache[worldx][worldy] = new Chunk();
                    executor.submit(() -> cacheChunk(worldx, worldy));
                    continue;
                }

                Chunk chunk = cache[worldx][worldy];

                if(!chunk.rendered){
                    continue;
                }

                Core.batch.draw(Core.atlas.getTextures().first(), chunk.vertices, 0, chunk.length);
            }
        }
    }

    private void fillChunk(float x, float y){
        Draw.color(Color.GRAY);
        Draw.crect("white", x, y, chunksize * tilesize, chunksize * tilesize);
        Draw.color();
    }

    private Chunk cacheChunk(int cx, int cy){
        Chunk chunk = cache[cx][cy];
        chunk.vertices = new float[chunksize*chunksize*vsize * 4*6];

        int idx = 0;
        float[] vertices = chunk.vertices;
        float color = NumberUtils.intToFloatColor(Color.WHITE.toIntBits());
        TextureRegion region = new TextureRegion(Core.atlas.getTextures().first());

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex, tiley);
                if(tile == null) continue;

                Block floor = tile.floor();
                if(!Draw.hasRegion(floor.name())) continue;

                TextureRegion base = Draw.region(floor.name());

                set(base, region, 1 + Mathf.random(1), 1 + Mathf.random(1));

                idx = draw(vertices, idx, region, tile.worldx(), tile.worldy(), color);

                for(int dx = -1; dx <= 1; dx ++) {
                    for (int dy = -1; dy <= 1; dy++) {

                        if (dx == 0 && dy == 0) continue;

                        Tile other = world.tile(tile.x + dx, tile.y + dy);

                        if (other == null) continue;

                        Block of = other.floor();

                        if(of.id < floor.id){
                            float ox = (dx == 0 ? Mathf.range(0.5f) : 0);
                            float oy = (dy == 0 ? Mathf.range(0.5f) : 0);
                            set(base, region, (int)(1.5f + 2f*dx + ox), (int)(2f - 2f*dy + oy));

                            idx = draw(vertices, idx, region,
                                    tile.worldx() + dx * tilesize,
                                    tile.worldy() + dy * tilesize, color);
                        }
                    }
                }
            }
        }

        chunk.length = idx;
        chunk.rendered = true;
        return chunk;
    }

    private void set(TextureRegion base, TextureRegion region, int x, int y){
        x = Mathf.clamp(x, 0, 3);
        y = Mathf.clamp(y, 0, 3);
        region.setRegion(base.getRegionX() + x *8, base.getRegionY() + y *8, 8, 8);
    }

    private int draw(float[] vertices, int idx, TextureRegion region, float x, float y, float color){
        x -= tilesize/2f;
        y -= tilesize/2f;
        float width = tilesize, height = tilesize;

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        vertices[idx ++] = x;
        vertices[idx ++] = y;
        vertices[idx ++] = color;
        vertices[idx ++] = u;
        vertices[idx ++] = v;

        vertices[idx ++] = x;
        vertices[idx ++] = fy2;
        vertices[idx ++] = color;
        vertices[idx ++] = u;
        vertices[idx ++] = v2;

        vertices[idx ++] = fx2;
        vertices[idx ++] = fy2;
        vertices[idx ++] = color;
        vertices[idx ++] = u2;
        vertices[idx ++] = v2;

        vertices[idx ++] = fx2;
        vertices[idx ++] = y;
        vertices[idx ++] = color;
        vertices[idx ++] = u2;
        vertices[idx ++] = v;

        return idx;
    }

    private class Chunk{
        float[] vertices;
        boolean rendered;
        int length;
    }

    public void clearTiles(){
        cache = null;
    }
}
