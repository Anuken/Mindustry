package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.graphics.PlanetGrid.*;
import mindustry.graphics.PlanetMesh.*;
import mindustry.maps.generators.*;
import mindustry.maps.planet.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class PlanetRenderer implements PlanetGenerator{
    private final Color outlineColor = Pal.accent.cpy().a(0.7f);
    private final float camLength = 4f, outlineRad = 1.2f;
    private final boolean drawnRect = true;

    private final PlanetMesh[] outlines = new PlanetMesh[10];
    private final Camera3D cam = new Camera3D();
    private final VertexBatch3D batch = new VertexBatch3D(false, true, 0);

    private float lastX, lastY;

    public PlanetRenderer(){
        Tmp.v1.trns(0, camLength);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
    }

    public void render(Planet planet){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);

        input();

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        PlanetMesh outline = outline(planet.size);

        planet.mesh.render(cam.combined());
        outline.render(cam.combined());

        Ptile tile = outline.getTile(cam.getPickRay(Core.input.mouseX(), Core.input.mouseY()));
        if(tile != null){
            for(int i = 0; i < tile.corners.length; i++){
                batch.color(outlineColor);
                batch.vertex(tile.corners[i].v);
            }
            batch.flush(cam.combined(), Gl.triangleFan);

            if(drawnRect){
                SectorRect rect = planet.mesh.projectTile(tile);
                rect.center.scl(outlineRad);
                rect.right.scl(outlineRad);
                rect.top.scl(outlineRad);

                batch.color(Pal.place);
                batch.vertex(rect.project(0, 0));
                batch.color(Pal.place);
                batch.vertex(rect.project(1, 0));
                batch.color(Pal.place);
                batch.vertex(rect.project(1, 1));
                batch.color(Pal.place);
                batch.vertex(rect.project(0, 1));
                batch.flush(cam.combined(), Gl.lineLoop);

                SectorRect coords = planet.mesh.projectTile(tile);

                if(Core.input.keyTap(KeyCode.SPACE)){
                    logic.reset();
                    Vars.world.loadGenerator(new Generator(250, 250){
                        @Override
                        public void generate(Tiles tiles){
                            TileGen gen = new TileGen();
                            tiles.each((x, y) -> {
                                gen.reset();
                                Vec3 position = coords.project(x / (float)width, y / (float)height);

                                planet.generator.generate(position, gen);
                                tiles.set(x, y, new Tile(x, y, gen.floor, gen.overlay, gen.block));
                            });

                            tiles.get(width/2, height/2).setBlock(Blocks.coreShard, Team.sharded);
                        }
                    });
                    state.set(State.playing);
                    logic.play();
                    ui.planet.hide();
                }
            }
        }

        Gl.disable(Gl.depthTest);
    }

    private PlanetMesh outline(int size){
        if(outlines[size] == null){
            outlines[size] = new PlanetMesh(size, this, outlineRad, true);
        }
        return outlines[size];
    }

    private void input(){
        Vec3 v = Tmp.v33.set(Core.input.mouseX(), Core.input.mouseY(), 0);

        if(Core.input.keyDown(KeyCode.MOUSE_LEFT)){
            cam.position.rotate(Vec3.Y, (v.x - lastX) / 10);
        }
        lastX = v.x;
        lastY = v.y;
    }

    @Override
    public float getHeight(Vec3 position){
        return 0;
    }

    @Override
    public Color getColor(Vec3 position){
        return outlineColor;
    }

    @Override
    public void generate(Vec3 position, TileGen tile){

    }
}
