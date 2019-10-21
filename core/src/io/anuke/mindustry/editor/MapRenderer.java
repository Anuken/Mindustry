package io.anuke.mindustry.editor;

import io.anuke.arc.*;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.collection.IntSet.IntSetIterator;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.IndexedRenderer;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;

import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer implements Disposable{
    private static final int chunkSize = 64;
    private IndexedRenderer[][] chunks;
    private IntSet updates = new IntSet();
    private IntSet delayedUpdates = new IntSet();
    private MapEditor editor;
    private int width, height;
    private Texture texture;

    public MapRenderer(MapEditor editor){
        this.editor = editor;
        this.texture = Core.atlas.find("clear-editor").getTexture();

        Events.on(ContentReloadEvent.class, e -> {
            texture = Core.atlas.find("clear-editor").getTexture();
        });
    }

    public void resize(int width, int height){
        if(chunks != null){
            for(int x = 0; x < chunks.length; x++){
                for(int y = 0; y < chunks[0].length; y++){
                    chunks[x][y].dispose();
                }
            }
        }

        chunks = new IndexedRenderer[(int)Math.ceil((float)width / chunkSize)][(int)Math.ceil((float)height / chunkSize)];

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                chunks[x][y] = new IndexedRenderer(chunkSize * chunkSize * 2);
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

        //????
        if(chunks == null){
            return;
        }

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                IndexedRenderer mesh = chunks[x][y];

                if(mesh == null){
                    continue;
                }

                mesh.getTransformMatrix().setToTranslation(tx, ty).scale(tw / (width * tilesize), th / (height * tilesize));
                mesh.setProjectionMatrix(Draw.proj());

                mesh.render(texture);
            }
        }
    }

    public void updatePoint(int x, int y){
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
        int x = wx / chunkSize, y = wy / chunkSize;
        IndexedRenderer mesh = chunks[x][y];
        Tile tile = editor.tiles()[wx][wy];

        Team team = tile.getTeam();
        Block floor = tile.floor();
        Block wall = tile.block();

        TextureRegion region;

        int idxWall = (wx % chunkSize) + (wy % chunkSize) * chunkSize;
        int idxDecal = (wx % chunkSize) + (wy % chunkSize) * chunkSize + chunkSize * chunkSize;

        if(wall != Blocks.air && (wall.synthetic() || wall instanceof BlockPart)){
            region = !Core.atlas.isFound(wall.editorIcon()) ? Core.atlas.find("clear-editor") : wall.editorIcon();

            if(wall.rotate){
                mesh.draw(idxWall, region,
                wx * tilesize + wall.offset(), wy * tilesize + wall.offset(),
                region.getWidth() * Draw.scl, region.getHeight() * Draw.scl, tile.rotation() * 90 - 90);
            }else{
                float width = region.getWidth() * Draw.scl, height = region.getHeight() * Draw.scl;

                mesh.draw(idxWall, region,
                wx * tilesize + wall.offset() + (tilesize - width) / 2f,
                wy * tilesize + wall.offset() + (tilesize - height) / 2f,
                width, height);
            }
        }else{
            region = floor.editorVariantRegions()[Mathf.randomSeed(idxWall, 0, floor.editorVariantRegions().length - 1)];

            mesh.draw(idxWall, region, wx * tilesize, wy * tilesize, 8, 8);
        }

        float offsetX = -(wall.size / 3) * tilesize, offsetY = -(wall.size / 3) * tilesize;

        if(wall.update || wall.destructible){
            mesh.setColor(team.color);
            region = Core.atlas.find("block-border-editor");
        }else if(!wall.synthetic() && wall != Blocks.air){
            region = !Core.atlas.isFound(wall.editorIcon()) ? Core.atlas.find("clear-editor") : wall.editorIcon();
            offsetX = tilesize / 2f - region.getWidth() / 2f * Draw.scl;
            offsetY = tilesize / 2f - region.getHeight() / 2f * Draw.scl;
        }else if(wall == Blocks.air && tile.overlay() != null){
            region = tile.overlay().editorVariantRegions()[Mathf.randomSeed(idxWall, 0, tile.overlay().editorVariantRegions().length - 1)];
        }else{
            region = Core.atlas.find("clear-editor");
        }

        float width = region.getWidth() * Draw.scl, height = region.getHeight() * Draw.scl;
        if(!wall.synthetic() && wall != Blocks.air && !wall.isMultiblock()){
            offsetX = 0;
            offsetY = 0;
            width = tilesize;
            height = tilesize;
        }

        mesh.draw(idxDecal, region, wx * tilesize + offsetX, wy * tilesize + offsetY, width, height);
        mesh.setColor(Color.white);
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
