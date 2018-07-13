package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Shader;
import io.anuke.ucore.scene.ui.layout.Unit;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Shaders{
    public static Outline outline;
    public static BlockBuild blockbuild;
    public static BlockPreview blockpreview;
    public static Shield shield;
    public static SurfaceShader water;
    public static SurfaceShader lava;
    public static SurfaceShader oil;
    public static Space space;
    public static UnitBuild build;
    public static MixShader mix;
    public static Shader fullMix;
    public static FogShader fog;

    public static void init(){
        outline = new Outline();
        blockbuild = new BlockBuild();
        blockpreview = new BlockPreview();
        shield = new Shield();
        water = new SurfaceShader("water");
        lava = new SurfaceShader("lava");
        oil = new SurfaceShader("oil");
        space = new Space();
        build = new UnitBuild();
        mix = new MixShader();
        fog = new FogShader();
        fullMix = new Shader("fullmix", "default");
    }

    public static class FogShader extends Shader{
        public FogShader(){
            super("fog", "default");
        }
    }

    public static class MixShader extends Shader{
        public Color color = new Color(Color.WHITE);

        public MixShader(){
            super("mix", "default");
        }

        @Override
        public void apply(){
            super.apply();
            shader.setUniformf("u_color", color);
        }
    }

    public static class Space extends SurfaceShader{

        public Space(){
            super("space2");
        }

        @Override
        public void apply(){
            super.apply();
            shader.setUniformf("u_center", world.width() * tilesize / 2f, world.height() * tilesize / 2f);
        }
    }

    public static class UnitBuild extends Shader{
        public float progress, time;
        public Color color = new Color();
        public TextureRegion region;

        public UnitBuild(){
            super("build", "default");
        }

        @Override
        public void apply(){
            shader.setUniformf("u_time", time);
            shader.setUniformf("u_color", color);
            shader.setUniformf("u_progress", progress);
            shader.setUniformf("u_uv", region.getU(), region.getV());
            shader.setUniformf("u_uv2", region.getU2(), region.getV2());
            shader.setUniformf("u_texsize", region.getTexture().getWidth(), region.getTexture().getHeight());
        }
    }

    public static class Outline extends Shader{
        public Color color = new Color();

        public Outline(){
            super("outline", "default");
        }

        @Override
        public void apply(){
            shader.setUniformf("u_color", color);
            shader.setUniformf("u_texsize", region.getTexture().getWidth(), region.getTexture().getHeight());
        }
    }

    public static class BlockBuild extends Shader{
        public Color color = new Color();
        public float progress;

        public BlockBuild(){
            super("blockbuild", "default");
        }

        @Override
        public void apply(){
            shader.setUniformf("u_progress", progress);
            shader.setUniformf("u_color", color);
            shader.setUniformf("u_uv", region.getU(), region.getV());
            shader.setUniformf("u_uv2", region.getU2(), region.getV2());
            shader.setUniformf("u_time", Timers.time());
            shader.setUniformf("u_texsize", region.getTexture().getWidth(), region.getTexture().getHeight());
        }
    }

    public static class BlockPreview extends Shader{
        public Color color = new Color();

        public BlockPreview(){
            super("blockpreview", "default");
        }

        @Override
        public void apply(){
            // shader.setUniformf("u_progress", progress);
            shader.setUniformf("u_color", color);
            shader.setUniformf("u_uv", region.getU(), region.getV());
            shader.setUniformf("u_uv2", region.getU2(), region.getV2());
            //shader.setUniformf("u_time", Timers.time());
            shader.setUniformf("u_texsize", region.getTexture().getWidth(), region.getTexture().getHeight());
        }
    }

    public static class Shield extends Shader{
        public static final int MAX_HITS = 3 * 64;
        public Color color = new Color();
        public FloatArray hits;

        public Shield(){
            super("shield", "default");
        }

        @Override
        public void apply(){
            float scaling = Core.cameraScale / 4f / Core.camera.zoom;
            if(hits.size > 0){
                shader.setUniform3fv("u_hits[0]", hits.items, 0, Math.min(hits.size, MAX_HITS));
                shader.setUniformi("u_hitamount", Math.min(hits.size, MAX_HITS) / 3);
            }
            shader.setUniformf("u_dp", Unit.dp.scl(1f));
            shader.setUniformf("u_color", color);
            shader.setUniformf("u_time", Timers.time() / Unit.dp.scl(1f));
            shader.setUniformf("u_scaling", scaling);
            shader.setUniformf("u_offset",
                    Core.camera.position.x - Core.camera.viewportWidth / 2 * Core.camera.zoom,
                    Core.camera.position.y - Core.camera.viewportHeight / 2 * Core.camera.zoom);
            shader.setUniformf("u_texsize", Gdx.graphics.getWidth() / Core.cameraScale * Core.camera.zoom,
                    Gdx.graphics.getHeight() / Core.cameraScale * Core.camera.zoom);
        }
    }

    public static class SurfaceShader extends Shader{

        public SurfaceShader(String frag){
            super(frag, "default");
        }

        @Override
        public void apply(){
            shader.setUniformf("camerapos",
                    Core.camera.position.x - Core.camera.viewportWidth / 2 * Core.camera.zoom,
                    Core.camera.position.y - Core.camera.viewportHeight / 2 * Core.camera.zoom);
            shader.setUniformf("screensize", Gdx.graphics.getWidth() / Core.cameraScale * Core.camera.zoom,
                    Gdx.graphics.getHeight() / Core.cameraScale * Core.camera.zoom);
            shader.setUniformf("time", Timers.time());
        }
    }
}
