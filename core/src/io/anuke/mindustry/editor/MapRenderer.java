package io.anuke.mindustry.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import io.anuke.mindustry.io.MapTileData.TileDataWriter;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Draw;

import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer {
    private static final int chunksize = 32;
    private CacheBatch batch;
    private int[][] chunks;
    private IntSet updates = new IntSet();
    private MapEditor editor;

    public MapRenderer(MapEditor editor){
        this.editor = editor;
    }

    public void resize(int width, int height){
        batch = new CacheBatch(width * height * 3);
        chunks = new int[width / chunksize][height / chunksize];
        updates.clear();
        updateAll();
    }

    public void draw(){
        Graphics.end();
        Graphics.useBatch(batch);

        IntSetIterator it = updates.iterator();
        int i = it.next();
        for(; it.hasNext; i = it.next()){
            int x = i % chunks.length;
            int y = i / chunks.length;
            render(x, y, chunks[x][y]);
        }
        updates.clear();

        Graphics.popBatch();

        Gdx.gl.glEnable(GL20.GL_BLEND);

        batch.beginDraw();

        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                int id = chunks[x][y];
                batch.drawCache(id);
            }
        }

        batch.endDraw();

        Graphics.begin();
    }

    public void updatePoint(int x, int y){
        x /= chunksize;
        y /= chunksize;
        updates.add(x + y * chunks.length);
    }

    public void updateAll(){
        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                render(x, y, chunks[x][y]);
            }
        }
    }

    private void render(int chunkx, int chunky, int previousID){
        if(previousID == -1){
            batch.begin();
        }else{
            batch.begin(previousID);
        }

        for(int x = 0; x < chunkx; x ++){
            for(int y = 0; y < chunky; y ++){
                int wx = chunkx*chunksize + x;
                int wy = chunky*chunksize + y;

                TileDataWriter data = editor.getMap().readAt(wx, wy);
                Block floor = Block.getByID(data.floor);
                Block wall = Block.getByID(data.wall);

                if(floor != Blocks.air) Draw.rect(floor.name, wx * tilesize, wy * tilesize);
                if(floor != Blocks.air) Draw.rect(wall.name, wx * tilesize, wy * tilesize);
            }
        }

        batch.end();
        chunks[chunkx][chunky] = batch.getLastCache();
    }
}
