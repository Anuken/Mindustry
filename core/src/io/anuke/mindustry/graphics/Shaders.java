package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.graphics.glutils.Shader;
import io.anuke.arc.scene.ui.layout.Scl;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.Time;

public class Shaders{
    public static Shadow shadow;
    public static BlockBuild blockbuild;
    public static @Nullable
    Shield shield;
    public static UnitBuild build;
    public static FogShader fog;
    public static MenuShader menu;
    public static SurfaceShader water, tar;

    public static void init(){
        shadow = new Shadow();
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
        menu = new MenuShader();
        water = new SurfaceShader("water");
        tar = new SurfaceShader("tar");
    }

    public static class MenuShader extends LoadShader{
        float time = 0f;

        public MenuShader(){
            super("menu", "default");
        }

        @Override
        public void apply(){
            time = time % 158;

            setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
            setUniformi("u_time", (int)(time += Core.graphics.getDeltaTime() * 60f));
            setUniformf("u_uv", Core.atlas.white().getU(), Core.atlas.white().getV());
            setUniformf("u_scl", Scl.scl(1f));
            setUniformf("u_uv2", Core.atlas.white().getU2(), Core.atlas.white().getV2());
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

    public static class Shadow extends LoadShader{
        public Color color = new Color();
        public TextureRegion region = new TextureRegion();
        public float scl;

        public Shadow(){
            super("shadow", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_color", color);
            setUniformf("u_scl", scl);
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
        }

        @Override
        public void apply(){
            setUniformf("camerapos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
            setUniformf("screensize", Core.camera.width, Core.camera.height);
            setUniformf("time", Time.time());
        }
    }

    public static class LoadShader extends Shader{
        public LoadShader(String frag, String vert){
            super(Core.files.internal("shaders/" + vert + ".vertex.glsl"), Core.files.internal("shaders/" + frag + ".fragment.glsl"));
        }
    }
}
