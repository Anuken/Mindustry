package mindustry.graphics.g3d;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.type.*;

public class PlanetRenderer implements Disposable{
    public static final float outlineRad = 1.17f, camLength = 4f;
    public static final Color
    outlineColor = Pal.accent.cpy().a(1f),
    hoverColor = Pal.accent.cpy().a(0.5f),
    borderColor = Pal.accent.cpy().a(0.3f),
    shadowColor = new Color(0, 0, 0, 0.7f);

    private static final Seq<Vec3> points = new Seq<>();

    /** Camera used for rendering. */
    public final Camera3D cam = new Camera3D();
    /** Raw vertex batch. */
    public final VertexBatch3D batch = new VertexBatch3D(20000, false, true, 0);

    private final Mesh[] outlines = new Mesh[10];
    public final PlaneBatch3D projector = new PlaneBatch3D();
    public final Mat3D mat = new Mat3D();
    public final FrameBuffer buffer = new FrameBuffer(2, 2, true);

    public final Bloom bloom = new Bloom(Core.graphics.getWidth()/4, Core.graphics.getHeight()/4, true, false){{
        setThreshold(0.8f);
        blurPasses = 6;
    }};
    public final Mesh atmosphere = MeshBuilder.buildHex(Color.white, 2, false, 1.5f);

    //seed: 8kmfuix03fw
    public final CubemapMesh skybox = new CubemapMesh(new Cubemap("cubemaps/stars/"));

    public PlanetRenderer(){
        projector.setScaling(1f / 150f);
        cam.fov = 60f;
        cam.far = 150f;
    }

    /** Render the entire planet scene to the screen. */
    public void render(PlanetParams params){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);
        Gl.depthMask(true);

        Gl.enable(Gl.cullFace);
        Gl.cullFace(Gl.back);

        int w = params.viewW <= 0 ? Core.graphics.getWidth() : params.viewW;
        int h = params.viewH <= 0 ? Core.graphics.getHeight() : params.viewH;

        bloom.blending = !params.drawSkybox;

        //lock to up vector so it doesn't get confusing
        cam.up.set(Vec3.Y);

        cam.resize(w, h);
        params.camPos.setLength((params.planet.radius + params.planet.camRadius) * camLength + (params.zoom-1f) * (params.planet.radius + params.planet.camRadius) * 2);

        if(params.otherCamPos != null){
            cam.position.set(params.otherCamPos).lerp(params.planet.position, params.otherCamAlpha).add(params.camPos);
        }else{
            cam.position.set(params.planet.position).add(params.camPos);
        }
        //cam.up.set(params.camUp); //TODO broken
        cam.lookAt(params.planet.position);
        cam.update();
        //write back once it changes.
        params.camUp.set(cam.up);
        params.camDir.set(cam.direction);

        projector.proj(cam.combined);
        batch.proj(cam.combined);

        Events.fire(Trigger.universeDrawBegin);

        //begin bloom
        bloom.resize(w, h);
        bloom.capture();

        if(params.drawSkybox){
            //render skybox at 0,0,0
            Vec3 lastPos = Tmp.v31.set(cam.position);
            cam.position.setZero();
            cam.update();

            Gl.depthMask(false);

            skybox.render(cam.combined);

            Gl.depthMask(true);

            cam.position.set(lastPos);
            cam.update();
        }

        Events.fire(Trigger.universeDraw);

        renderPlanet(params.solarSystem, params);
        renderTransparent(params.solarSystem, params);

        bloom.render();

        Events.fire(Trigger.universeDrawEnd);

        Gl.enable(Gl.blend);

        if(params.renderer != null){
            params.renderer.renderProjections(params.planet);
        }

        Gl.disable(Gl.cullFace);
        Gl.disable(Gl.depthTest);

        cam.update();
    }

    public void renderPlanet(Planet planet, PlanetParams params){
        if(!planet.visible()) return;

        cam.update();

        if(cam.frustum.containsSphere(planet.position, planet.clipRadius)){
            //render planet at offsetted position in the world
            planet.draw(params, cam.combined, planet.getTransform(mat));
        }

        for(Planet child : planet.children){
            renderPlanet(child, params);
        }
    }

    public void renderTransparent(Planet planet, PlanetParams params){
        if(!planet.visible()) return;

        planet.drawClouds(params, cam.combined, planet.getTransform(mat));

        if(planet.hasGrid() && planet == params.planet && params.drawUi){
            renderSectors(planet, params);
        }

        if(cam.frustum.containsSphere(planet.position, planet.clipRadius) && planet.parent != null && planet.hasAtmosphere && (params.alwaysDrawAtmosphere || Core.settings.getBool("atmosphere"))){
            planet.drawAtmosphere(atmosphere, cam);
        }

        for(Planet child : planet.children){
            renderTransparent(child, params);
        }

        batch.proj(cam.combined);

        if(params.drawUi){
            renderOrbit(planet, params);
        }
    }

    public void renderOrbit(Planet planet, PlanetParams params){
        if(planet.parent == null || !planet.visible() || params.uiAlpha <= 0.02f || !planet.drawOrbit) return;

        Vec3 center = planet.parent.position;
        float radius = planet.orbitRadius;
        int points = (int)(radius * 10);
        Angles.circleVectors(points, radius, (cx, cy) -> batch.vertex(Tmp.v32.set(center).add(cx, 0, cy), Pal.gray.write(Tmp.c1).a(params.uiAlpha)));
        batch.flush(Gl.lineLoop);
    }

    public void renderSectors(Planet planet, PlanetParams params){
        if(params.uiAlpha <= 0.02f) return;

        //apply transformed position
        batch.proj().mul(planet.getTransform(mat));

        if(params.renderer != null){
            params.renderer.renderSectors(planet);
        }

        //render sector grid
        float scaledOutlineRad = outlineRad * planet.radius;
        Mesh mesh = outline(planet.grid.size, planet.radius);
        Shader shader = Shaders.planetGrid;
        Vec3 tile = planet.intersect(cam.getMouseRay(), scaledOutlineRad);
        Shaders.planetGrid.mouse.lerp(tile == null ? Vec3.Zero : tile.sub(planet.position).rotate(Vec3.Y, planet.getRotation()), 0.2f);

        shader.bind();
        shader.setUniformMatrix4("u_proj", cam.combined.val);
        shader.setUniformMatrix4("u_trans", planet.getTransform(mat).val);
        shader.apply();
        mesh.render(shader, Gl.lines);
    }

    public void drawArc(Planet planet, Vec3 a, Vec3 b){
        drawArc(planet, a, b, Pal.accent, Color.clear, 1f);
    }

    public void drawArc(Planet planet, Vec3 a, Vec3 b, Color from, Color to, float length){
        drawArc(planet, a, b, from, to, length, 80f, 25);
    }

    public void drawArc(Planet planet, Vec3 a, Vec3 b, Color from, Color to, float length, float timeScale, int pointCount){
        //increase curve height when on opposite side of planet, so it doesn't tunnel through
        float scaledOutlineRad = outlineRad * planet.radius;
        float dot = 1f - (Tmp.v32.set(a).nor().dot(Tmp.v33.set(b).nor()) + 1f)/2f;

        Vec3 avg = Tmp.v31.set(b).add(a).scl(0.5f);
        avg.setLength(planet.radius*(1f+length) + dot * 1.35f);

        points.clear();
        points.addAll(Tmp.v33.set(b).setLength(scaledOutlineRad), Tmp.v31, Tmp.v34.set(a).setLength(scaledOutlineRad));
        Tmp.bz3.set(points);

        for(int i = 0; i < pointCount + 1; i++){
            float f = i / (float)pointCount;
            Tmp.c1.set(from).lerp(to, (f+ Time.globalTime /timeScale)%1f);
            batch.color(Tmp.c1);
            batch.vertex(Tmp.bz3.valueAt(Tmp.v32, f));
        }
        batch.flush(Gl.lineStrip);
    }

    public void drawBorders(Sector sector, Color base, float alpha){
        Color color = Tmp.c1.set(base).a((base.a + 0.3f + Mathf.absin(Time.globalTime, 5f, 0.3f)) * alpha);

        float r1 = 1f * sector.planet.radius;
        float r2 = outlineRad * sector.planet.radius + 0.001f;

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

    public void drawPlane(Sector sector, Runnable run){
        Draw.batch(projector, () -> {
            setPlane(sector);
            run.run();
        });
    }

    public void setPlane(Sector sector){
        float rotation = -sector.planet.getRotation();
        float length = 0.01f;

        projector.setPlane(
        //origin on sector position
        Tmp.v33.set(sector.tile.v).setLength((outlineRad + length) * sector.planet.radius).rotate(Vec3.Y, rotation).add(sector.planet.position),
        //face up
        sector.plane.project(Tmp.v32.set(sector.tile.v).add(Vec3.Y)).sub(sector.tile.v, sector.planet.radius).rotate(Vec3.Y, rotation).nor(),
        //right vector
        Tmp.v31.set(Tmp.v32).rotate(Vec3.Y, -rotation).add(sector.tile.v).rotate(sector.tile.v, 90).sub(sector.tile.v).rotate(Vec3.Y, rotation).nor()
        );
    }

    public void fill(Sector sector, Color color, float offset){
        float rr = outlineRad * sector.planet.radius + offset;
        for(int i = 0; i < sector.tile.corners.length; i++){
            Corner c = sector.tile.corners[i], next = sector.tile.corners[(i+1) % sector.tile.corners.length];
            batch.tri(Tmp.v31.set(c.v).setLength(rr), Tmp.v32.set(next.v).setLength(rr), Tmp.v33.set(sector.tile.v).setLength(rr), color);
        }
    }

    public void drawSelection(Sector sector, float alpha){
        drawSelection(sector, Tmp.c1.set(Pal.accent).a(alpha), 0.04f, 0.001f);
    }

    public void drawSelection(Sector sector, Color color, float stroke, float length){
        float arad = (outlineRad + length) * sector.planet.radius;

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

    public Mesh outline(int size, float radiusScale){
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
            }, size, true, outlineRad * radiusScale, 0.2f);
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

    public interface PlanetInterfaceRenderer{
        void renderSectors(Planet planet);
        void renderProjections(Planet planet);
    }
}
