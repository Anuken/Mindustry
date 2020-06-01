package mindustry.graphics.g3d;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.Objectives.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.type.*;
import mindustry.type.Sector.*;
import mindustry.ui.*;

public class PlanetRenderer implements Disposable{
    public static final float outlineRad = 1.17f, camLength = 4f;
    public static final Color
        outlineColor = Pal.accent.cpy().a(1f),
        hoverColor = Pal.accent.cpy().a(0.5f),
        borderColor = Pal.accent.cpy().a(0.3f),
        shadowColor = new Color(0, 0, 0, 0.7f);

    private static final Array<Vec3> points = new Array<>();

    /** Camera direction relative to the planet. Length is determined by zoom. */
    public final Vec3 camPos = new Vec3();
    /** The sun/main planet of the solar system from which everything is rendered. */
    public final Planet solarSystem = Planets.sun;
    /** Planet being looked at. */
    public Planet planet = Planets.starter;
    /** Camera used for rendering. */
    public Camera3D cam = new Camera3D();

    public float zoom = 1f, selectAlpha = 1f;
    public @Nullable Sector selected, hovered;

    private final Mesh[] outlines = new Mesh[10];
    private final VertexBatch3D batch = new VertexBatch3D(10000, false, true, 0);
    private final PlaneBatch3D projector = new PlaneBatch3D();
    private final Mat3D mat = new Mat3D();
    private final FrameBuffer buffer = new FrameBuffer(2, 2, true);

    private final Bloom bloom = new Bloom(Core.graphics.getWidth()/4, Core.graphics.getHeight()/4, true, false){{
        setThreshold(0.8f);
        blurPasses = 6;
    }};
    private final Mesh atmosphere = MeshBuilder.buildHex(Color.white, 2, false, 1.5f);

    //seed: 8kmfuix03fw
    private final CubemapMesh skybox = new CubemapMesh(new Cubemap("cubemaps/stars/"));

    public PlanetRenderer(){
        camPos.set(0, 0f, camLength);
        projector.setScaling(1f / 150f);
        cam.fov = 60f;
    }

    /** Render the entire planet scene to the screen. */
    public void render(){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);
        Gl.depthMask(true);

        Gl.enable(Gl.cullFace);
        Gl.cullFace(Gl.back);

        //lock to up vector so it doesn't get confusing
        cam.up.set(Vec3.Y);

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        camPos.setLength(planet.radius * camLength + (zoom-1f) * planet.radius * 2);
        cam.position.set(planet.position).add(camPos);
        cam.lookAt(planet.position);
        cam.update();

        projector.proj(cam.combined);
        batch.proj(cam.combined);

        beginBloom();

        skybox.render(cam.combined);

        renderPlanet(solarSystem);

        endBloom();

        Gl.enable(Gl.blend);

        //TODO perhaps draw this somewhere else
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

        cam.update();
    }

    private void beginBloom(){
        bloom.resize(Core.graphics.getWidth() / 4, Core.graphics.getHeight() / 4);
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

    @Override
    public void dispose(){
        skybox.dispose();
        batch.dispose();
        projector.dispose();
        atmosphere.dispose();
        buffer.dispose();
        bloom.dispose();
        for(Mesh m : outlines){
            if(m != null){
                m.dispose();
            }
        }
    }
}
