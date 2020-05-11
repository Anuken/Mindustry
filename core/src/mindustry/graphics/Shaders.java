package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.type.*;

import static mindustry.Vars.renderer;

public class Shaders{
    public static BlockBuild blockbuild;
    public static @Nullable Shield shield;
    public static UnitBuild build;
    public static FogShader fog;
    public static LightShader light;
    public static SurfaceShader water, tar, slag;
    public static PlanetShader planet;
    public static PlanetGridShader planetGrid;
    public static AtmosphereShader atmosphere;
    public static MeshShader mesh = new MeshShader();
    public static Shader unlit;

    public static void init(){
        blockbuild = new BlockBuild();
        try{
            shield = new Shield();
        }catch(Throwable t){
            //don't load shield shader
            shield = null;
            t.printStackTrace();
        }
        build = new UnitBuild();
        fog = new FogShader();
        light = new LightShader();
        water = new SurfaceShader("water");
        tar = new SurfaceShader("tar");
        slag = new SurfaceShader("slag");
        planet = new PlanetShader();
        planetGrid = new PlanetGridShader();
        atmosphere = new AtmosphereShader();
        unlit = new LoadShader("planet", "unlit");
    }

    public static class AtmosphereShader extends LoadShader{
        public Camera3D camera;
        public Planet planet;

        Mat3D mat = new Mat3D();

        public AtmosphereShader(){
            super("atmosphere", "atmosphere");
        }

        @Override
        public void apply(){
            setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());

            setUniformf("u_time", Time.globalTime() / 10f);
            setUniformf("u_campos", camera.position);
            setUniformf("u_rcampos", Tmp.v31.set(camera.position).sub(planet.position));
            setUniformf("u_light", planet.getLightNormal());
            setUniformf("u_color", planet.atmosphereColor.r, planet.atmosphereColor.g, planet.atmosphereColor.b);
            setUniformf("u_innerRadius", planet.radius + 0.02f);
            setUniformf("u_outerRadius", planet.radius * 1.3f);

            setUniformMatrix4("u_model", planet.getTransform(mat).val);
            setUniformMatrix4("u_projection", camera.combined.val);
            setUniformMatrix4("u_invproj", camera.invProjectionView.val);
        }
    }

    public static class PlanetShader extends LoadShader{
        public Vec3 lightDir = new Vec3(1, 1, 1).nor();
        public Color ambientColor = Color.white.cpy();
        public Vec3 camDir = new Vec3();

        public PlanetShader(){
            super("planet", "planet");
        }

        @Override
        public void apply(){
            setUniformf("u_lightdir", lightDir);
            setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);
            setUniformf("u_camdir", camDir);
        }
    }

    public static class MeshShader extends LoadShader{

        public MeshShader(){
            super("planet", "mesh");
        }
    }

    public static class PlanetGridShader extends LoadShader{
        public Vec3 mouse = new Vec3();

        public PlanetGridShader(){
            super("planetgrid", "planetgrid");
        }

        @Override
        public void apply(){
            setUniformf("u_mouse", mouse);
        }
    }

    public static class LightShader extends LoadShader{
        public Color ambient = new Color(0.01f, 0.01f, 0.04f, 0.99f);

        public LightShader(){
            super("light", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_ambient", ambient);
        }

    }

    public static class FogShader extends LoadShader{
        public FogShader(){
            super("fog", "default");
        }
    }

    public static class UnitBuild extends LoadShader{
        public float progress, time;
        public Color color = new Color();
        public TextureRegion region;

        public UnitBuild(){
            super("unitbuild", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_time", time);
            setUniformf("u_color", color);
            setUniformf("u_progress", progress);
            setUniformf("u_uv", region.getU(), region.getV());
            setUniformf("u_uv2", region.getU2(), region.getV2());
            setUniformf("u_texsize", region.getTexture().getWidth(), region.getTexture().getHeight());
        }
    }

    public static class BlockBuild extends LoadShader{
        public Color color = new Color();
        public float progress;
        public TextureRegion region = new TextureRegion();

        public BlockBuild(){
            super("blockbuild", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_progress", progress);
            setUniformf("u_color", color);
            setUniformf("u_uv", region.getU(), region.getV());
            setUniformf("u_uv2", region.getU2(), region.getV2());
            setUniformf("u_time", Time.time());
            setUniformf("u_texsize", region.getTexture().getWidth(), region.getTexture().getHeight());
        }
    }

    public static class Shield extends LoadShader{

        public Shield(){
            super("shield", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Scl.scl(1f));
            setUniformf("u_time", Time.time() / Scl.scl(1f));
            setUniformf("u_offset",
            Core.camera.position.x - Core.camera.width / 2,
            Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_texsize", Core.camera.width,
            Core.camera.height);
        }
    }

    public static class SurfaceShader extends LoadShader{

        public SurfaceShader(String frag){
            super(frag, "default");

            Core.assets.load("sprites/noise.png", Texture.class).loaded = t -> {
                ((Texture)t).setFilter(TextureFilter.Linear);
                ((Texture)t).setWrap(TextureWrap.Repeat);
            };
        }

        @Override
        public void apply(){
            setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_resolution", Core.camera.width, Core.camera.height);
            setUniformf("u_time", Time.time());

            if(hasUniform("u_noise")){
                Core.assets.get("sprites/noise.png", Texture.class).bind(1);
                renderer.effectBuffer.getTexture().bind(0);

                setUniformi("u_noise", 1);
            }
        }
    }

    public static class LoadShader extends Shader{
        public LoadShader(String frag, String vert){
            super(Core.files.internal("shaders/" + vert + ".vert"), Core.files.internal("shaders/" + frag + ".frag"));
        }
    }
}
