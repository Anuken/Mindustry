package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.collection.IntSet.IntSetIterator;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureAtlas.AtlasRegion;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Disposable;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.IndexedRenderer;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;

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
        final Point windowPoint = new Point(wx, wy);
        final Point gridPoint = new Point(wx/chunkSize, wy/chunkSize);

        final Point chunkStartPoint = getChunkStartPoint(windowPoint);
        final int chunkArea = chunkSize * chunkSize;


        IndexedRenderer mesh = getMesh(gridPoint);
        Tile tile = getTile(windowPoint);
        Block wall = tile.block();

        int indexWall = chunkStartPoint.x + chunkStartPoint.y;
        int indexDecal = chunkStartPoint.x  + chunkStartPoint.y + chunkArea;

        renderMeshRegion(windowPoint, mesh, tile, wall, indexWall);


        renderTileRegion(windowPoint, mesh, tile, wall, indexWall, indexDecal);
    }

    private void renderTileRegion(Point windowPoint, IndexedRenderer mesh, Tile tile, Block wall, int indexWall, int indexDecal) {
        TextureRegion textureRegion;
        final boolean isEditorIconExist = Core.atlas.isFound(wall.editorIcon());

        Float tileRegion = new Float();
        Team team = tile.getTeam();
        float offsetSize = -Mathf.floor((float)(wall.size / 3.0)) * tilesize;
        FloatPoint offset = new FloatPoint(offsetSize, offsetSize);

        final boolean isTeamDrawRequired = wall.update || wall.destructible;
        if(isTeamDrawRequired){
            mesh.setColor(team.color);
            textureRegion = getRegionInstance("block-border-editor");
        }else if(!wall.synthetic() && !isAirBlock(wall)){
            textureRegion = getEditorIcon(wall, isEditorIconExist);
            offset.x = getMeshOffset(textureRegion.getWidth());
            offset.y = getMeshOffset(textureRegion.getHeight());
        }else if(isAirBlock(wall) && tile.overlay() != null){
            int random = getRandomValue(indexWall, tile.overlay().editorVariantRegions().length);
            textureRegion = tile.overlay().editorVariantRegions()[random];
        }else{
            textureRegion = getRegionInstance("clear-editor");
        }

        tileRegion.x = windowPoint.x * tilesize + offset.x;
        tileRegion.y = windowPoint.y * tilesize + offset.y;
        tileRegion.width = textureRegion.getWidth() * Draw.scl;
        tileRegion.height = textureRegion.getHeight() * Draw.scl;

        drawMesh(mesh, tile, textureRegion, indexDecal, tileRegion, false);
        mesh.setColor(Color.WHITE);
    }


    private void renderMeshRegion(Point windowPoint, IndexedRenderer mesh, Tile tile, Block wall, int indexWall) {
        TextureRegion textureRegion;
        final boolean isBlockPart = (wall.synthetic() || wall instanceof BlockPart);
        final boolean isEditorIconExist = Core.atlas.isFound(wall.editorIcon());

        Float meshRegion = new Float();
        boolean needsRotate = false;
        Block floor = tile.floor();

        meshRegion.x = windowPoint.x * tilesize;
        meshRegion.y = windowPoint.y * tilesize;
        if(!isAirBlock(wall) && isBlockPart){
            textureRegion = getEditorIcon(wall, isEditorIconExist);
            needsRotate = wall.rotate;
            meshRegion.x += wall.offset();
            meshRegion.y += wall.offset();
            if (!needsRotate) {
              meshRegion.x += getMeshOffset(textureRegion.getWidth());
              meshRegion.y += getMeshOffset(textureRegion.getHeight());
            }
            meshRegion.width = textureRegion.getWidth() * Draw.scl;
            meshRegion.height = textureRegion.getHeight() * Draw.scl;
        }else{
            int random = getRandomValue(indexWall, floor.editorVariantRegions().length);
            textureRegion = floor.editorVariantRegions()[random];
            meshRegion.width = tilesize;
            meshRegion.height = tilesize;
        }
        drawMesh(mesh, tile, textureRegion, indexWall, meshRegion, needsRotate);
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

    private void drawMesh(IndexedRenderer mesh, Tile tile, TextureRegion textureRegion,
        int index, Rectangle2D.Float meshRegion, boolean needsRotate) {
        if(needsRotate){
            final float rotationDegree = tile.rotation() * 90 - 90;
            mesh.draw(index, textureRegion,
                      meshRegion.x,
                      meshRegion.y,
                      meshRegion.width,
                      meshRegion.height,
                      rotationDegree);
        }else{
            mesh.draw(index, textureRegion,
                      meshRegion.x,
                      meshRegion.y,
                      meshRegion.width,
                      meshRegion.height);
        }
    }

    private int getRandomValue(int index, int length) {
        return Mathf.randomSeed(index, 0, length - 1);
    }

    private Tile getTile(Point point){
        return editor.tiles()[point.x][point.y];
    }

    private IndexedRenderer getMesh(Point point) {
        return chunks[point.x][point.y];
    }

    private Point getChunkStartPoint(Point point) {
        int startX = (point.x % chunkSize);
        int startY = (point.y % chunkSize) * chunkSize;
        return new Point(startX, startY);
    }

    private boolean isAirBlock(Block block) {
        return block.equals(Blocks.air);
    }

    private AtlasRegion getRegionInstance(String name) {
        return Core.atlas.find(name);
    }

    private float getMeshOffset(float size) {
        return (tilesize - size * Draw.scl) / 2f;
    }

    private TextureRegion getEditorIcon(Block wall, boolean isEditorIconExist) {
        return isEditorIconExist ? wall.editorIcon() : getRegionInstance("clear-editor");
    }

    private class FloatPoint{
        private float x;
        private float y;
        private FloatPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
