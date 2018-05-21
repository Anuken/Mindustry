package io.anuke.mindustry.editor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
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
        if(chunks != null){
            for(int x = 0; x < chunks.length; x ++){
                for(int y = 0; y < chunks[0].length; y ++){
                    chunks[x][y].dispose();
                }
            }
        }

        chunks = new IndexedRenderer[(int)Math.ceil((float)width/chunksize)][(int)Math.ceil((float)height/chunksize )];

        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
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

        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                IndexedRenderer mesh = chunks[x][y];

                mesh.getTransformMatrix().setToTranslation(tx, ty, 0).scl(tw / (width * tilesize),
                        th / (height * tilesize), 1f);
                mesh.setProjectionMatrix(Core.batch.getProjectionMatrix());

                mesh.render(Core.atlas.getTextures().first());
            }
        }

        Graphics.begin();
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

        int offsetx = -(wall.size-1)/2;
        int offsety = -(wall.size-1)/2;

        String fregion = Draw.hasRegion(floor.name) ? floor.name : (Draw.hasRegion(floor.name + "1") ? (floor.name + "1") : "clear");

        TextureRegion region = Draw.region(fregion);
        mesh.draw((wx % chunksize) + (wy % chunksize)*chunksize, region, wx * tilesize, wy * tilesize, -1f, 8, 8);

        String wregion = Draw.hasRegion(wall.name) ? wall.name : (Draw.hasRegion(wall.name + "1") ? (wall.name + "1") : "clear");

        region = Draw.region(wregion);
        mesh.draw((wx % chunksize) + (wy % chunksize)*chunksize + chunksize*chunksize, region,
                wx * tilesize + offsetx*tilesize, wy * tilesize  + offsety * tilesize, 0f,
                region.getRegionWidth(), region.getRegionHeight());

    }
}
