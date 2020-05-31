package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class PlanetDialog extends BaseDialog{
    private static final Color
        outlineColor = Pal.accent.cpy().a(1f),
        hoverColor = Pal.accent.cpy().a(0.5f),
        borderColor = Pal.accent.cpy().a(0.3f),
        shadowColor = new Color(0, 0, 0, 0.7f);
    private static final float camLength = 4f;
    private static final float outlineRad = 1.16f;
    private static final Array<Vec3> points = new Array<>();

    //the base planet that's being rendered
    private final Planet solarSystem = Planets.sun;

    private final Mesh[] outlines = new Mesh[10];
    private final Camera3D cam = new Camera3D();
    private final VertexBatch3D batch = new VertexBatch3D(10000, false, true, 0);
    private final PlaneBatch3D projector = new PlaneBatch3D();
    private final Mat3D mat = new Mat3D();
    private final Vec3 camRelative = new Vec3();
    private final ResourcesDialog resources = new ResourcesDialog();

    private float zoom = 1f, smoothZoom = 1f, selectAlpha = 1f;
    private Bloom bloom = new Bloom(Core.graphics.getWidth()/4, Core.graphics.getHeight()/4, true, false){{
        setThreshold(0.8f);
        blurPasses = 6;
    }};
    private Planet planet = Planets.starter;
    private @Nullable Sector selected, hovered;
    private Table stable;
    private Mesh atmosphere = MeshBuilder.buildHex(Color.white, 2, false, 1.5f);

    //seed: 8kmfuix03fw
    private CubemapMesh skybox = new CubemapMesh(new Cubemap("cubemaps/stars/"));

    public PlanetDialog(){
        super("", Styles.fullDialog);

        Events.on(DisposeEvent.class, () -> {
            skybox.dispose();
            batch.dispose();
            projector.dispose();
            atmosphere.dispose();
            for(Mesh m : outlines){
                if(m != null){
                    m.dispose();
                }
            }
        });

        Events.on(ResizeEvent.class, e -> {
            bloom.resize(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4);
        });

        TextButtonStyle style = Styles.cleart;
        float bmargin = 6f;

        getCell(buttons).padBottom(-4);
        buttons.background(Styles.black).defaults().growX().height(64f).pad(0);

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back){
                Core.app.post(this::hide);
            }
        });

        //TODO
        //buttons.button("$back", Icon.left, style, this::hide).margin(bmargin);
        //buttons.button("Research", Icon.tree, style, () -> ui.tech.show()).margin(bmargin);
        //buttons.button("Database", Icon.book, style, () -> ui.database.show()).margin(bmargin);
        //buttons.button("Resources", Icon.file, style, resources::show).margin(bmargin);

        cam.fov = 60f;

        camRelative.set(0, 0f, camLength);
        projector.setScaling(1f / 150f);

        dragged((cx, cy) -> {
            float upV = camRelative.angle(Vec3.Y);
            float xscale = 9f, yscale = 10f;
            float margin = 1;

            //scale X speed depending on polar coordinate
            float speed = 1f - Math.abs(upV - 90) / 90f;

            camRelative.rotate(cam.up, cx / xscale * speed);

            //prevent user from scrolling all the way up and glitching it out
            float amount = cy / yscale;
            amount = Mathf.clamp(upV + amount, margin, 180f - margin) - upV;

            camRelative.rotate(Tmp.v31.set(cam.up).rotate(cam.direction, 90), amount);
        });

        scrolled(value -> {
            zoom = Mathf.clamp(zoom + value / 10f, 0.5f, 2f);
        });

        update(() -> {
            if(planet.isLandable()){
                hovered = planet.getSector(cam.getMouseRay(), outlineRad);
            }else{
                hovered = selected = null;
            }

            smoothZoom = Mathf.lerpDelta(smoothZoom, zoom, 0.4f);
            selectAlpha = Mathf.lerpDelta(selectAlpha, Mathf.num(smoothZoom < 1.9f), 0.1f);
        });

        addListener(new ElementGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, KeyCode button){
                selected = hovered != null && hovered.locked() ? null : hovered;
                if(selected != null){
                    updateSelected();
                }
            }
        });

        stable = new Table();
        stable.background(Styles.black3);

        shown(this::setup);
    }

    /** show with no limitations, just as a map. */
    @Override
    public Dialog show(){
        //TODO
        return super.show();
    }

    public void show(Sector selected, int range){
        //TODO
    }

    void setup(){
        cont.clear();
        titleTable.remove();

        cont.rect((x, y, w, h) -> render()).grow();
    }

    private void render(){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);
        Gl.depthMask(true);

        Gl.enable(Gl.cullFace);
        Gl.cullFace(Gl.back);

        //lock to up vector so it doesn't get confusing
        cam.up.set(Vec3.Y);

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        camRelative.setLength(planet.radius * camLength + (smoothZoom-1f) * planet.radius * 2);
        cam.position.set(planet.position).add(camRelative);
        cam.lookAt(planet.position);
        cam.update();

        //TODO hacky
        Shaders.planet.camDir.set(cam.direction).rotate(Vec3.Y, planet.getRotation());

        projector.proj(cam.combined);
        batch.proj(cam.combined);

        beginBloom();

        skybox.render(cam.combined);

        renderPlanet(solarSystem);

        endBloom();

        Gl.enable(Gl.blend);

        if(hovered != null){
            Draw.batch(projector, () -> {
                setPlane(hovered);
                Draw.color(Color.white, Pal.accent, Mathf.absin(5f, 1f));

                TextureRegion icon = hovered.locked() ? Icon.lock.getRegion() : hovered.is(SectorAttribute.naval) ? Liquids.water.icon(Cicon.large) : null;

                if(icon != null){
                    Draw.rect(icon, 0, 0);
                }

                Draw.reset();
            });
        }

        Gl.disable(Gl.cullFace);
        Gl.disable(Gl.depthTest);

        if(selected != null){
            addChild(stable);
            Vec3 pos = cam.project(Tmp.v31.set(selected.tile.v).setLength(outlineRad).rotate(Vec3.Y, -planet.getRotation()).add(planet.position));
            stable.setPosition(pos.x, pos.y, Align.center);
            stable.toFront();
        }else{
            stable.remove();
        }

        cam.update();
    }

    private void beginBloom(){
       bloom.capture();
    }

    private void endBloom(){
        bloom.render();
    }

    private void renderPlanet(Planet planet){
        //render planet at offsetted position in the world
        planet.mesh.render(cam.combined, planet.getTransform(mat));

        renderOrbit(planet);

        if(planet.isLandable() && planet == this.planet){
            renderSectors(planet);
        }

        if(planet.parent != null && planet.hasAtmosphere && Core.settings.getBool("atmosphere")){
            Blending.additive.apply();

            Shaders.atmosphere.camera = cam;
            Shaders.atmosphere.planet = planet;
            Shaders.atmosphere.bind();
            Shaders.atmosphere.apply();

            atmosphere.render(Shaders.atmosphere, Gl.triangles);

            Blending.normal.apply();
        }

        for(Planet child : planet.children){
            renderPlanet(child);
        }
    }

    private void renderOrbit(Planet planet){
        if(planet.parent == null) return;

        Vec3 center = planet.parent.position;
        float radius = planet.orbitRadius;
        int points = (int)(radius * 50);
        Angles.circleVectors(points, radius, (cx, cy) -> batch.vertex(Tmp.v32.set(center).add(cx, 0, cy), Pal.gray));
        batch.flush(Gl.lineLoop);
    }

    private void renderSectors(Planet planet){
        //apply transformed position
        batch.proj().mul(planet.getTransform(mat));

        for(Sector sec : planet.sectors){

            if(selectAlpha > 0.01f){
                if(sec.unlocked()){
                    Color color =
                        sec.hasBase() ? Team.sharded.color :
                        sec.preset != null ? Team.derelict.color :
                        sec.hasEnemyBase() ? Team.crux.color :
                        null;

                    if(color != null){
                        drawSelection(sec, Tmp.c1.set(color).mul(0.8f).a(selectAlpha), 0.026f, -0.001f);
                    }
                }else{
                    draw(sec, Tmp.c1.set(shadowColor).mul(1, 1, 1, selectAlpha), -0.001f);
                }
            }
        }

        if(hovered != null){
            draw(hovered, hoverColor, -0.001f);
            drawBorders(hovered, borderColor);
        }

        if(selected != null){
            drawSelection(selected);
            drawBorders(selected, borderColor);
        }

        batch.flush(Gl.triangles);

        //render arcs
        if(selected != null && selected.preset != null){
            for(Objective o : selected.preset.requirements){
                if(o instanceof SectorObjective){
                    SectorPreset preset = ((SectorObjective)o).preset;
                    drawArc(planet, selected.tile.v, preset.sector.tile.v);
                }
            }
        }

        //render sector grid
        Mesh mesh = outline(planet.grid.size);
        Shader shader = Shaders.planetGrid;
        Vec3 tile = planet.intersect(cam.getMouseRay(), outlineRad);
        Shaders.planetGrid.mouse.lerp(tile == null ? Vec3.Zero : tile.sub(planet.position).rotate(Vec3.Y, planet.getRotation()), 0.2f);

        shader.bind();
        shader.setUniformMatrix4("u_proj", cam.combined.val);
        shader.setUniformMatrix4("u_trans", planet.getTransform(mat).val);
        shader.apply();
        mesh.render(shader, Gl.lines);
    }

    private void drawArc(Planet planet, Vec3 a, Vec3 b){
        Vec3 avg = Tmp.v31.set(a).add(b).scl(0.5f);
        avg.setLength(planet.radius*2f);

        points.clear();
        points.addAll(Tmp.v33.set(a).setLength(outlineRad), Tmp.v31, Tmp.v34.set(b).setLength(outlineRad));
        Tmp.bz3.set(points);
        float points = 25;

        for(int i = 0; i < points + 1; i++){
            float f = i / points;
            Tmp.c1.set(Pal.accent).lerp(Color.clear, (f+Time.globalTime()/80f)%1f);
            batch.color(Tmp.c1);
            batch.vertex(Tmp.bz3.valueAt(Tmp.v32, f));

        }
        batch.flush(Gl.lineStrip);
    }

    private void drawBorders(Sector sector, Color base){
        Color color = Tmp.c1.set(base).a(base.a + 0.3f + Mathf.absin(Time.globalTime(), 5f, 0.3f));

        float r1 = 1f;
        float r2 = outlineRad + 0.001f;

        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner c = sector.tile.corners[i], next = sector.tile.corners[(i+1) % sector.tile.corners.length];

            Tmp.v31.set(c.v).setLength(r2);
            Tmp.v32.set(next.v).setLength(r2);
            Tmp.v33.set(c.v).setLength(r1);

            batch.tri2(Tmp.v31, Tmp.v32, Tmp.v33, color);

            Tmp.v31.set(next.v).setLength(r2);
            Tmp.v32.set(next.v).setLength(r1);
            Tmp.v33.set(c.v).setLength(r1);

            batch.tri2(Tmp.v31, Tmp.v32, Tmp.v33, color);
        }

        if(batch.getNumVertices() >= batch.getMaxVertices() - 6 * 6){
            batch.flush(Gl.triangles);
        }
    }

    private void updateSelected(){
        float x = stable.getX(Align.center), y = stable.getY(Align.center);
        stable.clear();
        stable.background(Styles.black6);

        //TODO add strings to bundle after prototyping is done

        stable.add("[accent]" + (selected.preset == null ? selected.id : selected.preset.localizedName)).row();
        stable.image().color(Pal.accent).fillX().height(3f).pad(3f).row();
        stable.add(selected.save != null ? selected.save.getPlayTime() : "[lightgray]Unexplored").row();

        stable.add("Resources:").row();
        stable.table(t -> {
            t.left();
            int idx = 0;
            int max = 5;
            for(UnlockableContent c : selected.data.resources){
                t.image(c.icon(Cicon.small)).padRight(3);
                if(++idx % max == 0) t.row();
            }
        }).fillX().row();

        //production
        if(selected.hasBase() && selected.save.meta.hasProduction){
            stable.add("Production:").row();
            stable.table(t -> {
                t.left();

                selected.save.meta.secinfo.exportRates().each(entry -> {
                    int total = (int)(entry.value * turnDuration / 60f);
                    if(total > 1){
                        t.image(entry.key.icon(Cicon.small)).padRight(3);
                        t.add(ui.formatAmount(total) + " /turn").color(Color.lightGray);
                        t.row();
                    }
                });
            }).row();
        }

        //stored resources
        if(selected.hasBase() && selected.save.meta.secinfo.coreItems.size > 0){
            stable.add("Stored:").row();
            stable.table(t -> {
                t.left();

                t.table(res -> {
                    int i = 0;
                    for(Item item : content.items()){
                        int amount = selected.save.meta.secinfo.coreItems.get(item);
                        if(amount > 0){
                            res.image(item.icon(Cicon.small)).padRight(3);
                            res.add(ui.formatAmount(amount)).color(Color.lightGray);
                            if(++i % 2 == 0){
                                res.row();
                            }
                        }
                    }
                });


            }).row();
        }

        //display how many turns this sector has been attacked
        if(selected.getTurnsPassed() > 0 && selected.hasBase()){
            stable.row();

            stable.add("[scarlet]" + Iconc.warning + " " + selected.getTurnsPassed() + "x attacks");
        }

        stable.row();

        stable.button("Launch", Styles.transt, () -> {
            if(selected != null){
                if(selected.is(SectorAttribute.naval)){
                    ui.showInfo("You need a naval loadout to launch here.");
                    return;
                }
                control.playSector(selected);
                hide();
            }
        }).growX().padTop(2f).height(50f).minWidth(170f);

        stable.pack();
        stable.setPosition(x, y, Align.center);

        stable.update(() -> {
            if(selected != null){
                //fade out UI when not facing selected sector
                Tmp.v31.set(selected.tile.v).rotate(Vec3.Y, -planet.getRotation()).scl(-1f).nor();
                float dot = cam.direction.dot(Tmp.v31);
                stable.getColor().a = Math.max(dot, 0f)*2f;
                if(dot*2f <= -0.1f){
                    stable.remove();
                    selected = null;
                }
            }
        });

        stable.act(0f);
    }

    private void setPlane(Sector sector){
        float rotation = -planet.getRotation();
        float length = 0.01f;

        projector.setPlane(
            //origin on sector position
            Tmp.v33.set(sector.tile.v).setLength(outlineRad + length).rotate(Vec3.Y, rotation).add(planet.position),
            //face up
            sector.plane.project(Tmp.v32.set(sector.tile.v).add(Vec3.Y)).sub(sector.tile.v).rotate(Vec3.Y, rotation).nor(),
            //right vector
            Tmp.v31.set(Tmp.v32).rotate(Vec3.Y, -rotation).add(sector.tile.v).rotate(sector.tile.v, 90).sub(sector.tile.v).rotate(Vec3.Y, rotation).nor()
        );
    }

    private void draw(Sector sector, Color color, float offset){
        float rr = outlineRad + offset;
        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner c = sector.tile.corners[i], next = sector.tile.corners[(i+1) % sector.tile.corners.length];
            batch.tri(Tmp.v31.set(c.v).setLength(rr), Tmp.v32.set(next.v).setLength(rr), Tmp.v33.set(sector.tile.v).setLength(rr), color);
        }
    }

    private void drawSelection(Sector sector){
        drawSelection(sector, Pal.accent, 0.04f, 0.001f);
    }

    private void drawSelection(Sector sector, Color color, float stroke, float length){
        float arad = outlineRad + length;

        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner next = sector.tile.corners[(i + 1) % sector.tile.corners.length];
            Corner curr = sector.tile.corners[i];

            next.v.scl(arad);
            curr.v.scl(arad);
            sector.tile.v.scl(arad);

            Tmp.v31.set(curr.v).sub(sector.tile.v).setLength(curr.v.dst(sector.tile.v) - stroke).add(sector.tile.v);
            Tmp.v32.set(next.v).sub(sector.tile.v).setLength(next.v.dst(sector.tile.v) - stroke).add(sector.tile.v);

            batch.tri(curr.v, next.v, Tmp.v31, color);
            batch.tri(Tmp.v31, next.v, Tmp.v32, color);

            sector.tile.v.scl(1f / arad);
            next.v.scl(1f / arad);
            curr.v.scl(1f /arad);
        }
    }

    private Mesh outline(int size){
        if(outlines[size] == null){
            outlines[size] = MeshBuilder.buildHex(new HexMesher(){
                @Override
                public float getHeight(Vec3 position){
                    return 0;
                }

                @Override
                public Color getColor(Vec3 position){
                    return outlineColor;
                }
            }, size, true, outlineRad, 0.2f);
        }
        return outlines[size];
    }
}
