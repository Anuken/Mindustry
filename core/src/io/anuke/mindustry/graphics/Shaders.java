package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.graphics.glutils.Shader;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Time;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Shaders{
    public static Outline outline;
    public static Shadow shadow;
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
    public static MenuShader menu;

    public static void init(){
        outline = new Outline();
        shadow = new Shadow();
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
        fullMix = new LoadShader("fullmix", "default");
        menu = new MenuShader();
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
            setUniformf("u_scl", Unit.dp.scl(1f));
            setUniformf("u_uv2", Core.atlas.white().getU2(), Core.atlas.white().getV2());
        }
    }

    public static class FogShader extends LoadShader{
        public FogShader(){
            super("fog", "default");
        }
    }

    public static class MixShader extends LoadShader{
        public Color color = new Color(Color.WHITE);

        public MixShader(){
            super("mix", "default");
        }

        @Override
        public void apply(){
            super.apply();
            setUniformf("u_color", color);
        }
    }

    public static class Space extends SurfaceShader{

        public Space(){
            super("space2");
        }

        @Override
        public void apply(){
            super.apply();
            setUniformf("u_center", world.width() * tilesize / 2f, world.height() * tilesize / 2f);
        }
    }

    public static class UnitBuild extends LoadShader{
        public float progress, time;
        public Color color = new Color();
        public TextureRegion region;

        public UnitBuild(){
            super("build", "default");
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

    public static class Outline extends LoadShader{
        public Color color = new Color();
        public TextureRegion region = new TextureRegion();
        public float scl;

        public Outline(){
            super("outline", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_color", color);
            setUniformf("u_scl", scl);
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

    public static class BlockPreview extends LoadShader{
        public Color color = new Color();
        public TextureRegion region = new TextureRegion();

        public BlockPreview(){
            super("blockpreview", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_color", color);
            setUniformf("u_uv", region.getU(), region.getV());
            setUniformf("u_uv2", region.getU2(), region.getV2());
            setUniformf("u_texsize", region.getTexture().getWidth(), region.getTexture().getHeight());
        }
    }

    public static class Shield extends LoadShader{

        public Shield(){
            super("shield", "default");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Unit.dp.scl(1f));
            setUniformf("u_time", Time.time() / Unit.dp.scl(1f));
            setUniformf("u_offset",
                    Core.camera.position.x - Core.camera.width / 2 ,
                    Core.camera.position.y - Core.camera.height / 2 );
            setUniformf("u_texsize", Core.camera.width ,
            Core.camera.height );
        }
    }

    public static class SurfaceShader extends LoadShader{

        public SurfaceShader(String frag){
            super(frag, "cache");
        }

        @Override
        public void apply(){
            setUniformf("camerapos",
                    Core.camera.position.x - Core.camera.width / 2 ,
                    Core.camera.position.y - Core.camera.height / 2 );
            setUniformf("screensize", Core.camera.width,
            Core.camera.height );
            setUniformf("u_time", Time.time());
        }
    }
    
    public static class LoadShader extends Shader{
        public LoadShader(String frag, String vert){
            super(Core.files.internal("shaders/" + vert + ".vertex"), Core.files.internal("shaders/" + frag + ".fragment"));
        }
    }
}
