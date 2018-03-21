package io.anuke.mindustry.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import io.anuke.mindustry.io.MapTileData.TileDataWriter;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer {
    private static final int chunksize = 32;
    private CacheBatch batch;
    private int[][] chunks;
    private IntSet updates = new IntSet();
    private MapEditor editor;
    private Matrix4 matrix = new Matrix4();

    public MapRenderer(MapEditor editor){
        this.editor = editor;
    }

    public void resize(int width, int height){
        if(batch != null) batch.dispose();
        batch = new CacheBatch(width * height * 5);
        chunks = new int[width / chunksize][height / chunksize];
        updates.clear();

        for(int x = 0; x < width / chunksize; x ++){
            for(int y = 0; y < height / chunksize; y ++){
                chunks[x][y] = -1;
            }
        }

        updateAll();
    }


    public void draw(float tx, float ty, float tw, float th){
        Graphics.end();
        Graphics.useBatch(batch);

        IntSetIterator it = updates.iterator();
        while(it.hasNext){
            int i = it.next();
            int x = i % chunks.length;
            int y = i / chunks.length;
            render(x, y, chunks[x][y]);
        }
        updates.clear();

        Graphics.popBatch();

        Gdx.gl.glEnable(GL20.GL_BLEND);

        batch.getTransformMatrix().setToTranslation(tx, ty, 0).scl(tw / (chunks.length * chunksize * tilesize),
                th / (chunks[0].length * chunksize * tilesize), 1f);
        batch.setProjectionMatrix(Core.batch.getProjectionMatrix());
        batch.beginDraw();

        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                int id = chunks[x][y];
                if(id != -1){
                    batch.drawCache(id);
                }
            }
        }

        batch.endDraw();

        Graphics.begin();
    }

    public void updatePoint(int x, int y){
        x /= chunksize;
        y /= chunksize;
        if(Mathf.inBounds(x, y, chunks))
            updates.add(x + y * chunks.length);
    }

    public void updateAll(){
        Graphics.useBatch(batch);

        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                render(x, y, chunks[x][y]);
            }
        }

        Graphics.popBatch();
    }

    private void render(int chunkx, int chunky, int previousID){
        Timers.mark();
        if(previousID == -1){
            batch.begin();
        }else{
            batch.begin(previousID);
        }

        for(int i = 0; i < 2; i ++) {
            for(int x = 0; x < chunksize; x ++){
                for(int y = 0; y < chunksize; y ++){

                    int wx = chunkx*chunksize + x;
                    int wy = chunky*chunksize + y;

                    TileDataWriter data = editor.getMap().readAt(wx, wy);
                    Block floor = Block.getByID(data.floor);
                    Block wall = Block.getByID(data.wall);

                    if(i == 0) {
                        String fregion = Draw.hasRegion(floor.name) ? floor.name : floor.name + "1";

                        if (floor != Blocks.air && Draw.hasRegion(fregion)) {
                            Draw.crect(fregion, wx * tilesize, wy * tilesize);
                        } else {
                            Draw.rect("blank", wx * tilesize, wy * tilesize, 0, 0);
                        }

                    }else{
                        String wregion = Draw.hasRegion(wall.name) ? wall.name : wall.name + "1";

                        if (wall != Blocks.air && Draw.hasRegion(wregion)) {
                            Draw.crect(wregion, wx * tilesize, wy * tilesize);
                        } else {
                            Draw.rect("blank", wx * tilesize, wy * tilesize, 0, 0);
                        }
                    }
                }
            }
        }

        batch.end();
        if(previousID == -1) chunks[chunkx][chunky] = batch.getLastCache();
        Log.info("Time to render cache: {0}", Timers.elapsed());
    }
}
