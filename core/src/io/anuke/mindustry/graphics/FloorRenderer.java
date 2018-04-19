package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class FloorRenderer {
    private final static int vsize = 4;
    private final static int chunksize = 32;

    private AsyncExecutor executor = new AsyncExecutor(4);
    private ShaderProgram program = createDefaultShader();
    private Chunk[][] cache;
    private IntSet drawnLayerSet = new IntSet();
    private IntArray drawnLayers = new IntArray();
    private short[] indices;

    public FloorRenderer(){
        int len = chunksize*chunksize*vsize * 6;
        indices = new short[len];
        short j = 0;
        for (int i = 0; i < len; i += 6, j += 4) {
            indices[i] = j;
            indices[i + 1] = (short)(j + 1);
            indices[i + 2] = (short)(j + 2);
            indices[i + 3] = (short)(j + 2);
            indices[i + 4] = (short)(j + 3);
            indices[i + 5] = j;
        }
    }

    public void drawFloor(){

        int chunksx = world.width() / chunksize, chunksy = world.height() / chunksize;

        if(cache == null || cache.length != chunksx || cache[0].length != chunksy){
            cache = new Chunk[chunksx][chunksy];
        }

        OrthographicCamera camera = Core.camera;

        int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
        int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;

        for(int x = -crangex; x <= crangex; x++) {
            for (int y = -crangey; y <= crangey; y++) {
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Mathf.inBounds(worldx, worldy, cache))
                    continue;

                fillChunk(worldx * chunksize * tilesize, worldy * chunksize * tilesize);
            }
        }

        int layers = DrawLayer.values().length;

        drawnLayers.clear();
        drawnLayerSet.clear();

        //preliminary layer check:
        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if (!Mathf.inBounds(worldx, worldy, cache))
                    continue;

                if (cache[worldx][worldy] == null) {
                    cache[worldx][worldy] = new Chunk();
                    executor.submit(() -> cacheChunk(worldx, worldy));
                    continue;
                }

                Chunk chunk = cache[worldx][worldy];

                if (!chunk.rendered) {
                    continue;
                }

                //loop through all layers, and add layer index if it exists
                for(int i = 0; i < layers - 1; i ++){
                    if(chunk.lengths[i] > 0){
                        drawnLayerSet.add(i);
                    }
                }
            }
        }

        IntSetIterator it = drawnLayerSet.iterator();
        while(it.hasNext){
            drawnLayers.add(it.next());
        }

        drawnLayers.sort();

        Graphics.end();
        beginDraw();

        for(int i = 0; i < drawnLayers.size; i ++) {
            DrawLayer layer = DrawLayer.values()[drawnLayers.get(i)];

            drawLayer(layer);
        }

        endDraw();
        Graphics.begin();
    }

    public void beginDraw(){
        Gdx.gl.glEnable(GL20.GL_BLEND);

        Core.atlas.getTextures().first().bind();

        program.begin();
        program.setUniformMatrix("u_projTrans", Core.camera.combined);
        program.setUniformi("u_texture", 0);
    }

    public void endDraw(){
        program.end();
    }

    public void drawLayer(DrawLayer layer){
        OrthographicCamera camera = Core.camera;

        int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
        int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;

        layer.begin();

        for (int x = -crangex; x <= crangex; x++) {
            for (int y = -crangey; y <= crangey; y++) {
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Mathf.inBounds(worldx, worldy, cache) || cache[worldx][worldy] == null || !cache[worldx][worldy].rendered){
                    continue;
                }

                Chunk chunk = cache[worldx][worldy];

                chunk.mesh.render(program, GL20.GL_TRIANGLES,
                        chunk.offsets[layer.ordinal()] / vsize,
                        chunk.lengths[layer.ordinal()] / vsize);
            }
        }

        layer.end();
    }

    private void fillChunk(float x, float y){
        Draw.color(Color.GRAY);
        Draw.crect("white", x, y, chunksize * tilesize, chunksize * tilesize);
        Draw.color();
    }

    private Chunk cacheChunk(int cx, int cy){
        Chunk chunk = cache[cx][cy];

        chunk.vertices =  new float[DrawLayer.values().length*chunksize*chunksize*vsize*4*6];

        for(DrawLayer layer : DrawLayer.values()){
            cacheChunkLayer(cx, cy, chunk, layer);
        }

        Timers.run(0f, () -> {
            chunk.mesh = new Mesh(true, chunk.vertices.length, 0,
                    new VertexAttribute(Usage.Position, 2, "a_position"),
                    new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

            chunk.mesh.setVertices(chunk.vertices, 0, chunk.idx);

            chunk.rendered = true;
        });
        return chunk;
    }

    private void cacheChunkLayer(int cx, int cy, Chunk chunk, DrawLayer layer){
        float[] vertices = chunk.vertices;
        chunk.offsets[layer.ordinal()] = chunk.idx;

        int idx = chunk.idx;
        TextureRegion region = new TextureRegion(Core.atlas.getTextures().first());

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex, tiley);
                if(tile == null) continue;

                if(tile.floor().drawLayer == layer && tile.block().drawLayer != DrawLayer.walls){
                    idx = drawFloor(tile, idx, region, vertices, false);
                }else if(tile.floor().drawLayer.ordinal() < layer.ordinal() && tile.block().drawLayer != DrawLayer.walls){
                    idx = drawFloor(tile, idx, region, vertices, true);
                }

                if(tile.block().drawLayer == layer && layer == DrawLayer.walls){
                    Block block = tile.block();
                    idx = draw(vertices,
                            idx,
                            Draw.region(block.variants > 0 ? (block.name() + MathUtils.random(1, block.variants)) : block.name()),
                            tile.worldx(),
                            tile.worldy());
                }
            }
        }

        chunk.lengths[layer.ordinal()] = idx - chunk.offsets[layer.ordinal()];
        chunk.idx = idx;
    }

    private int drawFloor(Tile tile, int idx, TextureRegion region, float[] vertices, boolean edgesOnly){
        MathUtils.random.setSeed(tile.id());
        Block block = tile.floor();

        if(!edgesOnly) {
            TextureRegion base = Draw.region(block.variants > 0 ? (block.name() + MathUtils.random(1, block.variants)) : block.name());
            idx = draw(vertices, idx, base, tile.worldx(), tile.worldy());
        }

        for(int dx = -1; dx <= 1; dx ++){
            for(int dy = -1; dy <= 1; dy ++){

                if(dx == 0 && dy == 0) continue;

                Tile other = world.tile(tile.x+dx, tile.y+dy);

                if(other == null) continue;

                Block floor = other.floor();

                if(floor.id <= block.id || !((Floor)block).blends.test(floor) || (floor.drawLayer.ordinal() > block.drawLayer.ordinal() && !edgesOnly) ||
                        (edgesOnly && floor.drawLayer == block.drawLayer)) continue;

                TextureRegion result = Draw.hasRegion(floor.name() + "edge") ? Draw.region(floor.name() + "edge") :
                        Draw.region(floor.edge + "edge");

                int sx = -dx*8+2, sy = -dy*8+2;
                int x = Mathf.clamp(sx, 0, 12);
                int y = Mathf.clamp(sy, 0, 12);
                int w = Mathf.clamp(sx+8, 0, 12) - x, h = Mathf.clamp(sy+8, 0, 12) - y;

                float rx = Mathf.clamp(dx*8, 0, 8-w);
                float ry = Mathf.clamp(dy*8, 0, 8-h);

                region.setTexture(result.getTexture());
                region.setRegion(result.getRegionX()+x, result.getRegionY()+y+h, w, -h);

                idx = drawc(vertices, idx, region, tile.worldx()-4 + rx, tile.worldy()-4 + ry, w, h);
            }
        }
        return idx;
    }

    private int draw(float[] vertices, int idx, TextureRegion region, float x, float y){
        return drawc(vertices, idx, region, x - tilesize/2f, y - tilesize/2f, tilesize, tilesize);
    }

    private int drawc(float[] vertices, int idx, TextureRegion region, float x, float y, float width, float height){

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        vertices[idx ++] = x;
        vertices[idx ++] = y;
        vertices[idx ++] = u;
        vertices[idx ++] = v;

        vertices[idx ++] = x;
        vertices[idx ++] = fy2;
        vertices[idx ++] = u;
        vertices[idx ++] = v2;

        vertices[idx ++] = fx2;
        vertices[idx ++] = fy2;
        vertices[idx ++] = u2;
        vertices[idx ++] = v2;

        vertices[idx ++] = x;
        vertices[idx ++] = y;
        vertices[idx ++] = u;
        vertices[idx ++] = v;

        vertices[idx ++] = fx2;
        vertices[idx ++] = y;
        vertices[idx ++] = u2;
        vertices[idx ++] = v;

        vertices[idx ++] = fx2;
        vertices[idx ++] = fy2;
        vertices[idx ++] = u2;
        vertices[idx ++] = v2;

        return idx;
    }

    private class Chunk{
        float[] vertices;
        int[] offsets = new int[DrawLayer.values().length];
        int[] lengths = new int[DrawLayer.values().length];
        int idx = 0;
        boolean rendered;
        Mesh mesh;
    }

    public void clearTiles(){
        if(cache != null){
            for(int x = 0; x < cache.length; x ++){
                for(int y = 0; y < cache[0].length; y ++){
                    if(cache[x][y] != null && cache[x][y].mesh != null){
                        cache[x][y].mesh.dispose();
                        cache[x][y] = null;
                    }
                }
            }
        }
        cache = null;
    }

    static ShaderProgram createDefaultShader () {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "uniform mat4 u_projTrans;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "\n" //
                + "void main()\n" //
                + "{\n" //
                + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "}\n";
        String fragmentShader = "#ifdef GL_ES\n" //
                + "#define LOWP lowp\n" //
                + "precision mediump float;\n" //
                + "#else\n" //
                + "#define LOWP \n" //
                + "#endif\n" //
                + "varying vec2 v_texCoords;\n" //
                + "uniform sampler2D u_texture;\n" //
                + "void main()\n"//
                + "{\n" //
                + "  gl_FragColor = texture2D(u_texture, v_texCoords);\n" //
                + "}";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }
}
