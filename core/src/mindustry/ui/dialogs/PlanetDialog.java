package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.PlanetGrid.*;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class PlanetDialog extends FloatingDialog{
    private static final Color outlineColor = Pal.accent.cpy().a(0.6f);
    private static final float camLength = 4f, outlineRad = 1.15f;
    private static final boolean drawRect = false;

    private final PlanetMesh[] outlines = new PlanetMesh[10];
    private final Camera3D cam = new Camera3D();
    private final VertexBatch3D batch = new VertexBatch3D(false, true, 0);

    private float lastX, lastY;
    private Sector selected;

    public PlanetDialog(){
        super("", Styles.fullDialog);

        addCloseButton();
        buttons.addImageTextButton("$techtree", Icon.tree, () -> ui.tech.show()).size(230f, 64f);

        Tmp.v1.trns(0, camLength);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);

        update(() -> {

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

        shown(this::setup);
    }

    void setup(){
        cont.clear();
        titleTable.remove();

        cont.addRect((x, y, w, h) -> {
            render(Planets.starter);
        }).grow();
    }

    private void render(Planet planet){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);

        //lock to up vector so it doesn't get confusing
        cam.up.set(Vec3.Y);

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        PlanetMesh outline = outline(planet.size);

        planet.mesh.render(cam.combined());
        outline.render(cam.combined());

        //dftactgrp sqlrpgle
        Ptile tile = outline.getTile(cam.getPickRay(Core.input.mouseX(), Core.input.mouseY()));
        if(tile != null){
            float length = 0.1f;

            Sector sector = planet.getSector(tile);
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
            batch.flush(cam.combined(), Gl.triangles);

            if(drawRect){
                SectorRect rect = sector.rect;
                rect.center.scl(outlineRad);
                rect.right.scl(outlineRad);
                rect.top.scl(outlineRad);

                batch.color(Color.red);
                batch.vertex(rect.center);
                batch.color(Color.red);
                batch.vertex(sector.tile.corners[0].v);

                batch.color(Color.green);
                batch.vertex(rect.center);
                batch.color(Color.green);
                batch.vertex(rect.top.cpy().add(rect.center));
                batch.flush(cam.combined(), Gl.lines);

                //Log.info((int)(sector.tile.corners[0].v.cpy().sub(rect.center).angle(rect.top)));

                rect.center.scl(1f / outlineRad);
                rect.right.scl(1f / outlineRad);
                rect.top.scl(1f / outlineRad);
            }

            if(Core.input.keyTap(KeyCode.SPACE)){
                control.playSector(sector);
                ui.planet.hide();
            }

        }

        Gl.disable(Gl.depthTest);
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
