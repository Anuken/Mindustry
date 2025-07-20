package mindustry.editor;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MapRenderer implements Disposable{
    private static final int chunkSize = 60;
    private static final Seq<Tile> tmpTiles = new Seq<>();

    private EditorSpriteCache[][] chunks;
    private IntSet recacheChunks = new IntSet();
    private int width, height;

    private Shader shader;

    public void resize(int width, int height){
        dispose();

        recacheChunks.clear();
        chunks = new EditorSpriteCache[(int)Math.ceil((float)width / chunkSize)][(int)Math.ceil((float)height / chunkSize)];

        this.width = width;
        this.height = height;

        recache();
    }

    public void draw(float tx, float ty, float tw, float th){
        if(shader == null){
            shader = new Shader(
            """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main(){
               v_color = a_color;
               v_color.a = v_color.a * (255.0/254.0);
               v_texCoords = a_texCoord0;
               gl_Position = u_projTrans * a_position;
            }
            """,

            """
            varying lowp vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
        
            void main(){
              gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
            }
            """
            );
        }

        Draw.flush();

        renderer.blocks.floor.checkChanges();

        boolean prev = renderer.animateWater;
        renderer.animateWater = false;

        Tmp.v3.set(Core.camera.position);
        Core.camera.position.set(world.width()/2f * tilesize, world.height()/2f * tilesize);
        Core.camera.width = 999999f;
        Core.camera.height = 999999f;
        Core.camera.mat.set(Draw.proj()).mul(Tmp.m3.setToTranslation(tx, ty).scale(tw / (width * tilesize), th / (height * tilesize)).translate(4f, 4f));
        renderer.blocks.floor.drawFloor();

        Tmp.m2.set(Draw.proj());

        //scissors are always enabled because this is drawn clipped in UI, make sure they don't interfere with drawing shadow events
        Gl.disable(Gl.scissorTest);

        renderer.blocks.processShadows();

        Gl.enable(Gl.scissorTest);

        Draw.proj(Core.camera.mat);

        Draw.shader(Shaders.darkness);
        Draw.rect(Draw.wrap(renderer.blocks.getShadowBuffer().getTexture()), world.width() * tilesize/2f - tilesize/2f, world.height() * tilesize/2f - tilesize/2f, world.width() * tilesize, -world.height() * tilesize);
        Draw.shader();

        Draw.proj(Tmp.m2);

        renderer.blocks.floor.beginDraw();
        renderer.blocks.floor.drawLayer(CacheLayer.walls);
        renderer.animateWater = prev;

        if(chunks == null) return;

        recacheChunks.each(i -> recacheChunk(Point2.x(i), Point2.y(i)));
        recacheChunks.clear();

        shader.bind();
        shader.setUniformMatrix4("u_projTrans", Core.camera.mat);

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                EditorSpriteCache mesh = chunks[x][y];

                if(mesh == null) continue;

                mesh.render(shader);
            }
        }

        Core.camera.position.set(Tmp.v3);
    }

    void updateStatic(int x, int y){
        renderer.blocks.floor.recacheTile(x, y);
    }

    void updateBlock(Tile tile){
        updateBlock(tile.x, tile.y);
        renderer.blocks.updateShadowTile(tile);
    }

    void updateBlock(int x, int y){
        recacheChunks.add(Point2.pack(x / chunkSize, y / chunkSize));
    }

    void recache(){
        renderer.blocks.floor.clearTiles();
        renderer.blocks.reload();

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                recacheChunk(x, y);
            }
        }
    }

    void recacheChunk(int cx, int cy){
        if(chunks[cx][cy] != null){
            chunks[cx][cy].dispose();
            chunks[cx][cy] = null;
        }

        EditorSpriteCache cache = new EditorSpriteCache(renderer.blocks.floor.getVertexBuffer());

        TextureRegion teamRegion = Core.atlas.find("block-border");

        tmpTiles.clear();

        for(int x = cx * chunkSize; x < (cx + 1) * chunkSize; x++){
            for(int y = cy * chunkSize; y < (cy + 1) * chunkSize; y++){
                Tile tile = world.tile(x, y);

                if(tile != null && tile.block() != Blocks.air && tile.block().cacheLayer == CacheLayer.normal && tile.isCenter()){
                    tmpTiles.add(tile);
                }
            }
        }

        tmpTiles.sort(Structs.comparingBool(b -> !b.block().synthetic()));

        for(Tile tile : tmpTiles){
            int x = tile.x, y = tile.y;
            Block block = tile.block();

            TextureRegion region = block.fullIcon;

            float width = region.width * region.scl(), height = region.height * region.scl();

            cache.draw(block.fullIcon,
            x * tilesize + block.offset - width / 2f,
            y * tilesize + block.offset - height / 2f,
            width/2f, height/2f,
            width, height,
            tile.build == null || !block.rotate ? 0 : tile.build.rotdeg(),
            Color.whiteFloatBits);

            if(tile.build != null){
                cache.draw(teamRegion,
                x * tilesize + block.offset - width / 2f,
                y * tilesize + block.offset - height / 2f,
                0f, 0f,
                teamRegion.width * teamRegion.scl(), teamRegion.height * teamRegion.scl(),
                0f,
                tile.build.team.color.toFloatBits());
            }
        }

        tmpTiles.clear();

        if(!cache.isEmpty()){
            cache.build(renderer.blocks.floor.getIndexData());
            chunks[cx][cy] = cache;
        }
    }

    @Override
    public void dispose(){
        if(chunks == null) return;

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                if(chunks[x][y] != null){
                    chunks[x][y].dispose();
                }
            }
        }
    }
}
