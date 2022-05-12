package mindustry.editor;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class MapRenderer implements Disposable{
    private static final int chunkSize = 64;
    private IndexedRenderer[][] chunks;
    private IntSet updates = new IntSet();
    private IntSet delayedUpdates = new IntSet();
    private TextureRegion clearEditor;
    private int width, height;

    public void resize(int width, int height){
        updates.clear();
        delayedUpdates.clear();
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
        clearEditor = Core.atlas.find("clear-editor");

        updates.each(i -> render(i % width, i / width));
        updates.clear();

        updates.addAll(delayedUpdates);
        delayedUpdates.clear();

        //????
        if(chunks == null){
            return;
        }

        var texture = Core.atlas.find("clear-editor").texture;

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
        clearEditor = Core.atlas.find("clear-editor");
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                render(x, y);
            }
        }
    }

    private TextureRegion getIcon(Block wall, int index){
        return !wall.editorIcon().found() ?
            clearEditor : wall.variants > 0 ?
            wall.editorVariantRegions()[Mathf.randomSeed(index, 0, wall.editorVariantRegions().length - 1)] :
            wall.editorIcon();
    }

    private void render(int wx, int wy){
        int x = wx / chunkSize, y = wy / chunkSize;
        IndexedRenderer mesh = chunks[x][y];
        Tile tile = editor.tiles().getn(wx, wy);

        Team team = tile.team();
        Floor floor = tile.floor();
        Floor overlay = tile.overlay();
        Block wall = tile.block();

        TextureRegion region;

        int idxWall = (wx % chunkSize) + (wy % chunkSize) * chunkSize;
        int idxDecal = (wx % chunkSize) + (wy % chunkSize) * chunkSize + chunkSize * chunkSize;
        boolean center = tile.isCenter();
        boolean useSyntheticWall = wall.synthetic() || overlay.wallOre;

        //draw synthetic wall or floor OR standard wall if wall ore
        if(wall != Blocks.air && useSyntheticWall){
            region = !center ? clearEditor : getIcon(wall, idxWall);

            float width = region.width * Draw.scl, height = region.height * Draw.scl, ox = wall.offset + (tilesize - width) / 2f, oy = wall.offset + (tilesize - height) / 2f;

            //force fit to tile
            if(overlay.wallOre && !wall.synthetic()){
                width = height = tilesize;
                ox = oy = 0f;
            }

            mesh.draw(idxWall, region,
            wx * tilesize + ox,
            wy * tilesize + oy,
            width, height,
            tile.build == null || !wall.rotate ? 0 : tile.build.rotdeg());
        }else{
            region = floor.editorVariantRegions()[Mathf.randomSeed(idxWall, 0, floor.editorVariantRegions().length - 1)];

            mesh.draw(idxWall, region, wx * tilesize, wy * tilesize, 8, 8);
        }

        float offsetX = -(wall.size / 3) * tilesize, offsetY = -(wall.size / 3) * tilesize;

        //draw non-synthetic wall or ore
        if((wall.update || wall.destructible) && center){
            mesh.setColor(team.color);
            region = Core.atlas.find("block-border-editor");
        }else if(!useSyntheticWall && wall != Blocks.air && center){
            region = getIcon(wall, idxWall);

            if(wall == Blocks.cliff){
                mesh.setColor(Tmp.c1.set(floor.mapColor).mul(1.6f));
                region = ((Cliff)Blocks.cliff).editorCliffs[tile.data & 0xff];
            }

            offsetX = tilesize / 2f - region.width / 2f * Draw.scl;
            offsetY = tilesize / 2f - region.height / 2f * Draw.scl;
        }else if((wall == Blocks.air || overlay.wallOre) && !overlay.isAir()){
            if(floor.isLiquid){
                mesh.setColor(Tmp.c1.set(1f, 1f, 1f, floor.overlayAlpha));
            }
            region = overlay.editorVariantRegions()[Mathf.randomSeed(idxWall, 0, tile.overlay().editorVariantRegions().length - 1)];
        }else{
            region = clearEditor;
        }

        float width = region.width * Draw.scl, height = region.height * Draw.scl;
        if(!wall.synthetic() && wall != Blocks.air && !wall.isMultiblock()){
            offsetX = offsetY = 0f;
            width = height = tilesize;
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
