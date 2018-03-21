package io.anuke.mindustry.editor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.io.MapTileData.TileDataWriter;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.MeshBatch;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer {
    private static final int chunksize = 32;
    private MeshBatch mesh = new MeshBatch(0);
    private int[][] chunks;
    private IntSet updates = new IntSet();
    private MapEditor editor;
    private int width, height;

    public MapRenderer(MapEditor editor){
        this.editor = editor;
    }

    public void resize(int width, int height){
        mesh.resize(width * height * 3);
        this.width = width;
        this.height = height;
        updateAll();
    }


    public void draw(float tx, float ty, float tw, float th){
        Timers.mark();

        Graphics.end();

        mesh.getTransformMatrix().setToTranslation(tx, ty, 0).scl(tw / (width * tilesize),
                th / (height * tilesize), 1f);
        mesh.setProjectionMatrix(Core.batch.getProjectionMatrix());

        mesh.render(Core.atlas.getTextures().first());

        Graphics.begin();

        long i = Timers.elapsed();

        if(i > 2) Log.info("Time to render: {0}", i);
    }

    public void updatePoint(int x, int y){
        render(x, y);
    }

    public void updateAll(){
        for(int x = 0; x < width; x ++){
            for(int y = 0; y < height; y ++){
                render(x, y);
            }
        }
    }

    private void render(int wx, int wy){
        TileDataWriter data = editor.getMap().readAt(wx, wy);
        Block floor = Block.getByID(data.floor);
        Block wall = Block.getByID(data.wall);

        String fregion = Draw.hasRegion(floor.name) ? floor.name : floor.name + "1";

        if (floor != Blocks.air && Draw.hasRegion(fregion)) {
            TextureRegion region = Draw.region(fregion);
            mesh.draw(wx + wy*width, region, wx * tilesize, wy * tilesize, 8, 8);
        }

        String wregion = Draw.hasRegion(wall.name) ? wall.name : wall.name + "1";

        if (wall != Blocks.air && Draw.hasRegion(wregion)) {
            TextureRegion region = Draw.region(wregion);
            mesh.draw(wx + wy*width + (width*height), region, wx * tilesize, wy * tilesize, 8, 8);
        }
    }
}
