package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.PlanetGrid.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class PlanetDialog extends FloatingDialog{
    private static final Color outlineColor = Pal.accent.cpy().a(0.6f);
    private static final float camLength = 4f, outlineRad = 1.15f;
    private static final boolean drawRect = false;

    private final PlanetMesh[] outlines = new PlanetMesh[10];
    private final Camera3D cam = new Camera3D();
    private final VertexBatch3D batch = new VertexBatch3D(false, true, 0);
    private final PlaneBatch3D projector = new PlaneBatch3D();

    private Planet planet = Planets.starter;
    private float lastX, lastY;
    private Sector selected, hovered;
    private Table selectTable;

    public PlanetDialog(){
        super("", Styles.fullDialog);

        addCloseButton();
        buttons.addImageTextButton("$techtree", Icon.tree, () -> ui.tech.show()).size(230f, 64f);

        Tmp.v1.trns(0, camLength);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
        projector.setScaling(1f / 300f);

        update(() -> {
            Ptile tile = outline(planet.size).getTile(cam.getPickRay(Core.input.mouseX(), Core.input.mouseY()));
            hovered = tile == null ? null : planet.getSector(tile);

            Vec3 v = Tmp.v33.set(Core.input.mouseX(), Core.input.mouseY(), 0);

            if(Core.input.keyDown(KeyCode.MOUSE_LEFT)){
                float upV = cam.position.angle(Vec3.Y);
                float xscale = 9f, yscale = 10f;
                float margin = 1;

                //scale X speed depending on polar coordinate
                float speed = 1f - Math.abs(upV - 90) / 90f;

                cam.position.rotate(cam.up, (v.x - lastX) / xscale * speed);

                //prevent user from scrolling all the way up and glitching it out
                float amount = (v.y - lastY) / yscale;
                amount = Mathf.clamp(upV + amount, margin, 180f - margin) - upV;

                cam.position.rotate(Tmp.v31.set(cam.up).rotate(cam.direction, 90), amount);
            }

            lastX = v.x;
            lastY = v.y;
        });

        addListener(new ElementGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, KeyCode button){
                selected = hovered;
            }
        });

        selectTable = new Table(t -> {
            t.background(Tex.button);
            t.margin(12f);
            t.add("this is some arbitrary text.");
        });

        selectTable.act(1f);
        selectTable.pack();
        selectTable.setPosition(0, 0, Align.center);

        shown(this::setup);
    }

    void setup(){
        cont.clear();
        titleTable.remove();

        cont.addRect((x, y, w, h) -> {
            render();
        }).grow();
    }

    private void render(){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);

        //lock to up vector so it doesn't get confusing
        cam.up.set(Vec3.Y);

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.lookAt(0, 0, 0);
        cam.update();

        batch.proj(cam.combined());

        PlanetMesh outline = outline(planet.size);

        planet.mesh.render(cam.combined());
        outline.render(cam.combined());

        if(hovered != null){
            drawHover(hovered);

            //if(Core.input.keyTap(KeyCode.SPACE)){
            //    control.playSector(hovered);
            //    ui.planet.hide();
            //}
        }

        if(selected != null){
            drawSelection(selected);

            projector.proj(cam.combined());
            projector.setPlane(
                //origin on sector position
                Tmp.v33.set(selected.tile.v).setLength(outlineRad + 0.05f),
                //face up
                selected.plane.project(Tmp.v32.set(selected.tile.v).add(Vec3.Y)).sub(selected.tile.v).nor(),
                //right vector
                Tmp.v31.set(Tmp.v32).add(selected.tile.v).rotate(selected.tile.v, 90).sub(selected.tile.v).nor()
            );

            Draw.batch(projector, () -> {
                selectTable.draw();
            });
        }

        Gl.disable(Gl.depthTest);
    }

    private void drawHover(Sector sector){
        for(Corner c : sector.tile.corners){
            batch.color(outlineColor);
            batch.vertex(c.v);
        }
        batch.flush(Gl.triangleFan);
    }

    private void drawSelection(Sector sector){
        float length = 0.1f;

        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner next = sector.tile.corners[(i + 1) % sector.tile.corners.length];
            Corner curr = sector.tile.corners[i];
            sector.tile.v.scl(outlineRad);
            Tmp.v31.set(curr.v).sub(sector.tile.v).setLength(length).add(sector.tile.v);
            Tmp.v32.set(next.v).sub(sector.tile.v).setLength(length).add(sector.tile.v);
            sector.tile.v.scl(1f / outlineRad);

            batch.tri(curr.v, next.v, Tmp.v31, Pal.accent);
            batch.tri(Tmp.v31, Tmp.v32, next.v, Pal.accent);
        }
        batch.flush(Gl.triangles);
    }

    private PlanetMesh outline(int size){
        if(outlines[size] == null){
            outlines[size] = new PlanetMesh(size, new PlanetMesher(){
                @Override
                public float getHeight(Vec3 position){
                    return 0;
                }

                @Override
                public Color getColor(Vec3 position){
                    return outlineColor;
                }
            }, outlineRad, true);
        }
        return outlines[size];
    }
}
