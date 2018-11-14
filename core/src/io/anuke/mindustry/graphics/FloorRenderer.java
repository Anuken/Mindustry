package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Structs;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class FloorRenderer{
    private final static int chunksize = 32;
    private final static int vertexSize = 6;
    private float[] vertices = new float[vertexSize];
    private Chunk[][] chunks;
    private ShaderProgram shader = createDefaultShader();
    private Mesh mesh;

    public FloorRenderer(){
        Events.on(WorldLoadGraphicsEvent.class, event -> clearTiles());
    }

    /**Draws all the floor tiles in the camera range.*/
    public void drawFloor(){
        OrthographicCamera camera = Core.camera;

        int crangex = (int) (camera.viewportWidth * camera.zoom / (chunksize * tilesize)) + 1;
        int crangey = (int) (camera.viewportHeight * camera.zoom / (chunksize * tilesize)) + 1;

        int camx = Mathf.scl(camera.position.x, chunksize * tilesize);
        int camy = Mathf.scl(camera.position.y, chunksize * tilesize);

        Gdx.gl.glEnable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE);
        Gdx.gl.glEnable( 0x8861);
        Core.atlas.getTextures().first().bind(0);
        shader.begin();
        shader.setUniformMatrix("u_projectionViewMatrix", Core.camera.combined);
        shader.setUniformi("u_texture", 0);

        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = camx + x;
                int worldy = camy + y;

                if(!Structs.inBounds(worldx, worldy, chunks))
                    continue;

                Chunk chunk = chunks[worldx][worldy];
                mesh.render(shader, GL20.GL_POINTS, chunk.start / vertexSize, chunk.end / vertexSize);
            }
        }

        shader.end();
    }

    /**Clears the mesh and renders the entire world to it.*/
    public void clearTiles(){
        if(mesh != null){
            mesh.dispose();
        }

        int size = world.width() * world.height() * vertexSize;

        mesh = new Mesh(true, size, 0,
            new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
            new VertexAttribute(Usage.TextureCoordinates, 4, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        mesh.getVerticesBuffer().position(0);
        mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
        cache();
    }

    private void addSprite(TextureRegion region, float x, float y){
        vertices[0] = x;
        vertices[1] = y;
        vertices[2] = region.getU();
        vertices[3] = region.getV();
        vertices[4] = region.getU2();
        vertices[5] = region.getV2();

        mesh.getVerticesBuffer().put(vertices);
    }

    private void cache(){
        Timers.mark();

        int chunksx = Mathf.ceil((float) (world.width()) / chunksize),
                chunksy = Mathf.ceil((float) (world.height()) / chunksize);
        chunks = new Chunk[chunksx][chunksy];

        for(int x = 0; x < chunksx; x++){
            for(int y = 0; y < chunksy; y++){
                int offset = mesh.getVerticesBuffer().position();

                for(int cx = x*chunksize; cx < (x + 1) * chunksize && cx < world.width(); cx++){
                    for(int cy = y*chunksize; cy < (y + 1) * chunksize && cy < world.height(); cy++){
                        Tile tile = world.tile(cx, cy);

                        Floor floor = tile.floor();
                        TextureRegion region = floor.getEditorIcon();
                        addSprite(region, tile.worldx(), tile.worldy());
                    }
                }

                chunks[x][y] = new Chunk(offset, mesh.getVerticesBuffer().position() - offset);

            }
        }
        Log.info("Cache: " + Timers.elapsed());
    }

    static ShaderProgram createDefaultShader () {
        String vertexShader =
        "#version 120\n"
        + "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
        + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
        + "attribute vec4 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
        //+ "attribute float a_size;\n"
        + "uniform mat4 u_projectionViewMatrix;\n" //
        + "varying vec4 v_texCoords;\n" //
        + "\n" //
        + "void main(){\n" //
        + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
        + "   gl_Position = u_projectionViewMatrix * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
        + "   gl_PointSize = 33.0;\n"
        + "}\n";
        String fragmentShader =
        "#version 120\n"
        + "#ifdef GL_ES\n" //
        + "precision mediump float;\n" //
        + "#endif\n" //
        + "varying vec4 v_texCoords;\n" //
        + "uniform sampler2D u_texture;\n" //
        + "void main(){\n"//
        + "  gl_FragColor = texture2D(u_texture, gl_PointCoord * (v_texCoords.zw - v_texCoords.xy) + v_texCoords.xy);\n" //
        + "}";
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (shader.isCompiled() == false) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }

    class Chunk{
        final int start, end;

        public Chunk(int start, int end){
            this.start = start;
            this.end = end;
        }
    }
}
