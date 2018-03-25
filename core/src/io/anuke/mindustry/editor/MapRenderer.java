package io.anuke.mindustry.editor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.IndexedRenderer;

import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer {
    private static final int chunksize = 64;
    private IndexedRenderer[][] chunks;
    private IntSet updates = new IntSet();
    private MapEditor editor;
    private int width, height;

    public MapRenderer(MapEditor editor){
        this.editor = editor;
    }

    public void resize(int width, int height){
        //TODO just freezes on tablets
        if(chunks != null){
            for(int x = 0; x < chunks.length; x ++){
                for(int y = 0; y < chunks[0].length; y ++){
                    chunks[x][y].dispose();
                }
            }
        }

        chunks = new IndexedRenderer[width/chunksize][height/chunksize];

        for(int x = 0; x < width/chunksize; x ++){
            for(int y = 0; y < height/chunksize; y ++){
                chunks[x][y] = new IndexedRenderer(chunksize*chunksize*2);
            }
        }
        this.width = width;
        this.height = height;
        updateAll();
    }


    public void draw(float tx, float ty, float tw, float th){
        Graphics.end();

        IntSetIterator it = updates.iterator();
        while(it.hasNext){
            int i = it.next();
            int x = i % width;
            int y = i / height;
            render(x, y);
        }
        updates.clear();

        for(int x = 0; x < width/chunksize; x ++){
            for(int y = 0; y < height/chunksize; y ++){
                IndexedRenderer mesh = chunks[x][y];

                mesh.getTransformMatrix().setToTranslation(tx, ty, 0).scl(tw / (width * tilesize),
                        th / (height * tilesize), 1f);
                mesh.setProjectionMatrix(Core.batch.getProjectionMatrix());

                mesh.render(Core.atlas.getTextures().first());
            }
        }

        Graphics.begin();

        long i = Timers.elapsed();
    }

    public void updatePoint(int x, int y){
        //TODO spread out over multiple frames?
        updates.add(x + y*width);
    }

    public void updateAll(){
        for(int x = 0; x < width; x ++){
            for(int y = 0; y < height; y ++){
                render(x, y);
            }
        }
    }

    private void render(int wx, int wy){
        int x = wx/chunksize, y = wy/chunksize;
        IndexedRenderer mesh = chunks[x][y];
        TileDataMarker data = editor.getMap().readAt(wx, wy);
        Block floor = Block.getByID(data.floor);
        Block wall = Block.getByID(data.wall);

        String fregion = Draw.hasRegion(floor.name) ? floor.name : floor.name + "1";

        if (floor != Blocks.air && Draw.hasRegion(fregion)) {
            TextureRegion region = Draw.region(fregion);
            mesh.draw((wx % chunksize) + (wy % chunksize)*chunksize, region, wx * tilesize, wy * tilesize, -1f, 8, 8);
        }

        String wregion = Draw.hasRegion(wall.name) ? wall.name : wall.name + "1";

        if (wall != Blocks.air && Draw.hasRegion(wregion)) {
            TextureRegion region = Draw.region(wregion);
            mesh.draw((wx % chunksize) + (wy % chunksize)*chunksize + chunksize*chunksize, region,
                    wx * tilesize - Math.max(region.getRegionWidth()-16f, 0), wy * tilesize - Math.max(region.getRegionHeight()-16f, 0), 0f,
                    region.getRegionWidth(), region.getRegionHeight());
        }
    }
}
