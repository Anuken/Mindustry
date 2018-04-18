package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class FloorRenderer {
    private final static int vsize = 6;
    private final static int chunksize = 32;

    private AsyncExecutor executor = new AsyncExecutor(4);
    private ShaderProgram program = createDefaultShader();
    private Chunk[][] cache;
    private float z = 0f;

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

        Graphics.end();

        Graphics.clear(Color.CLEAR);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        program.begin();

        Core.atlas.getTextures().first().bind();

        program.setUniformMatrix("u_projTrans", Core.camera.combined);
        program.setUniformi("u_texture", 0);

        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Mathf.inBounds(worldx, worldy, cache))
                    continue;

                if(cache[worldx][worldy] == null){
                    cache[worldx][worldy] = new Chunk();
                    executor.submit(() -> cacheChunk(worldx, worldy));
                    continue;
                }

                Chunk chunk = cache[worldx][worldy];

                if(!chunk.rendered){
                    continue;
                }

                chunk.mesh.render(program, GL20.GL_TRIANGLES, 0, chunk.length);

                //Core.batch.draw(Core.atlas.getTextures().first(), chunk.vertices, 0, chunk.length);
            }
        }

        program.end();

        Graphics.begin();
    }

    private void fillChunk(float x, float y){
        Draw.color(Color.GRAY);
        Draw.crect("white", x, y, chunksize * tilesize, chunksize * tilesize);
        Draw.color();
    }

    private Chunk cacheChunk(int cx, int cy){
        Chunk chunk = cache[cx][cy];
        chunk.vertices = new float[chunksize*chunksize*vsize * 4*6];

        int idx = 0;
        float[] vertices = chunk.vertices;
        float color = NumberUtils.intToFloatColor(Color.WHITE.toIntBits());
        TextureRegion region = new TextureRegion(Core.atlas.getTextures().first());

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex, tiley);
                if(tile == null) continue;

                Block block = tile.floor();
                if(!Draw.hasRegion(block.name()) || Draw.region(block.name()).getRegionWidth() == 8){
                    TextureRegion base = Draw.region(block.variants > 0 ? (block.name() + MathUtils.random(1, block.variants))  : block.name());
                    idx = draw(vertices, idx, base, tile.worldx(), tile.worldy(), color);

                    for(int dx = -1; dx <= 1; dx ++){
                        for(int dy = -1; dy <= 1; dy ++){

                            if(dx == 0 && dy == 0) continue;

                            Tile other = world.tile(tile.x+dx, tile.y+dy);

                            if(other == null) continue;

                            Block floor = other.floor();

                            if(floor.id <= block.id) continue;

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

                            idx = drawc(vertices, idx, region, tile.worldx()-4 + rx, tile.worldy()-4 + ry, w, h, color);
                        }
                    }
                }else {

                    TextureRegion base = Draw.region(block.name());

                    set(base, region, 1 + Mathf.random(1), 1 + Mathf.random(1));

                    idx = draw(vertices, idx, region, tile.worldx(), tile.worldy(), color);

                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {

                            if (dx == 0 && dy == 0) continue;

                            Tile other = world.tile(tile.x + dx, tile.y + dy);

                            if (other == null) continue;

                            Block floor = other.floor();

                            if (floor.id < block.id) {
                                float ox = (dx == 0 ? Mathf.range(0.5f) : 0);
                                float oy = (dy == 0 ? Mathf.range(0.5f) : 0);
                                set(base, region, (int) (1.5f + 2f * dx + ox), (int) (2f - 2f * dy + oy));

                                idx = draw(vertices, idx, region,
                                        tile.worldx() + dx * tilesize,
                                        tile.worldy() + dy * tilesize, color);
                            }
                        }
                    }
                }
            }
        }

        chunk.length = idx;
        Timers.run(0f, () -> {
            chunk.mesh = new Mesh(true, vertices.length, 0,
                    new VertexAttribute(Usage.Position, 3, "a_position"),
                    new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));
            chunk.mesh.setVertices(chunk.vertices, 0, chunk.length);

            chunk.rendered = true;
        });
        return chunk;
    }

    private void set(TextureRegion base, TextureRegion region, int x, int y){
        x = Mathf.clamp(x, 0, 3);
        y = Mathf.clamp(y, 0, 3);
        region.setRegion(base.getRegionX() + x *8, base.getRegionY() + y *8, 8, 8);
    }

    private int draw(float[] vertices, int idx, TextureRegion region, float x, float y, float color){
        return drawc(vertices, idx, region, x - tilesize/2f, y - tilesize/2f, tilesize, tilesize, color);
    }

    private int drawc(float[] vertices, int idx, TextureRegion region, float x, float y, float width, float height, float color){

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        vertices[idx ++] = x;
        vertices[idx ++] = y;
        vertices[idx ++] = z;
        vertices[idx ++] = u;
        vertices[idx ++] = v;

        vertices[idx ++] = x;
        vertices[idx ++] = fy2;
        vertices[idx ++] = z;
        vertices[idx ++] = u;
        vertices[idx ++] = v2;

        vertices[idx ++] = fx2;
        vertices[idx ++] = fy2;
        vertices[idx ++] = z;
        vertices[idx ++] = u2;
        vertices[idx ++] = v2;

        vertices[idx ++] = x;
        vertices[idx ++] = y;
        vertices[idx ++] = z;
        vertices[idx ++] = u;
        vertices[idx ++] = v;

        vertices[idx ++] = fx2;
        vertices[idx ++] = y;
        vertices[idx ++] = z;
        vertices[idx ++] = u2;
        vertices[idx ++] = v;

        vertices[idx ++] = fx2;
        vertices[idx ++] = fy2;
        vertices[idx ++] = z;
        vertices[idx ++] = u2;
        vertices[idx ++] = v2;

        return idx;
    }

    private class Chunk{
        float[] vertices;
        boolean rendered;
        int length;
        Mesh mesh;
    }

    public void clearTiles(){
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
