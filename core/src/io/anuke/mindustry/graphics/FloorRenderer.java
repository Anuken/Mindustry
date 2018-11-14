package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Events;

import static io.anuke.mindustry.Vars.world;

public class FloorRenderer{
    private final static int chunksize = 64;
    private final static int vertexSize = 4;
    private float[] vertices = new float[vertexSize];
    private ShaderProgram shader = createDefaultShader();
    private int length;
    private Mesh mesh;

    public FloorRenderer(){
        Events.on(WorldLoadGraphicsEvent.class, event -> clearTiles());
    }

    /**Draws all the floor tiles in the camera range.*/
    public void drawFloor(){
        Gdx.gl.glEnable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE);
        shader.begin();
        shader.setUniformMatrix("u_projectionViewMatrix", Core.camera.combined);
        shader.setUniformi("u_texture", 0);
        mesh.render(shader, GL20.GL_POINTS, 0, length / vertexSize);
        shader.end();
    }

    /**Clears the mesh and renders the entire world to it.*/
    public void clearTiles(){
        if(mesh != null){
            mesh.dispose();
        }

        int size = world.width() * world.height() * vertexSize;

        length = 0;
        mesh = new Mesh(true, size, 0,
            new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
            new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        mesh.getVerticesBuffer().position(0);
        mesh.getVerticesBuffer().limit(mesh.getMaxVertices());
        cache();
    }

    private void addSprite(TextureRegion region, float x, float y){
        vertices[0] = x;
        vertices[1] = y;
        vertices[2] = region.getU();
        vertices[3] = region.getV();

        mesh.getVerticesBuffer().put(vertices);
        length += vertexSize;
    }

    private void cache(){
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);

                Floor floor = tile.floor();
                TextureRegion region = floor.getEditorIcon();
                addSprite(region, tile.worldx(), tile.worldy());
            }
        }
    }

    static ShaderProgram createDefaultShader () {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
        + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
        + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
        //+ "attribute float a_size;\n"
        + "uniform mat4 u_projectionViewMatrix;\n" //
        + "varying vec2 v_texCoords;\n" //
        + "\n" //
        + "void main(){\n" //
        + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
        + "   gl_Position =  u_projectionViewMatrix * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
        + "   gl_PointSize = 8.0f;\n"
        + "}\n";
        String fragmentShader = "#ifdef GL_ES\n" //
        + "precision mediump float;\n" //
        + "#endif\n" //
        + "varying vec2 v_texCoords;\n" //
        + "uniform sampler2D u_texture;\n" //
        + "void main(){\n"//
        + "  gl_FragColor = texture2D(u_texture, gl_PointCoord);\n" //
        + "}";
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (shader.isCompiled() == false) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }
}
