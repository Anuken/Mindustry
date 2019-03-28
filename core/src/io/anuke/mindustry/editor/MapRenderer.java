package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.collection.IntSet.IntSetIterator;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Disposable;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.IndexedRenderer;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer implements Disposable{
    private static final int chunksize = 64;
    private IndexedRenderer[][] chunks;
    private IntSet updates = new IntSet();
    private IntSet delayedUpdates = new IntSet();
    private MapEditor editor;
    private int width, height;
    private Texture texture;

    public MapRenderer(MapEditor editor){
        this.editor = editor;
        texture = Core.atlas.find("clear-editor").getTexture();
    }

    public void resize(int width, int height){
        if(chunks != null){
            for(int x = 0; x < chunks.length; x++){
                for(int y = 0; y < chunks[0].length; y++){
                    chunks[x][y].dispose();
                }
            }
        }

        chunks = new IndexedRenderer[(int) Math.ceil((float) width / chunksize)][(int) Math.ceil((float) height / chunksize)];

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                chunks[x][y] = new IndexedRenderer(chunksize * chunksize * 2);
            }
        }
        this.width = width;
        this.height = height;
        updateAll();
    }

    public void draw(float tx, float ty, float tw, float th){
        Draw.flush();

        IntSetIterator it = updates.iterator();
        while(it.hasNext){
            int i = it.next();
            int x = i % width;
            int y = i / width;
            render(x, y);
        }
        updates.clear();

        updates.addAll(delayedUpdates);
        delayedUpdates.clear();

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                IndexedRenderer mesh = chunks[x][y];

                if(mesh == null){
                    chunks[x][y] = new IndexedRenderer(chunksize * chunksize * 2);
                    mesh = chunks[x][y];
                }

                mesh.getTransformMatrix().setToTranslation(tx, ty).scale(tw / (width * tilesize), th / (height * tilesize));
                mesh.setProjectionMatrix(Draw.proj());

                mesh.render(texture);
            }
        }
    }

    public void updatePoint(int x, int y){
        //TODO spread out over multiple frames?
        updates.add(x + y * width);
    }

    public void updateAll(){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                render(x, y);
            }
        }
    }

    private void render(int wx, int wy){
        int x = wx / chunksize, y = wy / chunksize;
        IndexedRenderer mesh = chunks[x][y];
        Tile tile = editor.tiles()[wx][wy];

        Team team = tile.getTeam();
        Block floor = tile.floor();
        Block wall = tile.block();

        TextureRegion region;

        int idxWall = (wx % chunksize) + (wy % chunksize) * chunksize;
        int idxDecal = (wx % chunksize) + (wy % chunksize) * chunksize + chunksize * chunksize;

        if(wall != Blocks.air && (wall.synthetic() || wall == Blocks.part)){
            region = !Core.atlas.isFound(wall.editorIcon()) ? Core.atlas.find("clear-editor") : wall.editorIcon();

            if(wall.rotate){
                mesh.draw(idxWall, region,
                        wx * tilesize + wall.offset(), wy * tilesize + wall.offset(),
                        region.getWidth() * Draw.scl, region.getHeight() * Draw.scl, tile.getRotation() * 90 - 90);
            }else{
                mesh.draw(idxWall, region,
                        wx * tilesize + wall.offset() + (tilesize - region.getWidth() * Draw.scl)/2f,
                        wy * tilesize + wall.offset() + (tilesize - region.getHeight() * Draw.scl)/2f,
                        region.getWidth() * Draw.scl, region.getHeight() * Draw.scl);
            }
        }else{
            region = floor.editorVariantRegions()[Mathf.randomSeed(idxWall, 0, floor.editorVariantRegions().length-1)];

            mesh.draw(idxWall, region, wx * tilesize, wy * tilesize, 8, 8);
        }

        float offsetX = -(wall.size/3)*tilesize, offsetY = -(wall.size/3) * tilesize;

        if(wall.update || wall.destructible){
            mesh.setColor(team.color);
            region = Core.atlas.find("block-border-editor");
        }else if(!wall.synthetic() && wall != Blocks.air){
            region = !Core.atlas.isFound(wall.editorIcon()) ? Core.atlas.find("clear-editor") : wall.editorIcon();
            offsetX = tilesize/2f - region.getWidth()/2f * Draw.scl;
            offsetY = tilesize/2f - region.getHeight()/2f * Draw.scl;
        }else if(wall == Blocks.air && tile.ore() != null){
            region = tile.ore().editorVariantRegions()[Mathf.randomSeed(idxWall, 0, tile.ore().editorVariantRegions().length-1)];
        }else{
            region = Core.atlas.find("clear-editor");
        }

        mesh.draw(idxDecal, region,
                wx * tilesize + offsetX, wy * tilesize + offsetY,
                region.getWidth() * Draw.scl, region.getHeight() * Draw.scl);
        mesh.setColor(Color.WHITE);
    }

    @Override
    public void dispose(){
        if(chunks == null){
            return;
        }
        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                if(chunks[x][y] != null){
                    chunks[x][y].dispose();
                }
            }
        }
    }
}
