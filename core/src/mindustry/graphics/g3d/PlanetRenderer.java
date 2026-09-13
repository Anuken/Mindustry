package mindustry.graphics.g3d;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
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

    /** Camera used for rendering. */
    public final Camera3D cam = new Camera3D();
    /** Raw vertex batch. */
    public final VertexBatch3D batch = new VertexBatch3D(20000, false, true, 0);

    public final PlaneBatch3D projector = new PlaneBatch3D();
    public final Mat3D mat = new Mat3D();

    public final Bloom bloom = new Bloom(Core.graphics.getWidth()/4, Core.graphics.getHeight()/4, true, false){{
        setThreshold(0.8f);
        blurPasses = 6;
    }};
    public final Mesh atmosphere = MeshBuilder.buildHex(Color.white, 2, 1.5f);

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

        Planet solarSystem = params.planet.solarSystem;
        renderPlanet(solarSystem, params);
        renderTransparent(solarSystem, params);

        //TODO: will draw under icons and look bad. maybe limit arcs based on facing dot product
        if(params.renderer != null){
            batch.proj().mul(params.planet.getTransform(mat));
            params.renderer.renderOverProjections(params.planet);
        }

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
        planet.renderSectors(batch, cam, params);
    }

    public void drawArcLine(Planet planet, Vec3 a, Vec3 b){
        drawArcLine(planet, a, b, Pal.accent, Tmp.c3.set(Pal.accent).a(0f), 1f, 80f, 25, 0.006f);
    }

    public void drawArc(Planet planet, Vec3 a, Vec3 b){
        drawArc(planet, a, b, Pal.accent, Color.clear, 1f);
    }

    public void drawArc(Planet planet, Vec3 a, Vec3 b, Color from, Color to, float length){
        drawArc(planet, a, b, from, to, length, 80f, 25);
    }

    public void drawArc(Planet planet, Vec3 a, Vec3 b, Color from, Color to, float length, float timeScale, int pointCount){
        planet.drawArc(batch, a, b, from, to, length, timeScale, pointCount);
    }

    public void drawArcLine(Planet planet, Vec3 a, Vec3 b, Color from, Color to, float length, float timeScale, int pointCount, float stroke){
        planet.drawArcLine(batch, a, b, from, to, length, timeScale, pointCount, stroke);
    }

    public void drawBorders(Sector sector, Color base, float alpha){
        sector.planet.drawBorders(batch, sector, base, alpha);
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
        sector.planet.setPlane(sector, projector);
    }

    public void fill(Sector sector, Color color, float offset){
        sector.planet.fill(batch, sector, color, offset);
    }

    public void drawSelection(Sector sector, float alpha){
        drawSelection(sector, Tmp.c1.set(Pal.accent).a(alpha), 0.04f, 0.001f);
    }

    public void drawSelection(Sector sector, Color color, float stroke, float length){
        sector.planet.drawSelection(batch, sector, color, stroke, length);
    }

    /** Draws sector when selected. Supply the batch with {@link Gl#triangles triangle} vertices. */
    public void drawSpecialSelection(Sector sector, Color color, float stroke, float length){
        drawSelection(sector, color, stroke, length);

        float arad = (outlineRad + length) * sector.planet.radius;
        float span = 0.1f;

        for(int i = 0; i < sector.tile.corners.length; i += 2){
            Corner next = sector.tile.corners[(i + 1) % sector.tile.corners.length];
            Corner curr = sector.tile.corners[i];

            next.v.scl(arad);
            curr.v.scl(arad);
            sector.tile.v.scl(arad);

            Tmp.v31.set(curr.v).sub(sector.tile.v).setLength(curr.v.dst(sector.tile.v) - stroke).add(sector.tile.v);
            Tmp.v32.set(next.v).sub(sector.tile.v).setLength(next.v.dst(sector.tile.v) - stroke).add(sector.tile.v);
            Tmp.v33.set(Tmp.v31).lerp(Tmp.v32, span);
            Tmp.v34.set(Tmp.v31).lerp(Tmp.v32, 1f - span);

            Tmp.v31.set(Tmp.v33).sub(sector.tile.v).setLength(Tmp.v31.len() - stroke).add(sector.tile.v);
            Tmp.v32.set(Tmp.v34).sub(sector.tile.v).setLength(Tmp.v32.len() - stroke).add(sector.tile.v);

            batch.quad(Tmp.v33, Tmp.v34, Tmp.v32, Tmp.v31, color);

            sector.tile.v.scl(1f / arad);
            next.v.scl(1f / arad);
            curr.v.scl(1f / arad);
        }
    }

    @Override
    public void dispose(){
        skybox.dispose();
        batch.dispose();
        projector.dispose();
        atmosphere.dispose();
        bloom.dispose();
    }

    public interface PlanetInterfaceRenderer{
        void renderSectors(Planet planet);
        void renderProjections(Planet planet);
        default void renderOverProjections(Planet planet){}
    }
}
