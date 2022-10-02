package mindustry.graphics;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class Shaders{
    public static BlockBuildShader blockbuild;
    public static @Nullable ShieldShader shield;
    public static BuildBeamShader buildBeam;
    public static UnitBuildShader build;
    public static UnitArmorShader armor;
    public static DarknessShader darkness;
    public static FogShader fog;
    public static LightShader light;
    public static SurfaceShader water, mud, tar, slag, cryofluid, space, caustics, arkycite;
    public static PlanetShader planet;
    public static CloudShader clouds;
    public static PlanetGridShader planetGrid;
    public static AtmosphereShader atmosphere;
    public static ShockwaveShader shockwave;
    public static MeshShader mesh;
    public static Shader unlit;
    public static Shader screenspace;

    public static void init(){
        mesh = new MeshShader();
        blockbuild = new BlockBuildShader();
        try{
            shield = new ShieldShader();
        }catch(Throwable t){
            //don't load shield shader
            shield = null;
            t.printStackTrace();
        }
        fog = new FogShader();
        buildBeam = new BuildBeamShader();
        build = new UnitBuildShader();
        armor = new UnitArmorShader();
        darkness = new DarknessShader();
        light = new LightShader();
        water = new SurfaceShader("water");
        arkycite = new SurfaceShader("arkycite");
        mud = new SurfaceShader("mud");
        tar = new SurfaceShader("tar");
        slag = new SurfaceShader("slag");
        cryofluid = new SurfaceShader("cryofluid");
        space = new SpaceShader("space");
        caustics = new SurfaceShader("caustics"){
            @Override
            public String textureName(){
                return "caustics";
            }
        };
        planet = new PlanetShader();
        clouds = new CloudShader();
        planetGrid = new PlanetGridShader();
        atmosphere = new AtmosphereShader();
        unlit = new LoadShader("planet", "unlit");
        screenspace = new LoadShader("screenspace", "screenspace");

        //disabled for now...
        //shockwave = new ShockwaveShader();
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

            setUniformf("u_time", Time.globalTime / 10f);
            setUniformf("u_campos", camera.position);
            setUniformf("u_rcampos", Tmp.v31.set(camera.position).sub(planet.position));
            setUniformf("u_light", planet.getLightNormal());
            setUniformf("u_color", planet.atmosphereColor.r, planet.atmosphereColor.g, planet.atmosphereColor.b);
            setUniformf("u_innerRadius", planet.radius + planet.atmosphereRadIn);
            setUniformf("u_outerRadius", planet.radius + planet.atmosphereRadOut);

            setUniformMatrix4("u_model", planet.getTransform(mat).val);
            setUniformMatrix4("u_projection", camera.combined.val);
            setUniformMatrix4("u_invproj", camera.invProjectionView.val);
        }
    }

    public static class PlanetShader extends LoadShader{
        public Vec3 lightDir = new Vec3(1, 1, 1).nor();
        public Color ambientColor = Color.white.cpy();
        public Vec3 camDir = new Vec3();
        public Vec3 camPos = new Vec3();
        public Planet planet;

        public PlanetShader(){
            super("planet", "planet");
        }

        @Override
        public void apply(){
            camDir.set(renderer.planets.cam.direction).rotate(Vec3.Y, planet.getRotation());

            setUniformf("u_lightdir", lightDir);
            setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);
            setUniformf("u_camdir", camDir);
            setUniformf("u_campos", renderer.planets.cam.position);
        }
    }

    public static class CloudShader extends LoadShader{
        public Vec3 lightDir = new Vec3(1, 1, 1).nor();
        public Color ambientColor = Color.white.cpy();
        public Vec3 camDir = new Vec3();
        public float alpha = 1f;
        public Planet planet;

        public CloudShader(){
            super("planet", "clouds");
        }

        @Override
        public void apply(){
            camDir.set(renderer.planets.cam.direction).rotate(Vec3.Y, planet.getRotation());

            setUniformf("u_alpha", alpha);
            setUniformf("u_lightdir", lightDir);
            setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);
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
            super("light", "screenspace");
        }

        @Override
        public void apply(){
            setUniformf("u_ambient", ambient);
        }

    }

    public static class DarknessShader extends LoadShader{
        public DarknessShader(){
            super("darkness", "default");
        }
    }

    public static class FogShader extends LoadShader{
        public FogShader(){
            super("fog", "default");
        }
    }

    public static class UnitBuildShader extends LoadShader{
        public float progress, time;
        public Color color = new Color();
        public TextureRegion region;

        public UnitBuildShader(){
            super("unitbuild", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_time", time);
            setUniformf("u_color", color);
            setUniformf("u_progress", progress);
            setUniformf("u_uv", region.u, region.v);
            setUniformf("u_uv2", region.u2, region.v2);
            setUniformf("u_texsize", region.texture.width, region.texture.height);
        }
    }

    public static class UnitArmorShader extends LoadShader{
        public float progress, time;
        public TextureRegion region;

        public UnitArmorShader(){
            super("unitarmor", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_time", time);
            setUniformf("u_progress", progress);
            setUniformf("u_uv", region.u, region.v);
            setUniformf("u_uv2", region.u2, region.v2);
            setUniformf("u_texsize", region.texture.width, region.texture.height);
        }
    }

    public static class BlockBuildShader extends LoadShader{
        public float progress;
        public TextureRegion region = new TextureRegion();
        public float time;

        public BlockBuildShader(){
            super("blockbuild", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_progress", progress);
            setUniformf("u_uv", region.u, region.v);
            setUniformf("u_uv2", region.u2, region.v2);
            setUniformf("u_time", time);
            setUniformf("u_texsize", region.texture.width, region.texture.height);
        }
    }

    public static class ShieldShader extends LoadShader{

        public ShieldShader(){
            super("shield", "screenspace");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Scl.scl(1f));
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_offset",
                Core.camera.position.x - Core.camera.width / 2,
                Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_texsize", Core.camera.width, Core.camera.height);
            setUniformf("u_invsize", 1f/Core.camera.width, 1f/Core.camera.height);
        }
    }

    public static class BuildBeamShader extends LoadShader{

        public BuildBeamShader(){
            super("buildbeam", "screenspace");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Scl.scl(1f));
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_offset",
            Core.camera.position.x - Core.camera.width / 2,
            Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_texsize", Core.camera.width, Core.camera.height);
            setUniformf("u_invsize", 1f/Core.camera.width, 1f/Core.camera.height);
        }
    }

    //seed: 8kmfuix03fw
    public static class SpaceShader extends SurfaceShader{
        Texture texture;

        public SpaceShader(String frag){
            super(frag);

            Core.assets.load("sprites/space.png", Texture.class).loaded = t -> {
                texture = t;
                texture.setFilter(TextureFilter.linear);
                texture.setWrap(TextureWrap.mirroredRepeat);
            };
        }

        @Override
        public void apply(){
            setUniformf("u_campos", Core.camera.position.x, Core.camera.position.y);
            setUniformf("u_ccampos", Core.camera.position);
            setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
            setUniformf("u_time", Time.time);

            texture.bind(1);
            renderer.effectBuffer.getTexture().bind(0);

            setUniformi("u_stars", 1);
        }
    }

    public static class SurfaceShader extends Shader{
        Texture noiseTex;

        public SurfaceShader(String frag){
            super(getShaderFi("screenspace.vert"), getShaderFi(frag + ".frag"));
            loadNoise();
        }

        public SurfaceShader(String vertRaw, String fragRaw){
            super(vertRaw, fragRaw);
            loadNoise();
        }

        public String textureName(){
            return "noise";
        }

        public void loadNoise(){
            Core.assets.load("sprites/" + textureName() + ".png", Texture.class).loaded = t -> {
                t.setFilter(TextureFilter.linear);
                t.setWrap(TextureWrap.repeat);
            };
        }

        @Override
        public void apply(){
            setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_resolution", Core.camera.width, Core.camera.height);
            setUniformf("u_time", Time.time);

            if(hasUniform("u_noise")){
                if(noiseTex == null){
                    noiseTex = Core.assets.get("sprites/" + textureName() + ".png", Texture.class);
                }

                noiseTex.bind(1);
                renderer.effectBuffer.getTexture().bind(0);

                setUniformi("u_noise", 1);
            }
        }
    }

    public static class ShockwaveShader extends LoadShader{
        static final int max = 64;
        static final int size = 5;

        //x y radius life[1-0] lifetime
        protected FloatSeq data = new FloatSeq();
        protected FloatSeq uniforms = new FloatSeq();
        protected boolean hadAny = false;
        protected FrameBuffer buffer = new FrameBuffer();

        public float lifetime = 20f;

        public ShockwaveShader(){
            super("shockwave", "screenspace");

            Events.run(Trigger.update, () -> {
                if(state.isPaused()) return;
                if(state.isMenu()){
                    data.size = 0;
                    return;
                }

                var items = data.items;
                for(int i = 0; i < data.size; i += size){
                    //decrease lifetime
                    items[i + 3] -= Time.delta / items[i + 4];

                    if(items[i + 3] <= 0f){
                        //swap with head.
                        if(data.size > size){
                            System.arraycopy(items, data.size - size, items, i, size);
                        }

                        data.size -= size;
                        i -= size;
                    }
                }
            });

            Events.run(Trigger.preDraw, () -> {
                hadAny = data.size > 0;

                if(hadAny){
                    buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                    buffer.begin(Color.clear);
                }
            });

            Events.run(Trigger.postDraw, () -> {
                if(hadAny){
                    buffer.end();
                    Draw.blend(Blending.disabled);
                    buffer.blit(this);
                    Draw.blend();
                }
            });
        }

        @Override
        public void apply(){
            int count = data.size / size;

            setUniformi("u_shockwave_count", count);
            if(count > 0){
                setUniformf("u_resolution", Core.camera.width, Core.camera.height);
                setUniformf("u_campos", Core.camera.position.x - Core.camera.width/2f, Core.camera.position.y - Core.camera.height/2f);

                uniforms.clear();

                var items = data.items;
                for(int i = 0; i < count; i++){
                    int offset = i * size;

                    uniforms.add(
                    items[offset], items[offset + 1], //xy
                    items[offset + 2] * (1f - items[offset + 3]), //radius * time
                    items[offset + 3] //time
                    //lifetime ignored
                    );
                }

                setUniform4fv("u_shockwaves", uniforms.items, 0, uniforms.size);
            }
        }

        public void add(float x, float y, float radius){
            add(x, y, radius, 20f);
        }

        public void add(float x, float y, float radius, float lifetime){
            //replace first entry
            if(data.size / size >= max){
                var items = data.items;
                items[0] = x;
                items[1] = y;
                items[2] = radius;
                items[3] = 1f;
                items[4] = lifetime;
            }else{
                data.addAll(x, y, radius, 1f, lifetime);
            }
        }
    }

    public static class LoadShader extends Shader{
        public LoadShader(String frag, String vert){
            super(getShaderFi(vert + ".vert"), getShaderFi(frag + ".frag"));
        }
    }

    public static Fi getShaderFi(String file){
        return Core.files.internal("shaders/" + file);
    }
}
